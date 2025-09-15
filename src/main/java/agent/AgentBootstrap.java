package agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public final class AgentBootstrap {

    private static final String CORE_AGENT_CLASS = "agent.UnifiedFinalSqlAgentV5";
    private static final String CORE_AGENT_METHOD = "premain";
    private static final String LIBS_DIR = "libs/";

    public static void premain(String agentArgs, Instrumentation inst) {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Find the location of our own agent JAR
            final File agentJarFile = new File(AgentBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // Create a list of URLs for our custom classloader, pointing to the JARs inside our agent JAR
            final List<URL> dependencyUrls = new ArrayList<>();
            dependencyUrls.add(agentJarFile.toURI().toURL()); // Add the agent JAR itself

            try (JarFile jarFile = new JarFile(agentJarFile)) {
                jarFile.stream()
                        .filter(entry -> !entry.isDirectory() && entry.getName().startsWith(LIBS_DIR) && entry.getName().endsWith(".jar"))
                        .forEach(entry -> {
                            try {
                                // Create a URL like: jar:file:/path/to/agent.jar!/libs/dependency.jar
                                URL jarUrl = new URL("jar:" + agentJarFile.toURI().toURL() + "!/" + entry.getName());
                                dependencyUrls.add(jarUrl);
                            } catch (Exception e) {
                                System.err.println("SQL Agent Bootstrap Error: Failed to create URL for " + entry.getName());
                                e.printStackTrace();
                            }
                        });
            }

            // Create the isolated classloader with no parent to ensure full isolation
            final URLClassLoader agentClassLoader = new URLClassLoader(dependencyUrls.toArray(new URL[0]), null);
            Thread.currentThread().setContextClassLoader(agentClassLoader);

            // Load the real agent class and invoke its entry point via reflection
            final Class<?> coreAgentClass = agentClassLoader.loadClass(CORE_AGENT_CLASS);
            final Method agentEntryMethod = coreAgentClass.getMethod(CORE_AGENT_METHOD, String.class, Instrumentation.class);
            agentEntryMethod.invoke(null, agentArgs, inst);

        } catch (Exception e) {
            System.err.println("SQL Agent Bootstrap Critical Error: Failed to start agent core.");
            e.printStackTrace();
        } finally {
            // CRITICAL: Always restore the original context classloader
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }
}