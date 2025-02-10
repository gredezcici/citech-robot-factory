package jmeter;

import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.FileInputStream;
import java.io.File;
import java.nio.charset.Charset;

public class JMXAnalyzer {
    public static void main(String[] args) throws Exception {
        // Initialize JMeter
        JMeterUtils.loadJMeterProperties("/Users/chaochen/Downloads/apache-jmeter-5.6.3/bin/jmeter.properties");
        JMeterUtils.setJMeterHome("/Users/chaochen/Downloads/apache-jmeter-5.6.3/");
        // Load the JMX file

        JMeterUtils.initLocale();
        HashTree testPlanTree = SaveService.loadTree(new File("/Users/chaochen/Downloads/mytest.jmx"));

        // Analyze the JMX file
        analyzeJMX(testPlanTree);
    }

    public static void analyzeJMX(HashTree testPlanTree) {
        // Traverse through the JMeter Test Plan Tree and analyze
        testPlanTree.traverse(new JMXTreeVisitor());
    }
}
