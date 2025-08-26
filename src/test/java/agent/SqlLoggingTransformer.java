package agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class SqlLoggingTransformer implements ClassFileTransformer {

    // We target the specific implementation class for the H2 driver's Statement.
    // A more advanced agent might discover these classes dynamically.
    private static final String TARGET_CLASS_NAME = "org.h2.jdbc.JdbcStatement";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // We check if the class being loaded is our target class.
        // Class names are passed with '/' instead of '.' as separators.
        if (!TARGET_CLASS_NAME.replace('.', '/').equals(className)) {
            return null; // If not our target, we don't transform it.
        }

        try {
            // Use Javassist to parse the class file buffer.
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(TARGET_CLASS_NAME);

            // Intercept the 'executeQuery' method.
            CtMethod executeQuery = cc.getDeclaredMethod("executeQuery");
            // Add code to be executed before the original method body.
            // '$1' refers to the first parameter of the intercepted method (the SQL string).
            executeQuery.insertBefore("System.out.println(\"AGENT ==> Executing Query: \" + $1);");

            // Intercept the 'executeUpdate' method.
            CtMethod executeUpdate = cc.getDeclaredMethod("executeUpdate");
            executeUpdate.insertBefore("System.out.println(\"AGENT ==> Executing Update: \" + $1);");

            // Intercept the 'execute' method.
            CtMethod execute = cc.getDeclaredMethod("execute");
            execute.insertBefore("System.out.println(\"AGENT ==> Executing: \" + $1);");

            System.out.println("====== Transformed " + TARGET_CLASS_NAME + " ======");

            // Return the modified bytecode.
            return cc.toBytecode();

        } catch (Exception e) {
            System.err.println("Agent failed to transform " + className);
            e.printStackTrace();
            return null;
        }
    }
}
