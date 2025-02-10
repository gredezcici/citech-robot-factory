package jmeter;

import org.apache.jmeter.control.ThroughputController;
import org.apache.jmeter.protocol.jdbc.sampler.JDBCSampler;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;

import java.io.FileOutputStream;
import java.io.IOException;

public class JMXCreator {
    // Method to add a Throughput Controller to the test plan
    public static void addThroughputController(HashTree testPlanTree) throws IOException {
        // Create a ThreadGroup for the ThroughputController
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Example Thread Group");
        threadGroup.setNumThreads(10);  // Set the number of threads
        threadGroup.setRampUp(5);       // Set the ramp-up period in seconds
        HashTree threadGroupTree = testPlanTree.add(threadGroup);

        // Create the ThroughputController
        ThroughputController throughputController = new ThroughputController();
        throughputController.setName("Throughput Controller");

        // Set the execution mode (use Percent Executions, which is the most common)
        throughputController.setStyle(ThroughputController.BYPERCENT);
        throughputController.setPercentThroughput(50);  // Set to 50% executions

        // Add the ThroughputController to the ThreadGroup
        HashTree controllerTree = threadGroupTree.add(throughputController);

        // Add child elements to the ThroughputController
        // Example: Adding an HTTP Sampler or JDBC Sampler
        // Create your sampler here
        JDBCSampler jdbcSampler = new JDBCSampler();
        jdbcSampler.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        jdbcSampler.setProperty(TestElement.TEST_CLASS, "JDBCSampler");

        jdbcSampler.setProperty("name", "JDBC Request Sampler");
        jdbcSampler.setProperty("dbName", "myDbConnection");  // The data source name set in JDBC Connection Config
        jdbcSampler.setProperty("query type", "Select Statement");  // Query Type: Select Statement, Update, etc.
        // Use CSV variables in the query
        jdbcSampler.setProperty("query", "SELECT * FROM my_table WHERE column1 = '${column1}' AND column2 = '${column2}';");  // SQL query using CSV variables

        controllerTree.add(jdbcSampler);

    }

    // Example function to create a sampler (e.g., HTTP Sampler or JDBC Sampler)

}
