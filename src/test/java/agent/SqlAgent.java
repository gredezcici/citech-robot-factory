package agent;
import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
public class SqlAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("--- SQL Interception Agent (Javassist) is running ---");
        inst.addTransformer(new SqlTransformer());
    }

    static class SqlTransformer implements ClassFileTransformer {
        private final ClassPool pool = ClassPool.getDefault();

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            // Convert class name from JVM format (e.g., com/example/MyClass) to Java format (e.g., com.example.MyClass)
            String dotClassName = className.replaceAll("/", ".");

            try {
                // --- Interceptor 1: MyBatis Executor ---
                if ("org.apache.ibatis.executor.BaseExecutor".equals(dotClassName) || "org.apache.ibatis.executor.CachingExecutor".equals(dotClassName)) {
                    CtClass ctClass = pool.get(dotClassName);

                    // Intercept update(MappedStatement, Object)
                    CtMethod updateMethod = ctClass.getDeclaredMethod("update");
                    updateMethod.insertBefore("com.example.agent.MyBatisInterceptorUtil.intercept($1, $2, \"update\");");

                    // Intercept query(MappedStatement, Object, RowBounds, ResultHandler)
                    CtMethod queryMethod = ctClass.getDeclaredMethod("query", new CtClass[]{
                            pool.get("org.apache.ibatis.mapping.MappedStatement"),
                            pool.get("java.lang.Object"),
                            pool.get("org.apache.ibatis.session.RowBounds"),
                            pool.get("org.apache.ibatis.session.ResultHandler")
                    });
                    queryMethod.insertBefore("com.example.agent.MyBatisInterceptorUtil.intercept($1, $2, \"query\");");

                    return ctClass.toBytecode();
                }

                // --- Interceptor 2: Generic JDBC PreparedStatement ---
                // We need to check if the class implements PreparedStatement
                CtClass ctClass = pool.get(dotClassName);
                if (ctClass.isInterface()) {
                    return null; // Don't modify interfaces
                }

                CtClass[] interfaces = ctClass.getInterfaces();
                boolean isPreparedStatement = false;
                for (CtClass anInterface : interfaces) {
                    if ("java.sql.PreparedStatement".equals(anInterface.getName())) {
                        isPreparedStatement = true;
                        break;
                    }
                }

                if (isPreparedStatement) {
                    // Intercept methods with no arguments
                    for (CtMethod method : ctClass.getDeclaredMethods()) {
                        if (("execute".equals(method.getName()) || "executeQuery".equals(method.getName()) || "executeUpdate".equals(method.getName()))
                                && method.getParameterTypes().length == 0) {
                            // $0 refers to 'this'
                            method.insertBefore("com.example.agent.JdbcInterceptorUtil.intercept($0, \"" + method.getName() + "\");");
                        }
                    }
                    return ctClass.toBytecode();
                }

            } catch (NotFoundException e) {
                // This is expected for classes we don't intend to transform.
            } catch (CannotCompileException | java.io.IOException e) {
                System.err.println("Error transforming class " + dotClassName + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Return null if no transformation was applied
            return null;
        }
    }
}
