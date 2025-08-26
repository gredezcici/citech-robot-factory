package agent;

import java.lang.instrument.Instrumentation;

public class SqlLoggingAgent {

    /**
     * This method is the agent's entry point.
     *
     * @param agentArgs Agent arguments passed from the command line.
     * @param inst      The instrumentation instance provided by the JVM.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("====== SQL Logging Agent Initialized ======");
        // We add our custom transformer to the instrumentation instance.
        // This transformer will be called for every class that is loaded by the JVM.
        inst.addTransformer(new SqlLoggingTransformer());
    }
}