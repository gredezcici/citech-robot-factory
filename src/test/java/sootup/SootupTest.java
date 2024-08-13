package sootup;

import org.junit.jupiter.api.Test;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.signatures.MethodSignature;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chao Chen
 */
public class SootupTest {
    @Test
    public void testA() {
        AnalysisInputLocation inputLocation =
                new JavaClassPathAnalysisInputLocation("/Users/chaochen/IdeaProjects/citech-robot-factory/target/test-classes/sootup/");

        JavaView view = new JavaView(inputLocation);

        JavaClassType classType = view.getIdentifierFactory().getClassType("HelloWorld");

        MethodSignature methodSignature =
                view.getIdentifierFactory()
                        .getMethodSignature(classType, "main", "void", Collections.singletonList("java.lang.String[]"));

        JavaSootClass sootClass = view.getClass(classType).get();

        JavaSootMethod sootMethod = sootClass.getMethod(methodSignature.getSubSignature()).get();

        sootMethod.getBody().getStmts();
    }
    @Test
    public void testB(){

    }
}
