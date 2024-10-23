package jmeter;

import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.protocol.jdbc.config.DataSourceElement;
import org.apache.jmeter.protocol.jdbc.sampler.JDBCSampler;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;


import java.io.FileOutputStream;

public class JMeterJDBCTestPlanWithCSV {
    public static void main(String[] args) throws Exception {
        // Initialize JMeter Properties

        JMeterUtils.loadJMeterProperties("/Users/chaochen/Downloads/apache-jmeter-5.6.3/bin/jmeter.properties");
        JMeterUtils.setJMeterHome("/Users/chaochen/Downloads/apache-jmeter-5.6.3/");

        JMeterUtils.initLocale();

        // Create Test Plan Tree
        HashTree testPlanTree = new HashTree();

        // Loop Controller (optional)
        LoopController loopController = new LoopController();
        loopController.setLoops(1);  // Number of iterations
        loopController.setFirst(true);
        loopController.initialize();

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setNumThreads(1);   // Number of threads (users)
        threadGroup.setRampUp(1);       // Ramp-up period
        threadGroup.setSamplerController(loopController);  // Attach Loop Controller

        // CSV Data Set Config
        CSVDataSet csvDataSet = new CSVDataSet();
        csvDataSet.setName("CSV Data Set Config");
        csvDataSet.setProperty("filename", "/path/to/your/csvfile.csv");  // Path to your CSV file
        csvDataSet.setProperty("variableNames", "column1,column2");  // Column names in your CSV file
        csvDataSet.setProperty("delimiter", ",");  // CSV delimiter
        csvDataSet.setProperty("recycle", "true");  // Whether to loop over the CSV data
        csvDataSet.setProperty("stopThread", "false");  // Whether to stop the thread when the end of the file is reached
        csvDataSet.setProperty("ignoreFirstLine", "false");  // Set to true if the first line is a header row

        // JDBC Connection Configuration
        DataSourceElement jdbcConfig = new DataSourceElement();
        jdbcConfig.setName("JDBC Connection Config");
        jdbcConfig.setDataSource("myDbConnection");  // Name of the DataSource
        jdbcConfig.setProperty("dbUrl", "jdbc:mysql://localhost:3306/mydb");  // Database URL
        jdbcConfig.setProperty("driver", "com.mysql.cj.jdbc.Driver");  // JDBC driver class
        jdbcConfig.setProperty("username", "myuser");  // Database username
        jdbcConfig.setProperty("password", "mypassword");  // Database password
        jdbcConfig.setProperty("poolMax", "10");  // Max pool size (optional)

        // JDBC Request (Sampler)
        JDBCSampler jdbcSampler = new JDBCSampler();
        jdbcSampler.setProperty("name", "JDBC Request Sampler");
        jdbcSampler.setProperty("dbName", "myDbConnection");  // The data source name set in JDBC Connection Config
        jdbcSampler.setProperty("query type","Select Statement");  // Query Type: Select Statement, Update, etc.
        // Use CSV variables in the query
        jdbcSampler.setProperty("query","SELECT * FROM my_table WHERE column1 = '${column1}' AND column2 = '${column2}';");  // SQL query using CSV variables

        // Constant Timer (Adding Delay)
        ConstantTimer timer = new ConstantTimer();
        timer.setDelay("5000");  // Delay in milliseconds (e.g., 5000 ms = 5 seconds)

        // Build the test plan
        HashTree threadGroupHashTree = testPlanTree.add(threadGroup);
        testPlanTree.add(jdbcConfig);
        testPlanTree.add(jdbcSampler);
        threadGroupHashTree.add(timer);
        threadGroupHashTree.add(csvDataSet);    // Add CSV Data Set to Thread Group
                // Add Timer to Thread Group
        testPlanTree.add(threadGroupHashTree);
        // Save the Test Plan to a JMX file
        SaveService.saveTree(testPlanTree, new FileOutputStream("jdbc_testplan_with_csv1.jmx"));

        System.out.println("JMeter JDBC Test Plan with CSV Data Set created programmatically!");
    }
}
