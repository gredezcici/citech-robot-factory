package agent;

import javassist.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Unified javaagent that exports FINAL SQL across:
 *  - Plain JDBC (any framework that uses JDBC under the hood)
 *  - HikariCP & Druid (unwraps proxies to reach the real driver Statement)
 *  - MyBatis (captures mapperId + inlined final SQL via BoundSql)
 *
 * Build with MANIFEST.MF:
 *   Premain-Class: agent.UnifiedFinalSqlAgent
 *   Can-Redefine-Classes: true
 *   Can-Retransform-Classes: true
 *
 * Run:
 *   java \
 *   -javaagent:/path/sql-agent.jar \
 *   -Dsql.agent.out=/var/log/final-sql.jsonl \
 *   -Dsql.agent.inline=true \
 *   -Dsql.agent.driverToString=true \
 *   -Dsql.agent.export.credentials=true \
 *   -Dsql.agent.export.password=false \
 *   -Dsql.agent.mask.password=true \
 *   -Dsql.agent.password.mask=*** \
 *   -jar your-app.jar
 *
 * SECURITY NOTE:
 *   Exporting credentials is dangerous. By default, password logging is OFF.
 *   Turn it on only if you fully understand the implications. Masking is ON by default.
 */
public final class UnifiedFinalSqlAgentV2 {
    private UnifiedFinalSqlAgentV2() {
    }

    public static void premain(String args, Instrumentation inst) {
        final String out = System.getProperty("sql.agent.out", "final-sql.jsonl");
        Log.init(out);

        inst.addTransformer(new Xf(), true);

        // Best-effort: retransform already-loaded classes of interest
        try {
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (!inst.isModifiableClass(c)) continue;
                String n = c.getName();
                if (n.startsWith("org.apache.ibatis.executor.") ||
                        implementsAnyJdbc(c)) {
                    try {
                        inst.retransformClasses(c);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        Log.json("{\"type\":\"agent-start\",\"out\":\"" + esc(out) + "\"}");
    }

    static boolean implementsAnyJdbc(Class<?> c) {
        try {
            for (Class<?> itf : c.getInterfaces()) {
                if (itf == java.sql.Connection.class || itf == java.sql.PreparedStatement.class || itf == java.sql.Statement.class)
                    return true;
            }
            Class<?> s = c.getSuperclass();
            return s != null && implementsAnyJdbc(s);
        } catch (Throwable t) {
            return false;
        }
    }

    // === Transformer ===
    static final class Xf implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> cls, ProtectionDomain pd, byte[] buf) {
            if (className == null) return null;
            String n = className.replace('/', '.');
            if (n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jdk.") || n.startsWith("sun.") || n.startsWith("com.sun."))
                return null;

            try {
                ClassPool cp = ClassPool.getDefault();
                if (loader != null) cp.insertClassPath(new LoaderClassPath(loader));
                CtClass ct = cp.get(n);
                if (ct.isInterface() || ct.isAnnotation() || ct.isEnum()) return null;

                boolean mod = false;

                // MyBatis
                if (n.equals("org.apache.ibatis.executor.BaseExecutor") || n.equals("org.apache.ibatis.executor.CachingExecutor")) {
                    mod |= instrumentMyBatis(ct);
                }

                // JDBC: Connection / PreparedStatement / Statement
                if (subtypeOf(cp, ct, "java.sql.Connection")) mod |= instrumentConnection(ct);
                if (subtypeOf(cp, ct, "java.sql.PreparedStatement")) mod |= instrumentPreparedStatement(ct);
                if (subtypeOf(cp, ct, "java.sql.Statement")) mod |= instrumentStatement(ct);

                if (!mod) return null;
                byte[] out = ct.toBytecode();
                ct.detach();
                return out;
            } catch (Throwable t) {
                try {
                    Log.json("{\"type\":\"agent-error\",\"class\":\"" + esc(n) + "\",\"error\":\"" + esc(String.valueOf(t)) + "\"}");
                } catch (Throwable ignored) {
                }
                return null;
            }
        }

        boolean subtypeOf(ClassPool cp, CtClass ct, String fqn) {
            try {
                return ct.subtypeOf(cp.get(fqn));
            } catch (NotFoundException e) {
                return false;
            }
        }

        boolean instrumentMyBatis(CtClass ct) throws Exception {
            boolean mod = false;
            for (CtMethod m : ct.getDeclaredMethods()) {
                String mn = m.getName();
                CtClass[] p = m.getParameterTypes();
                if ("update".equals(mn) && p.length >= 2 && p[0].getName().equals("org.apache.ibatis.mapping.MappedStatement")) {
                    injectMyBatisAround(m);
                    mod = true;
                }
                if ("query".equals(mn) && p.length >= 4 && p[0].getName().equals("org.apache.ibatis.mapping.MappedStatement")) {
                    injectMyBatisAround(m);
                    mod = true;
                }
            }
            return mod;
        }

        void injectMyBatisAround(CtMethod m) throws CannotCompileException {
            m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".MyBatisCtx.enter($1, $2); }");
            m.insertAfter("{ " + UnifiedFinalSqlAgent.class.getName() + ".MyBatisCtx.exit(); }", true);
        }

        boolean instrumentConnection(CtClass ct) throws Exception {
            boolean mod = false;
            for (CtMethod m : ct.getDeclaredMethods()) {
                String name = m.getName();
                if (("prepareStatement".equals(name) || "prepareCall".equals(name))) {
                    CtClass[] p = m.getParameterTypes();
                    if (p.length >= 1 && p[0].getName().equals("java.lang.String")) {
                        String code = "if ($_ != null && $1 != null) " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.register($_, $1);";
                        try {
                            m.insertAfter("{ " + code + " }", false);
                            mod = true;
                        } catch (CannotCompileException ignored) {
                        }
                    }
                }
            }
            return mod;
        }

        boolean instrumentPreparedStatement(CtClass ct) throws Exception {
            boolean mod = false;
            for (CtMethod m : ct.getDeclaredMethods()) {
                String name = m.getName();
                CtClass[] p = m.getParameterTypes();
                if (name.startsWith("set") && p.length >= 1 && CtClass.intType.equals(p[0])) {
                    String code = ("setNull".equals(name)) ? UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.setParam($0, $1, (Object)null);" : UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.setParam($0, $1, $2);";
                    try {
                        m.insertBefore("{ " + code + " }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
                if ("clearParameters".equals(name) && p.length == 0) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.clearParams($0); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
                if ("addBatch".equals(name) && p.length == 0) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.markBatch($0, null); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
                if (name.startsWith("execute")) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".onJdbcExecute($0, \"" + name + "\", $args); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                    try {
                        CtClass thr = ct.getClassPool().get("java.lang.Throwable");
                        m.addCatch("{ " + UnifiedFinalSqlAgent.class.getName() + ".onJdbcException($0, \"" + name + "\", $args, $e); throw $e; }", thr);
                        mod = true;
                    } catch (Throwable ignored) {
                    }
                }
                if ("close".equals(name) && p.length == 0) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.clearAll($0); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
            }
            return mod;
        }

        boolean instrumentStatement(CtClass ct) throws Exception {
            boolean mod = false;
            for (CtMethod m : ct.getDeclaredMethods()) {
                String name = m.getName();
                CtClass[] p = m.getParameterTypes();
                if ("addBatch".equals(name) && p.length == 1 && p[0].getName().equals("java.lang.String")) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.markBatch($0, (String)$1); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
                if (name.startsWith("execute")) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".onJdbcExecute($0, \"" + name + "\", $args); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                    try {
                        CtClass thr = ct.getClassPool().get("java.lang.Throwable");
                        m.addCatch("{ " + UnifiedFinalSqlAgent.class.getName() + ".onJdbcException($0, \"" + name + "\", $args, $e); throw $e; }", thr);
                        mod = true;
                    } catch (Throwable ignored) {
                    }
                }
                if ("close".equals(name) && p.length == 0) {
                    try {
                        m.insertBefore("{ " + UnifiedFinalSqlAgent.class.getName() + ".JdbcCapture.clearAll($0); }");
                        mod = true;
                    } catch (CannotCompileException ignored) {
                    }
                }
            }
            return mod;
        }
    }

