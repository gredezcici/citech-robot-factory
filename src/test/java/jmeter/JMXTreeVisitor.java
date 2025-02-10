package jmeter;

import lombok.Data;
import org.apache.jmeter.protocol.jdbc.sampler.JDBCSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;

import java.util.ArrayList;
import java.util.List;

@Data
public class JMXTreeVisitor implements HashTreeTraverser {
    List<JDBCSampler> jdbcSamplers = new ArrayList<>();
    @Override
    public void addNode(Object node, HashTree subTree) {
        if (node instanceof TestElement) {
            TestElement testElement = (TestElement) node;
            System.out.println("Found Test Element: " + testElement.getName());
            System.out.println("Class: " + testElement.getClass().getName());
            System.out.println("Properties: " + testElement.getPropertyAsString("TestPlan.comments"));

            if(testElement instanceof JDBCSampler) {
                jdbcSamplers.add((JDBCSampler) testElement);
            }
        }
    }

    @Override
    public void subtractNode() {
        // Optional method, used when moving up the tree
    }

    @Override
    public void processPath() {
        // Optional method, used when visiting elements
    }
}
