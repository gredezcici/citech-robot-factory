package agent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javassist.*;

/**
 * Java agent that intercepts JDBC queries (Statement/PreparedStatement) and
 * exports them as JSON Lines to a file. This version uses **Javassist** for
 * bytecode instrumentation instead of ByteBuddy.
 *
 * Build as a javaagent JAR with MANIFEST.MF containing:
 *   Premain-Class: com.example.jdbcagent.JdbcQueryAgent
 *   Can-Redefine-Classes: true
 *   Can-Retransform-Classes: true
 *
 * Run with for example:
 *   java \
 *   -javaagent:/path/jdbc-query-agent.jar \
 *   -Djdbc.agent.out=/var/log/jdbc-queries.jsonl \
 *   -jar your-app.jar
 */
public final class JdbcQueryAgent {
    private JdbcQueryAgent() {}

    // === Agent entry ===
    public static void premain(String agentArgs, Instrumentation inst) {
        final String outPath = System.getProperty("jdbc.agent.out", "jdbc-queries.jsonl");
        QueryLogWriter.init(outPath);

        inst.addTransformer(new JdbcTransformer(), true);

        // (Optional) retransform already-loaded JDBC driver classes
        try {
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (!inst.isModifiableClass(c)) continue;
                String n = c.getName();
                if (ignore(n)) continue;
                if (implementsAnyJdbc(c)) {
                    try { inst.retransformClasses(c); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        QueryLogWriter.get().logJson("{\"type\":\"agent-start\",\"impl\":\"javassist\",\"out\":\"" + escape(outPath) + "\"}");
    }

    static boolean implementsAnyJdbc(Class<?> c) {
        try {
            for (Class<?> itf : c.getInterfaces()) {
                if (itf == java.sql.Connection.class || itf == java.sql.PreparedStatement.class || itf == java.sql.Statement.class) return true;
            }
            Class<?> s = c.getSuperclass();
            return s != null && implementsAnyJdbc(s);
        } catch (Throwable t) { return false; }
    }

    static boolean ignore(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") ||
                name.startsWith("sun.")  || name.startsWith("com.sun.") ||
                name.startsWith("com.example.jdbcagent.");
    }

    // === Transformer ===
    static final class JdbcTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classfileBuffer) {
            if (className == null) return null;
            String dotted = className.replace('/', '.');
            if (ignore(dotted)) return null;

            ClassPool pool = ClassPool.getDefault();
            if (loader != null) pool.insertClassPath(new LoaderClassPath(loader));

            CtClass ct;
            try {
                ct = pool.get(dotted);
            } catch (NotFoundException e) {
                return null;
            }

            try {
                if (ct.isInterface() || ct.isAnnotation() || ct.isEnum()) return null;

                boolean modified = false;
                boolean isConn = subtypeOf(pool, ct, "java.sql.Connection");
                boolean isPrep = subtypeOf(pool, ct, "java.sql.PreparedStatement");
                boolean isStmt = subtypeOf(pool, ct, "java.sql.Statement");

                if (isConn) modified |= instrumentConnection(pool, ct);
                if (isPrep) modified |= instrumentPreparedStatement(pool, ct);
                if (isStmt) modified |= instrumentStatement(pool, ct);

                if (!modified) return null;

                byte[] out = ct.toBytecode();
                ct.detach();
                return out;
            } catch (Throwable t) {
                try {
                    QueryLogWriter.get().logJson("{\"type\":\"agent-error\",\"class\":\"" + escape(dotted) + "\",\"error\":\"" + escape(String.valueOf(t)) + "\"}");
                } catch (Throwable ignored) {}
                try { ct.detach(); } catch (Throwable ignored) {}
                return null;
            }
        }
    }

    static boolean subtypeOf(ClassPool pool, CtClass ct, String target) {
        try { return ct.subtypeOf(pool.get(target)); } catch (NotFoundException e) { return false; }
    }

    // === Instrumentation helpers ===
    static boolean instrumentConnection(ClassPool pool, CtClass ct) throws Exception {
        boolean modified = false;
        for (CtMethod m : ct.getDeclaredMethods()) {
            String name = m.getName();
            if (!("prepareStatement".equals(name) || "prepareCall".equals(name))) continue;
            CtClass[] params = m.getParameterTypes();
            if (params.length >= 1 && params[0].getName().equals("java.lang.String")) {
                // On successful return, map Prepared/CallableStatement -> SQL
                String code = "if ($_ != null && $1 != null) " + JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.register($_, $1);";
                try { m.insertAfter("{ " + code + " }", false); modified = true; } catch (CannotCompileException ignored) {}
            }
        }
        return modified;
    }

    static boolean instrumentPreparedStatement(ClassPool pool, CtClass ct) throws Exception {
        boolean modified = false;
        for (CtMethod m : ct.getDeclaredMethods()) {
            String name = m.getName();
            CtClass[] p = m.getParameterTypes();

            // setXxx(int, ...)
            if (name.startsWith("set") && p.length >= 1 && CtClass.intType.equals(p[0])) {
                String code;
                if ("setNull".equals(name)) {
                    code = JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.setParam($0, $1, (Object)null);";
                } else {
                    // Value is the second argument by convention
                    code = JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.setParam($0, $1, $2);";
                }
                try { m.insertBefore("{ " + code + " }" ); modified = true; } catch (CannotCompileException ignored) {}
            }

            // clearParameters()
            if ("clearParameters".equals(name) && p.length == 0) {
                String code = JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.clearParams($0);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }

            // addBatch() on PS (no-arg)
            if ("addBatch".equals(name) && p.length == 0) {
                String code = JdbcQueryAgent.class.getName() + ".onAddBatch($0, $args);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }

            // execute*
            if (name.startsWith("execute")) {
                String code = JdbcQueryAgent.class.getName() + ".onExecute($0, \"" + name + "\", $args);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }

            // close()
            if ("close".equals(name) && p.length == 0) {
                String code = JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.clearAll($0);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }
        }
        return modified;
    }

    static boolean instrumentStatement(ClassPool pool, CtClass ct) throws Exception {
        boolean modified = false;
        for (CtMethod m : ct.getDeclaredMethods()) {
            String name = m.getName();
            CtClass[] p = m.getParameterTypes();

            // addBatch(String) on Statement
            if ("addBatch".equals(name) && p.length == 1 && p[0].getName().equals("java.lang.String")) {
                String code = JdbcQueryAgent.class.getName() + ".onAddBatch($0, $args);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }

            // execute*
            if (name.startsWith("execute")) {
                String code = JdbcQueryAgent.class.getName() + ".onExecute($0, \"" + name + "\", $args);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }

            // close()
            if ("close".equals(name) && p.length == 0) {
                String code = JdbcQueryAgent.class.getName() + ".QueryCaptureRegistry.clearAll($0);";
                try { m.insertBefore("{ " + code + " }"); modified = true; } catch (CannotCompileException ignored) {}
            }
        }
        return modified;
    }

    // === Hooks invoked from injected bytecode ===
    public static void onAddBatch(Object self, Object[] args) {
        try {
            String sql = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                sql = (String) args[0];
            }
            if (sql != null) QueryCaptureRegistry.register(self, sql);
            QueryCaptureRegistry.markBatch(self, sql);
        } catch (Throwable ignored) {}
    }

    public static void onExecute(Object self, String method, Object[] args) {
        try {
            String sql = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                sql = (String) args[0];
            } else {
                sql = QueryCaptureRegistry.lookupSql(self);
            }
            Map<Integer, Object> params = QueryCaptureRegistry.snapshotParams(self);
            boolean batch = QueryCaptureRegistry.isInBatch(self);

            String json = JsonBuilder.start()
                    .kv("type", "query")
                    .kv("ts", Instant.now().toString())
                    .kv("thread", Thread.currentThread().getName())
                    .kv("method", method)
                    .kv("driverClass", self.getClass().getName())
                    .kv("batch", batch)
                    .kvRaw("sql", sql == null ? null : escape(sql))
                    .kvRaw("params", JsonBuilder.params(params))
                    .end();
            QueryLogWriter.get().logJson(json);
        } catch (Throwable t) {
            try {
                QueryLogWriter.get().logJson("{\"type\":\"agent-error\",\"when\":\"execute\",\"error\":\"" + escape(String.valueOf(t)) + "\"}");
            } catch (Throwable ignored) {}
        } finally {
            try { QueryCaptureRegistry.clearBatch(self); } catch (Throwable ignored) {}
        }
    }

    // === Registry and Logging (same as ByteBuddy version) ===

    static final class QueryCaptureRegistry {
        private static final Map<Object, String> STMT_SQL = Collections.synchronizedMap(new WeakHashMap<>());
        private static final Map<Object, Map<Integer, Object>> STMT_PARAMS = Collections.synchronizedMap(new WeakHashMap<>());
        private static final Map<Object, Boolean> STMT_BATCH = Collections.synchronizedMap(new WeakHashMap<>());

        static void register(Object stmt, String sql) { if (stmt != null && sql != null) STMT_SQL.put(stmt, sql); }
        static String lookupSql(Object stmt) { return STMT_SQL.get(stmt); }
        static void setParam(Object stmt, int index, Object value) {
            Map<Integer, Object> m = STMT_PARAMS.get(stmt);
            if (m == null) { m = Collections.synchronizedMap(new TreeMap<>()); STMT_PARAMS.put(stmt, m); }
            m.put(index, value);
        }
        static void clearParams(Object stmt) { STMT_PARAMS.remove(stmt); }
        static Map<Integer, Object> snapshotParams(Object stmt) {
            Map<Integer, Object> m = STMT_PARAMS.get(stmt);
            if (m == null) return Collections.emptyMap();
            synchronized (m) { return new TreeMap<>(m); }
        }
        static void markBatch(Object stmt, String sql) { if (sql != null) STMT_SQL.put(stmt, sql); STMT_BATCH.put(stmt, Boolean.TRUE); }
        static boolean isInBatch(Object stmt) { return Boolean.TRUE.equals(STMT_BATCH.get(stmt)); }
        static void clearBatch(Object stmt) { STMT_BATCH.remove(stmt); }
        static void clearAll(Object stmt) { STMT_SQL.remove(stmt); STMT_PARAMS.remove(stmt); STMT_BATCH.remove(stmt); }
    }

    static final class QueryLogWriter {
        private static QueryLogWriter INSTANCE;
        static synchronized void init(String path) { if (INSTANCE == null) INSTANCE = new QueryLogWriter(path); }
        static QueryLogWriter get() { if (INSTANCE == null) throw new IllegalStateException("QueryLogWriter not initialized"); return INSTANCE; }

        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private final String path;

        private QueryLogWriter(String path) {
            this.path = path;
            Thread t = new Thread(this::drainLoop, "jdbc-agent-writer");
            t.setDaemon(true); t.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { queue.put("__EOF__"); } catch (InterruptedException ignored) {} }));
        }

        void logJson(String json) { try { queue.put(json); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

        private void drainLoop() {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8, true))) {
                while (true) { String line = queue.take(); if ("__EOF__".equals(line)) break; w.write(line); w.newLine(); w.flush(); }
            } catch (IOException | InterruptedException e) { throw new UncheckedIOException(e instanceof IOException ? (IOException) e : new IOException(e)); }
        }
    }

    // === Minimal JSON helpers ===
    static String escape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\""); break;
                case '\\': sb.append("\\"); break;
                case '\n': sb.append("\n"); break;
                case '\r': sb.append("\r"); break;
                case '\t': sb.append("\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.toString();
    }

    static final class JsonBuilder {
        private final StringBuilder sb = new StringBuilder();
        private boolean first = true;
        static JsonBuilder start() { return new JsonBuilder().raw("{"); }
        JsonBuilder raw(String r) { sb.append(r); return this; }
        JsonBuilder kv(String k, String v) { return kvRaw(k, v == null ? null : escape(v)); }
        JsonBuilder kv(String k, boolean v) { return kvRaw(k, String.valueOf(v)); }
        JsonBuilder kvRaw(String k, String raw) {
            if (!first) sb.append(','); first = false;
            sb.append('"').append(escape(k)).append('"').append(':');
            if (raw == null) sb.append("null");
            else if (raw.startsWith("[") || raw.startsWith("{") || raw.equals("true") || raw.equals("false") || raw.matches("^-?\\d+(\\.\\d+)?$")) sb.append(raw);
            else sb.append('"').append(raw).append('"');
            return this;
        }
        String end() { return raw("}").toString(); }

        static String params(Map<Integer, Object> params) {
            StringBuilder p = new StringBuilder(); p.append('{'); boolean first = true;
            for (Map.Entry<Integer, Object> e : params.entrySet()) {
                if (!first) p.append(','); first = false;
                p.append('"').append(e.getKey()).append('"').append(':');
                Object v = e.getValue();
                if (v == null) { p.append("null"); }
                else if (v instanceof Number || v instanceof Boolean) { p.append(String.valueOf(v)); }
                else { p.append('"').append(escape(String.valueOf(v))).append('"'); }
            }
            p.append('}'); return p.toString();
        }
    }
}