    // ==== MyBatis context & rendering (via reflection) ====
    static final class MyBatisCtx {
        static final ThreadLocal<Ctx> TL = ThreadLocal.withInitial(Ctx::new);

        static final class Ctx {
            String mapperId;
            String rawSql;
            String finalSql;
            long startNs;
            DriverSpecific.ConnInfo ci;
        }

        static void enter(Object mappedStatement, Object paramObj) {
            Ctx c = TL.get();
            c.startNs = System.nanoTime();
            try {
                Class<?> msC = Class.forName("org.apache.ibatis.mapping.MappedStatement");
                Class<?> bsC = Class.forName("org.apache.ibatis.mapping.BoundSql");
                Class<?> cfgC = Class.forName("org.apache.ibatis.session.Configuration");
                Class<?> pmC = Class.forName("org.apache.ibatis.mapping.ParameterMapping");
                Class<?> pmodeC = Class.forName("org.apache.ibatis.mapping.ParameterMode");

                Object cfg = msC.getMethod("getConfiguration").invoke(mappedStatement);
                Object bound = msC.getMethod("getBoundSql", Object.class).invoke(mappedStatement, paramObj);
                c.mapperId = String.valueOf(msC.getMethod("getId").invoke(mappedStatement));
                c.rawSql = String.valueOf(bsC.getMethod("getSql").invoke(bound));

                @SuppressWarnings("unchecked")
                List<Object> pms = (List<Object>) bsC.getMethod("getParameterMappings").invoke(bound);
                Vendor vendor = Vendor.UNKNOWN;
                try {
                    Object dbIdObj = cfgC.getMethod("getDatabaseId").invoke(cfg);
                    if (dbIdObj != null) {
                        String dbId = String.valueOf(dbIdObj).toLowerCase();
                        if (dbId.contains("mysql")) vendor = Vendor.MYSQL;
                        else if (dbId.contains("postgre")) vendor = Vendor.POSTGRES;
                        else if (dbId.contains("oracle")) vendor = Vendor.ORACLE;
                        else if (dbId.contains("sqlserver") || dbId.contains("mssql")) vendor = Vendor.SQLSERVER;
                        else if (dbId.contains("h2")) vendor = Vendor.H2;
                        else if (dbId.contains("sqlite")) vendor = Vendor.SQLITE;
                        else if (dbId.contains("db2")) vendor = Vendor.DB2;
                    }
                } catch (Throwable ignored) {
                }
                // Capture DataSource credentials from MyBatis Environment
                try {
                    boolean exportCreds = Boolean.parseBoolean(System.getProperty("sql.agent.export.credentials", "true"));
                    if (exportCreds) {
                        Class<?> envC = Class.forName("org.apache.ibatis.mapping.Environment");
                        Object env = cfgC.getMethod("getEnvironment").invoke(cfg);
                        if (env != null) {
                            Object ds = envC.getMethod("getDataSource").invoke(env);
                            if (ds != null) {
                                c.ci = DriverSpecific.readConnInfoFromDataSource(ds);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
                List<String> values = new ArrayList<>();

                Object meta = (paramObj == null ? null : cfgC.getMethod("newMetaObject", Object.class).invoke(cfg, paramObj));
                Object thr = cfgC.getMethod("getTypeHandlerRegistry").invoke(cfg);
                boolean isSimple = false;
                if (meta != null) {
                    try {
                        isSimple = (Boolean) thr.getClass().getMethod("hasTypeHandler", Class.class).invoke(thr, paramObj.getClass());
                    } catch (Throwable ignored) {
                    }
                }

                Object OUT = Enum.valueOf((Class<Enum>) pmodeC.asSubclass(Enum.class), "OUT");

                for (Object pm : pms) {
                    Object mode = pmC.getMethod("getMode").invoke(pm);
                    if (mode != null && mode.equals(OUT)) {
                        values.add("NULL");
                        continue;
                    }
                    String prop = String.valueOf(pmC.getMethod("getProperty").invoke(pm));
                    Object value;
                    boolean hasAdd = (Boolean) bsC.getMethod("hasAdditionalParameter", String.class).invoke(bound, prop);
                    if (hasAdd) value = bsC.getMethod("getAdditionalParameter", String.class).invoke(bound, prop);
                    else if (meta == null) value = null;
                    else if (isSimple) value = paramObj;
                    else {
                        try {
                            value = meta.getClass().getMethod("getValue", String.class).invoke(meta, prop);
                        } catch (Throwable t) {
                            value = null;
                        }
                    }
                    if (value == null && prop != null && prop.startsWith("__frch_")) {
                        boolean has = (Boolean) bsC.getMethod("hasAdditionalParameter", String.class).invoke(bound, prop);
                        if (has) value = bsC.getMethod("getAdditionalParameter", String.class).invoke(bound, prop);
                    }
                    values.add(Sql.inlineFormat(value, vendor));
                }

                c.finalSql = Sql.inline(c.rawSql, values);
            } catch (Throwable t) {
                c.finalSql = c.rawSql; // fallback to raw
                try {
                    Log.json("{\"type\":\"agent-warn\",\"msg\":\"mybatis-render-failed\",\"error\":\"" + esc(String.valueOf(t)) + "\"}");
                } catch (Throwable ignored) {
                }
            }
        }

        static void exit() {
            Ctx c = TL.get();
            long tookMs = (System.nanoTime() - c.startNs) / 1_000_000L;
            String json = Json.start()
                    .kv("type", "mybatis-final-sql")
                    .kv("ts", Instant.now().toString())
                    .kv("thread", Thread.currentThread().getName())
                    .kv("mapperId", c.mapperId)
                    .kvRaw("finalSql", c.finalSql == null ? null : esc(c.finalSql))
                    .kvRaw("rawSql", c.rawSql == null ? null : esc(c.rawSql))
                    .kv("tookMs", String.valueOf(tookMs))
                    .kv("jdbcUrl", c.ci == null ? null : c.ci.url)
                    .kv("jdbcUser", c.ci == null ? null : c.ci.user)
                    .kv("jdbcPassword", c.ci == null ? null : c.ci.password)
                    .end();
            Log.json(json);
            c.mapperId = c.rawSql = c.finalSql = null;
            c.ci = null;
            c.startNs = 0L;
        }
    }

    // ==== JDBC hooks ====
    public static void onJdbcExecute(Object self, String method, Object[] args) {
        try {
            String sql = null;
            if (args != null && args.length > 0 && args[0] instanceof String) sql = (String) args[0];
            if (sql == null) sql = JdbcCapture.lookupSql(self);

            Map<Integer, Object> params = JdbcCapture.snapshotParams(self);
            boolean batch = JdbcCapture.isBatch(self);

            // prefer MyBatis-rendered finalSql if present in this thread
            MyBatisCtx.Ctx ctx = MyBatisCtx.TL.get();
            String finalSql = (ctx != null && ctx.finalSql != null) ? ctx.finalSql : null;

            java.sql.Connection conn = null;
            try {
                conn = (self instanceof java.sql.Statement) ? ((java.sql.Statement) self).getConnection() : null;
            } catch (Throwable ignored) {
            }
            Vendor v = Vendor.detect(conn);

            // Druid/Hikari unwrap + driver-specific rendering
            Object driverStmt = DriverSpecific.unwrapPool(self);
            String driverRendered = DriverSpecific.tryRender(driverStmt, v);
            if (finalSql == null) {
                if (driverRendered != null) finalSql = driverRendered;
                else if (sql != null && Boolean.parseBoolean(System.getProperty("sql.agent.inline", "true"))) {
                    finalSql = Sql.inlineWithParams(sql, params, v);
                }
            }

            // connection info (URL / user / optional password)
            boolean exportCreds = Boolean.parseBoolean(System.getProperty("sql.agent.export.credentials", "true"));
            DriverSpecific.ConnInfo ci = exportCreds ? DriverSpecific.readConnInfo(conn) : null;
            if (exportCreds) {
                MyBatisCtx.Ctx mctx = MyBatisCtx.TL.get();
                if (mctx != null) ci = DriverSpecific.merge(ci, mctx.ci);
            }

            String json = Json.start()
                    .kv("type", "jdbc")
                    .kv("ts", Instant.now().toString())
                    .kv("thread", Thread.currentThread().getName())
                    .kv("method", method)
                    .kv("driverClass", self.getClass().getName())
                    .kv("batch", String.valueOf(batch))
                    .kvRaw("sql", esc(sql))
                    .kvRaw("finalSql", finalSql == null ? null : esc(finalSql))
                    .kvRaw("params", Json.params(params))
                    .kv("vendor", v.name())
                    .kv("jdbcUrl", ci == null ? null : ci.url)
                    .kv("jdbcUser", ci == null ? null : ci.user)
                    .kv("jdbcPassword", (ci == null) ? null : ci.password)
                    .end();
            Log.json(json);
        } catch (Throwable t) {
            try {
                Log.json("{\"type\":\"agent-error\",\"when\":\"jdbc-execute\",\"error\":\"" + esc(String.valueOf(t)) + "\"}");
            } catch (Throwable ignored) {
            }
        } finally {
            try {
                JdbcCapture.clearBatch(self);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onJdbcException(Object self, String method, Object[] args, Throwable e) {
        try {
            String sql = null;
            if (args != null && args.length > 0 && args[0] instanceof String) sql = (String) args[0];
            if (sql == null) sql = JdbcCapture.lookupSql(self);

            Map<Integer, Object> params = JdbcCapture.snapshotParams(self);
            boolean batch = JdbcCapture.isBatch(self);

            java.sql.Connection conn = null;
            try {
                conn = (self instanceof java.sql.Statement) ? ((java.sql.Statement) self).getConnection() : null;
            } catch (Throwable ignored) {
            }
            Vendor v = Vendor.detect(conn);

            String finalSql = null;
            try {
                Object driverStmt = DriverSpecific.unwrapPool(self);
                finalSql = DriverSpecific.tryRender(driverStmt, v);
                if (finalSql == null && sql != null && Boolean.parseBoolean(System.getProperty("sql.agent.inline", "true"))) {
                    finalSql = Sql.inlineWithParams(sql, params, v);
                }
            } catch (Throwable ignored2) {
            }

            String json = Json.start()
                    .kv("type", "jdbc-error")
                    .kv("ts", Instant.now().toString())
                    .kv("thread", Thread.currentThread().getName())
                    .kv("method", method)
                    .kv("driverClass", self.getClass().getName())
                    .kv("batch", String.valueOf(batch))
                    .kvRaw("sql", esc(sql))
                    .kvRaw("finalSql", finalSql == null ? null : esc(finalSql))
                    .kvRaw("params", Json.params(params))
                    .kv("vendor", v.name())
                    .kv("exceptionClass", e.getClass().getName())
                    .kv("exceptionMessage", String.valueOf(e.getMessage()))
                    .kv("stack", stack(e))
                    .end();
            Log.json(json);
        } catch (Throwable logFail) {
            try {
                Log.json("{\"type\":\"agent-error\",\"when\":\"jdbc-exception-log\",\"error\":\"" + esc(String.valueOf(logFail)) + "\"}");
            } catch (Throwable ignored) {
            }
        } finally {
            try {
                JdbcCapture.clearBatch(self);
            } catch (Throwable ignored) {
            }
        }
    }

    static final class JdbcCapture {
        private static final Map<Object, String> STMT_SQL = Collections.synchronizedMap(new WeakHashMap<>());
        private static final Map<Object, Map<Integer, Object>> STMT_PARAMS = Collections.synchronizedMap(new WeakHashMap<>());
        private static final Map<Object, Boolean> STMT_BATCH = Collections.synchronizedMap(new WeakHashMap<> ());

        static void register(Object stmt, String sql) {
            if (stmt != null && sql != null) STMT_SQL.put(stmt, sql);
        }

        static String lookupSql(Object stmt) {
            return STMT_SQL.get(stmt);
        }

        static void setParam(Object stmt, int index, Object value) {
            Map<Integer, Object> m = STMT_PARAMS.get(stmt);
            if (m == null) {
                m = Collections.synchronizedMap(new TreeMap<>());
                STMT_PARAMS.put(stmt, m);
            }
            m.put(index, value);
        }

        static void clearParams(Object stmt) {
            STMT_PARAMS.remove(stmt);
        }

        static Map<Integer, Object> snapshotParams(Object stmt) {
            Map<Integer, Object> m = STMT_PARAMS.get(stmt);
            if (m == null) return Collections.emptyMap();
            synchronized (m) {
                return new TreeMap<>(m);
            }
        }

        static void markBatch(Object stmt, String sql) {
            if (sql != null) STMT_SQL.put(stmt, sql);
            STMT_BATCH.put(stmt, Boolean.TRUE);
        }

        static boolean isBatch(Object stmt) {
            return Boolean.TRUE.equals(STMT_BATCH.get(stmt));
        }

        static void clearBatch(Object stmt) {
            STMT_BATCH.remove(stmt);
        }

        static void clearAll(Object stmt) {
            STMT_SQL.remove(stmt);
            STMT_PARAMS.remove(stmt);
            STMT_BATCH.remove(stmt);
        }
    }

    // ==== Driver / vendor helpers ====
    enum Vendor {
        MYSQL, POSTGRES, ORACLE, SQLSERVER, H2, SQLITE, DB2, UNKNOWN;

        static Vendor detect(java.sql.Connection c) {
            try {
                if (c == null) return UNKNOWN;
                String url = c.getMetaData().getURL();
                if (url == null) return UNKNOWN;
                String u = url.toLowerCase();
                if (u.startsWith("jdbc:mysql:")) return MYSQL;
                if (u.startsWith("jdbc:postgresql:")) return POSTGRES;
                if (u.startsWith("jdbc:oracle:")) return ORACLE;
                if (u.startsWith("jdbc:sqlserver:")) return SQLSERVER;
                if (u.startsWith("jdbc:h2:")) return H2;
                if (u.startsWith("jdbc:sqlite:")) return SQLITE;
                if (u.startsWith("jdbc:db2:")) return DB2;
            } catch (Throwable ignored) {
            }
            return UNKNOWN;
        }
    }

    static final class DriverSpecific {
        /**
         * Container for JDBC credential info
         */
        static final class ConnInfo {
            String url;
            String user;
            String password;
        }

        /**
         * Unwrap common pool proxies (Hikari & Druid) to the underlying driver Statement.
         */
        static Object unwrapPool(Object obj) {
            try {
                if (obj == null) return null;
                Object cur = obj;
                for (int depth = 0; depth < 8 && cur != null; depth++) {
                    String n = cur.getClass().getName();
                    boolean isPool = n.startsWith("com.zaxxer.hikari.") || n.startsWith("com.alibaba.druid.");
                    if (!isPool) return cur; // reached real driver stmt
                    Object next = findDelegateField(cur);
                    if (next == null)
                        next = tryDelegateMethod(cur, "getTargetStatement", "getStatement", "getPreparedStatement", "getRawStatement", "getDelegate");
                    if (next == null) return cur;
                    cur = next;
                }
                return cur;
            } catch (Throwable t) {
                return obj;
            }
        }

        private static Object tryDelegateMethod(Object obj, String... names) {
            for (String nm : names) {
                try {
                    java.lang.reflect.Method m = obj.getClass().getDeclaredMethod(nm);
                    m.setAccessible(true);
                    Object v = m.invoke(obj);
                    if (v instanceof java.sql.Statement) return v;
                } catch (Throwable ignored) {
                }
            }
            return null;
        }

        private static Object findDelegateField(Object obj) {
            for (Class<?> k = obj.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    Class<?> ft = f.getType();
                    if (java.sql.Statement.class.isAssignableFrom(ft)) {
                        try {
                            f.setAccessible(true);
                            return f.get(obj);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
            // Try common accessors
            try {
                java.lang.reflect.Method m = obj.getClass().getDeclaredMethod("getTargetStatement");
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Throwable ignored) {
            }
            try {
                java.lang.reflect.Method m = obj.getClass().getDeclaredMethod("getPreparedStatement");
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Throwable ignored) {
            }
            try {
                java.lang.reflect.Method m = obj.getClass().getDeclaredMethod("getStatement");
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Throwable ignored) {
            }
            return null;
        }

        /**
         * Attempt to render final SQL via driver-specific hooks (e.g., MySQL's asSql()), or fallback to toString().
         */
        static String tryRender(Object stmt, Vendor v) {
            try {
                if (stmt == null) return null;
                // MySQL Connector/J historical asSql()
                if (v == Vendor.MYSQL) {
                    java.lang.reflect.Method m = findNoArg(stmt.getClass(), "asSql");
                    if (m != null) {
                        m.setAccessible(true);
                        Object r = m.invoke(stmt);
                        if (r != null) return String.valueOf(r);
                    }
                    if (Boolean.getBoolean("sql.agent.driverToString")) {
                        String s = String.valueOf(stmt);
                        int idx = s.indexOf(": ");
                        return idx > 0 ? s.substring(idx + 2) : s;
                    }
                } else if (Boolean.getBoolean("sql.agent.driverToString")) {
                    return String.valueOf(stmt);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        static java.lang.reflect.Method findNoArg(Class<?> c, String name) {
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Method m : k.getDeclaredMethods())
                    if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
            }
            return null;
        }

        /**
         * Extract creds from a known DataSource implementation via reflection.
         */
        static ConnInfo readConnInfoFromDataSource(Object ds) {
            if (ds == null) return null;
            ConnInfo out = new ConnInfo();
            try {
                // Try common getters first
                try {
                    Object v = callNoArg(ds, "getJdbcUrl");
                    if (v != null) out.url = String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    Object v = callNoArg(ds, "getUrl");
                    if (v != null) out.url = String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    Object v = callNoArg(ds, "getUsername");
                    if (v != null) out.user = String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    Object v = callNoArg(ds, "getUser");
                    if (v != null) out.user = String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    Object v = callNoArg(ds, "getPassword");
                    if (v != null) out.password = String.valueOf(v);
                } catch (Throwable ignored) {
                }

                // HikariConfig/HikariDataSource properties
                if (out.password == null) {
                    try {
                        Object props = callNoArg(ds, "getDataSourceProperties");
                        if (props instanceof java.util.Properties) {
                            String p = ((java.util.Properties) props).getProperty("password");
                            if (p != null) out.password = p;
                            if (out.user == null)
                                out.user = ((java.util.Properties) props).getProperty("user", ((java.util.Properties) props).getProperty("username"));
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // Apache DBCP BasicDataSource
                if (out.url == null || out.user == null || out.password == null) {
                    try {
                        if (ds.getClass().getName().startsWith("org.apache.commons.dbcp2.")) {
                            Object v;
                            if (out.url == null && (v = callNoArg(ds, "getUrl")) != null) out.url = String.valueOf(v);
                            if (out.user == null && (v = callNoArg(ds, "getUsername")) != null)
                                out.user = String.valueOf(v);
                            if (out.password == null && (v = callNoArg(ds, "getPassword")) != null)
                                out.password = String.valueOf(v);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // Tomcat JDBC pool
                if (out.url == null || out.user == null || out.password == null) {
                    try {
                        if (ds.getClass().getName().startsWith("org.apache.tomcat.jdbc.pool.")) {
                            Object v;
                            if (out.url == null && (v = callNoArg(ds, "getUrl")) != null) out.url = String.valueOf(v);
                            if (out.user == null && (v = callNoArg(ds, "getUsername")) != null)
                                out.user = String.valueOf(v);
                            if (out.password == null && (v = callNoArg(ds, "getPassword")) != null)
                                out.password = String.valueOf(v);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // c3p0
                if (out.url == null || out.user == null || out.password == null) {
                    try {
                        if (ds.getClass().getName().startsWith("com.mchange.v2.c3p0.")) {
                            Object v;
                            if (out.url == null && (v = callNoArg(ds, "getJdbcUrl")) != null)
                                out.url = String.valueOf(v);
                            if (out.user == null && (v = callNoArg(ds, "getUser")) != null)
                                out.user = String.valueOf(v);
                            if (out.password == null && (v = callNoArg(ds, "getPassword")) != null)
                                out.password = String.valueOf(v);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // Final fallback: inspect a nested "dataSource" field
                if (out.password == null || out.user == null || out.url == null) {
                    Object inner = findFieldByName(ds, "dataSource", "xaDataSource", "targetDataSource");
                    if (inner != null && inner != ds) {
                        ConnInfo deeper = readConnInfoFromDataSource(inner);
                        out = merge(out, deeper);
                    }
                }
            } catch (Throwable ignored) {
            }
            // Apply mask policy here? leave raw; caller masks.
            return (out.url == null && out.user == null && out.password == null) ? null : out;
        }

        /**
         * Try to discover a DataSource by walking the connection's object graph (shallow).
         */
        static Object findDataSourceFromConnection(Object root, int depth, java.util.Set<Integer> seen) {
            if (root == null || depth > 5) return null;
            int id = System.identityHashCode(root);
            if (!seen.add(id)) return null;
            Class<?> c = root.getClass();
            try {
                // If it looks like a DataSource
                if (root instanceof javax.sql.DataSource) return root;
                if (c.getName().endsWith("DataSource")) return root;
            } catch (Throwable ignored) {
            }
            // Drill into interesting fields
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                java.lang.reflect.Field[] fs = k.getDeclaredFields();
                int scanned = 0;
                for (java.lang.reflect.Field f : fs) {
                    if (scanned++ > 32) break; // safety cap
                    try {
                        f.setAccessible(true);
                        Object v = f.get(root);
                        if (v == null) continue;
                        String n = f.getName();
                        String tn = f.getType().getName();
                        if (n.toLowerCase().contains("datasource") || tn.endsWith("DataSource") || tn.contains("Hikari") || tn.contains("Druid")) {
                            Object ds = findDataSourceFromConnection(v, depth + 1, seen);
                            if (ds != null) return ds;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            // Try common accessor methods
            try {
                Object v = callNoArg(root, "getDataSource");
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
            try {
                Object v = callNoArg(root, "getHikariPool");
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
            try {
                Object v = callNoArg(root, "getPool");
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
            try {
                Object v = callNoArg(root, "getPoolEntry");
                if (v != null) return findDataSourceFromConnection(v, depth + 1, seen);
            } catch (Throwable ignored) {
            }
            return null;
        }

        static Object callNoArg(Object o, String name) throws Exception {
            if (o == null) return null;
            for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                try {
                    java.lang.reflect.Method m = k.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m.invoke(o);
                } catch (NoSuchMethodException nsme) { /* try super */ }
            }
            return null;
        }

        static ConnInfo readConnInfo(java.sql.Connection c) {
            ConnInfo ci = new ConnInfo();
            try {
                if (c != null) {
                    java.sql.DatabaseMetaData md = c.getMetaData();
                    if (md != null) {
                        try {
                            ci.url = md.getURL();
                        } catch (Throwable ignored) {
                        }
                        try {
                            ci.user = md.getUserName();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            boolean wantPwd = Boolean.parseBoolean(System.getProperty("sql.agent.export.password", "false"));
            if (c != null) {
                // Try to find a DataSource behind this connection and read credentials from it
                try {
                    Object ds = findDataSourceFromConnection(c, 0, new java.util.HashSet<>());
                    ConnInfo fromDs = readConnInfoFromDataSource(ds);
                    ci = merge(ci, fromDs);
                } catch (Throwable ignored) {
                }
            }

            // Fallback: attempt to parse user/password out of the URL if missing
            try {
                if (ci.url != null && (ci.user == null || (wantPwd && (ci.password == null)))) {
                    java.util.Map<String, String> up = parseCredsFromUrl(ci.url);
                    if (ci.user == null) ci.user = up.get("user");
                    if (wantPwd && ci.password == null) ci.password = up.get("password");
                }
            } catch (Throwable ignored) {
            }

            // Mask if requested
            boolean mask = Boolean.parseBoolean(System.getProperty("sql.agent.mask.password", "true"));
            String maskRep = System.getProperty("sql.agent.password.mask", "***");
            if (ci.password != null && (!wantPwd || mask)) {
                ci.password = maskRep;
            }
            return ci;
        }

        static ConnInfo merge(ConnInfo a, ConnInfo b) {
            if (a == null) return b;
            if (b == null) return a;
            ConnInfo r = new ConnInfo();
            r.url = (a.url != null) ? a.url : b.url;
            r.user = (a.user != null) ? a.user : b.user;
            r.password = (a.password != null) ? a.password : b.password;
            return r;
        }

        static java.util.Map<String, String> parseCredsFromUrl(String url) {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            if (url == null) return m;
            String u = url;
            try {
                // Handle jdbc:sqlserver://host;user=U;password=P;...
                if (u.startsWith("jdbc:sqlserver:")) {
                    String[] parts = u.split(";");
                    for (String part : parts) {
                        int i = part.indexOf('=');
                        if (i > 0) {
                            String k = part.substring(0, i).trim().toLowerCase();
                            String v = part.substring(i + 1);
                            if (k.equals("user") || k.equals("username")) m.put("user", v);
                            if (k.equals("password") || k.equals("pwd") || k.equals("pass")) m.put("password", v);
                        }
                    }
                    return m;
                }
                // Everything else: parse query string after '?'
                int q = u.indexOf('?');
                if (q >= 0 && q + 1 < u.length()) {
                    String query = u.substring(q + 1);
                    for (String kv : query.split("[&;]")) {
                        int i = kv.indexOf('=');
                        if (i > 0) {
                            String k = kv.substring(0, i).trim().toLowerCase();
                            String v = kv.substring(i + 1);
                            if (k.equals("user") || k.equals("username")) m.put("user", v);
                            if (k.equals("password") || k.equals("pwd") || k.equals("pass")) m.put("password", v);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return m;
        }

        private static Object findFieldByName(Object obj, String... names) {
            if (obj == null || names == null) return null;
            for (Class<?> k = obj.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    for (String n : names) {
                        if (f.getName().equals(n)) {
                            try {
                                f.setAccessible(true);
                                return f.get(obj);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
            return null;
        }

        private static Object findFieldByTypeName(Object obj, String... typeNameContains) {
            if (obj == null || typeNameContains == null) return null;
            for (Class<?> k = obj.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    String tn = f.getType().getName();
                    for (String frag : typeNameContains) {
                        if (tn.contains(frag)) {
                            try {
                                f.setAccessible(true);
                                return f.get(obj);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    // ==== SQL inliner ====
    static final class Sql {
        static String inlineWithParams(String sql, Map<Integer, Object> params, Vendor vendor) {
            if (sql == null || params == null || params.isEmpty()) return sql;
            int max = params.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<String> values = new ArrayList<>(Math.max(max, 0));
            for (int i = 1; i <= max; i++) values.add(inlineFormat(params.get(i), vendor));
            return inline(sql, values);
        }

        static String inline(String sql, List<String> formattedValues) {
            if (sql == null || formattedValues == null || formattedValues.isEmpty()) return sql;
            StringBuilder out = new StringBuilder(sql.length() + 64);
            int vi = 0;
            boolean inS = false, inD = false, inLC = false, inBC = false;
            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);
                char n = (i + 1 < sql.length() ? sql.charAt(i + 1) : '\0');
                if (!inS && !inD && !inBC && c == '-' && n == '-') {
                    inLC = true;
                    out.append(c);
                    continue;
                }
                if (inLC) {
                    out.append(c);
                    if (c == '\n') inLC = false;
                    continue;
                }
                if (!inS && !inD && !inLC && c == '/' && n == '*') {
                    inBC = true;
                    out.append(c);
                    continue;
                }
                if (inBC) {
                    out.append(c);
                    if (c == '*' && n == '/') {
                        out.append(n);
                        i++;
                        inBC = false;
                    }
                    continue;
                }
                if (!inD && c == '\'') {
                    out.append(c);
                    if (inS) {
                        if (n == '\'') {
                            out.append(n);
                            i++;
                        } else {
                            inS = false;
                        }
                    } else {
                        inS = true;
                    }
                    continue;
                }
                if (!inS && c == '\"') {
                    inD = !inD;
                    out.append(c);
                    continue;
                }
                if (!inS && !inD && c == '?' && vi < formattedValues.size()) {
                    out.append(formattedValues.get(vi++));
                    continue;
                }
                out.append(c);
            }
            return out.toString();
        }

        static String inlineFormat(Object v, Vendor vendor) {
            if (v == null) return "NULL";
            if (v instanceof Number) return String.valueOf(v);
            if (v instanceof Boolean) return ((Boolean) v) ? "TRUE" : "FALSE";
            if (v instanceof java.sql.Date) return "DATE '" + v + "'";
            if (v instanceof java.sql.Time) return "TIME '" + v + "'";
            if (v instanceof java.sql.Timestamp) {
                java.time.LocalDateTime ldt = ((java.sql.Timestamp) v).toLocalDateTime();
                return "TIMESTAMP '" + ldt.toString().replace('T', ' ') + "'";
            }
            if (v instanceof java.time.LocalDate) return "DATE '" + v + "'";
            if (v instanceof java.time.LocalDateTime) return "TIMESTAMP '" + v.toString().replace('T', ' ') + "'";
            if (v instanceof java.time.Instant) {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant((java.time.Instant) v, java.time.ZoneId.systemDefault());
                return "TIMESTAMP '" + ldt.toString().replace('T', ' ') + "'";
            }
            if (v instanceof java.util.UUID) return '\'' + esc(String.valueOf(v)) + '\'';
            if (v instanceof byte[]) {
                int max = Integer.getInteger("sql.agent.maxBytes", 256);
                byte[] b = (byte[]) v;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(b.length, max); i++) sb.append(String.format("%02X", b[i]));
                String hex = sb.toString();
                switch (vendor) {
                    case MYSQL:
                    case SQLITE:
                    case DB2:
                        return "X'" + hex + (b.length > max ? "…" : "") + "'";
                    case POSTGRES:
                        return "'\\x" + hex + (b.length > max ? "…" : "") + "'::bytea";
                    default:
                        return "0x" + hex + (b.length > max ? "…" : "");
                }
            }
            if (v instanceof java.sql.Array) {
                try {
                    Object arr = ((java.sql.Array) v).getArray();
                    if (arr instanceof Object[]) {
                        Object[] a = (Object[]) arr;
                        List<String> parts = new ArrayList<>();
                        for (Object o : a) parts.add(inlineFormat(o, vendor));
                        return '(' + String.join(", ", parts) + ')';
                    }
                } catch (Throwable ignored) {
                }
                return "ARRAY[...]";
            }
            if (v.getClass().isArray()) {
                int len = java.lang.reflect.Array.getLength(v);
                List<String> parts = new ArrayList<>(len);
                for (int i = 0; i < len; i++) parts.add(inlineFormat(java.lang.reflect.Array.get(v, i), vendor));
                return '(' + String.join(", ", parts) + ')';
            }
            if (v instanceof Iterable) {
                List<String> parts = new ArrayList<>();
                for (Object o : (Iterable<?>) v) parts.add(inlineFormat(o, vendor));
                return '(' + String.join(", ", parts) + ')';
            }
            return '\'' + esc(String.valueOf(v)) + '\'';
        }
    }

    // ==== JSON + logging ====
    static final class Json {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        static Json start() {
            Json j = new Json();
            j.sb.append('{');
            return j;
        }

        Json kv(String k, String v) {
            return kvRaw(k, v == null ? null : esc(v));
        }

        Json kvRaw(String k, String raw) {
            if (!first) sb.append(',');
            first = false;
            sb.append('\"').append(esc(k)).append('\"').append(':');
            if (raw == null) sb.append("null");
            else if (isRaw(raw)) sb.append(raw);
            else sb.append('\"').append(raw).append('\"');
            return this;
        }

        String end() {
            return sb.append('}').toString();
        }

        static boolean isRaw(String r) {
            return r.startsWith("{") || r.startsWith("[") || r.equals("true") || r.equals("false") || r.matches("^-?\\d+(\\.\\d+)?$");
        }

        static String params(Map<Integer, Object> params) {
            StringBuilder p = new StringBuilder();
            p.append('{');
            boolean f = true;
            for (Map.Entry<Integer, Object> e : params.entrySet()) {
                if (!f) p.append(',');
                f = false;
                p.append('\"').append(e.getKey()).append('\"').append(':');
                Object v = e.getValue();
                if (v == null) p.append("null");
                else if (v instanceof Number || v instanceof Boolean) p.append(String.valueOf(v));
                else p.append('\"').append(esc(String.valueOf(v))).append('\"');
            }
            p.append('}');
            return p.toString();
        }
    }

    static final class Log {
        static String path;
        private static BlockingQueue<String> Q;
        private static Thread T;

        static synchronized void init(String p) {
            path = p;
            try {
                File f = new File(p);
                File dir = f.getParentFile();
                if (dir != null) dir.mkdirs();
            } catch (Throwable ignored) {
            }
            Q = new LinkedBlockingQueue<>();
            T = new Thread(Log::drain, "sql-agent-writer");
            T.setDaemon(true);
            T.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Q.put("__EOF__");
                } catch (InterruptedException ignored) {
                }
            }));
        }

        static void json(String line) {
            try {
                Q.put(line);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        static void drain() {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8, true))) {
                while (true) {
                    String line = Q.take();
                    if ("__EOF__".equals(line)) {
                        List<String> rest = new ArrayList<>();
                        Q.drainTo(rest);
                        for (String l : rest) {
                            w.write(l);
                            w.newLine();
                        }
                        w.flush();
                        break;
                    }
                    w.write(line);
                    w.newLine();
                    w.flush();
                }
            } catch (IOException | InterruptedException e) {
                throw new UncheckedIOException(e instanceof IOException ? (IOException) e : new IOException(e));
            }
        }
    }

    // utils
    static String stack(Throwable t) {
        StringBuilder sb = new StringBuilder();
        if (t == null) return "";
        sb.append(String.valueOf(t));
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append("\n\tat ").append(ste);
        }
        Throwable c = t.getCause();
        while (c != null) {
            sb.append("\nCaused by: ").append(c);
            for (StackTraceElement ste : c.getStackTrace()) {
                sb.append("\n\tat ").append(ste);
            }
            c = c.getCause();
        }
        return sb.toString();
    }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
    static String nz(String s) {
        return s == null ? "" : s;
    }
}