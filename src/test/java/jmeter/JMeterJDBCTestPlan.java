package jmeter;

/**
 * @author Chao Chen
 */

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.protocol.jdbc.config.DataSourceElement;
import org.apache.jmeter.protocol.jdbc.sampler.JDBCSampler;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.timers.poissonarrivals.PreciseThroughputTimer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.FileOutputStream;

public class JMeterJDBCTestPlan {

    public static void main(String[] args) throws Exception {
        // Step 1: Create a Test Plan
        JMeterUtils.loadJMeterProperties("/Users/chaochen/Downloads/apache-jmeter-5.6.3/bin/jmeter.properties");
        JMeterUtils.setJMeterHome("/Users/chaochen/Downloads/apache-jmeter-5.6.3/");

        JMeterUtils.initLocale();
        TestPlan testPlan = new TestPlan("Test Plan");
        testPlan.setProperty(TestElement.GUI_CLASS, "TestPlanGui");
        testPlan.setProperty(TestElement.TEST_CLASS, "TestPlan");
        testPlan.setProperty(TestElement.NAME, "Test Plan");
        // Step 2: Create Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setProperty(TestElement.GUI_CLASS, "ThreadGroupGui");
        threadGroup.setProperty(TestElement.TEST_CLASS, "ThreadGroup");
        threadGroup.setProperty(TestElement.NAME, "Thread Group");
        threadGroup.setName("Thread Group");
        threadGroup.setNumThreads(1);  // Number of users
        threadGroup.setRampUp(1);      // Ramp-up time
        threadGroup.setProperty("ThreadGroup.same_user_on_next_iteration", true);
        threadGroup.setProperty("ThreadGroup.on_sample_error", "continue");

        // Loop Controller
        Arguments loopController = new Arguments();
        loopController.setName("Loop Controller");
        loopController.addArgument("Loops", "1");
        loopController.addArgument("Continue Forever", "false");
        HashTree loopControllerTree = new HashTree();
        loopControllerTree.add(loopController);

        // Step 3: Create HashTree for the test plan
        HashTree testPlanTree = new HashTree();
        HashTree threadGroupTree = testPlanTree.add(testPlan, threadGroup);

        // Step 4: JDBC Sampler 1
        JDBCSampler jdbcSampler1 = new JDBCSampler();
        jdbcSampler1.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        jdbcSampler1.setProperty(TestElement.TEST_CLASS, "JDBCSampler");
        jdbcSampler1.setProperty(TestElement.NAME, "JDBC Request");
        jdbcSampler1.setName("JDBC Request");
        jdbcSampler1.setQuery("select 1");
        jdbcSampler1.setQueryType("Select Statement");
        jdbcSampler1.setResultSetHandler("Store as String");

        HashTree jdbcSampler1Tree = threadGroupTree.add(jdbcSampler1);
        // CSV Data Set 1
        CSVDataSet csvDataSet1 = new CSVDataSet();
        csvDataSet1.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        csvDataSet1.setProperty(TestElement.TEST_CLASS, "CSVDataSet");
        csvDataSet1.setProperty(TestElement.NAME, "CSV Data Set Config 1");
        csvDataSet1.setName("CSV Data Set Config 1");
        csvDataSet1.setProperty("delimiter", ",");
        csvDataSet1.setProperty("ignoreFirstLine", "false");
        csvDataSet1.setProperty("recycle", "true");
        csvDataSet1.setProperty("shareMode", "shareMode.thread");
        jdbcSampler1Tree.add(csvDataSet1);

        // Precise Throughput Timer
        PreciseThroughputTimer throughputTimer = new PreciseThroughputTimer();
        throughputTimer.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        throughputTimer.setProperty(TestElement.TEST_CLASS, "PreciseThroughputTimer");
        throughputTimer.setProperty(TestElement.NAME, "Precise Throughput Timer");
        throughputTimer.setName("Precise Throughput Timer");
        throughputTimer.setProperty("throughput", "100.0");
        throughputTimer.setProperty("throughputPeriod", "3600");
        throughputTimer.setProperty("duration", "3600");
        jdbcSampler1Tree.add(throughputTimer);

//        // Constant Timer
//        ConstantTimer constantTimer1 = new ConstantTimer();
//        constantTimer1.setName("Constant Timer");
//        constantTimer1.setProperty("ConstantTimer.delay", "300");
//        jdbcSampler1Tree.add(constantTimer1);

        // Step 5: JDBC Sampler 2
        JDBCSampler jdbcSampler2 = new JDBCSampler();
        jdbcSampler2.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        jdbcSampler2.setProperty(TestElement.TEST_CLASS, "JDBCSampler");
        jdbcSampler2.setProperty(TestElement.NAME, "JDBC Request");
        jdbcSampler2.setName("JDBC Request");
        jdbcSampler2.setQueryType("Select Statement");
        jdbcSampler2.setResultSetHandler("Store as String");

        HashTree jdbcSampler2Tree = threadGroupTree.add(jdbcSampler2);

        // CSV Data Set 2
        CSVDataSet csvDataSet2 = new CSVDataSet();
        csvDataSet2.setProperty(TestElement.GUI_CLASS, "TestBeanGUI");
        csvDataSet2.setProperty(TestElement.TEST_CLASS, "CSVDataSet");
        csvDataSet2.setProperty(TestElement.NAME, "CSV Data Set Config 1");
        csvDataSet2.setName("CSV Data Set Config 2");
        csvDataSet2.setProperty("ignoreFirstLine", "false");
        csvDataSet2.setProperty("delimiter", ",");
        csvDataSet2.setProperty("recycle", "true");
        csvDataSet2.setProperty("shareMode", "shareMode.all");
        jdbcSampler2Tree.add(csvDataSet2);

        // Step 6: JDBC Connection Configuration 1
        DataSourceElement jdbcConfig1 = new DataSourceElement();
        jdbcConfig1.setName("JDBC Connection Configuration 1");
        jdbcConfig1.setProperty("autocommit", "true");
        jdbcConfig1.setProperty("timeout", "10000");
        jdbcConfig1.setProperty("keepAlive", "true");
        jdbcConfig1.setProperty("connectionAge", "5000");
        jdbcConfig1.setProperty("dataSource", "v1");

        testPlanTree.add(jdbcConfig1);

        // Step 7: JDBC Connection Configuration 2
        DataSourceElement jdbcConfig2 = new DataSourceElement();
        jdbcConfig2.setName("JDBC Connection Configuration 2");
        jdbcConfig2.setProperty("autocommit", "true");
        jdbcConfig2.setProperty("timeout", "10000");
        jdbcConfig2.setProperty("keepAlive", "true");
        jdbcConfig2.setProperty("connectionAge", "5000");
        jdbcConfig2.setProperty("dataSource", "v2");

        testPlanTree.add(jdbcConfig2);

        // Step 8: Save the Test Plan to a JMX file
        SaveService.saveTree(testPlanTree, new FileOutputStream("generated_test_plan.jmx"));

        System.out.println("JMX file successfully generated.");
    }
}

