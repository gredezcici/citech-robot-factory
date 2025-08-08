package sootup;


import sootup.core.IdentifierFactory;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import java.util.*;

public class StringValueAnalysisRunnerV2 {
    public static void main(String[] a) {
        // --- 1. Set up View ---
        JavaClassPathAnalysisInputLocation inputLocation =
                new JavaClassPathAnalysisInputLocation("/Users/chenchao/IdeaProjects/citech-robot-factory/target/test-classes");
        JavaView view = new JavaView(inputLocation);

        // --- 2. Identify class & method ---
        IdentifierFactory idFactory = view.getIdentifierFactory();
        ClassType targetClassType = idFactory.getClassType("sootup.sample.TargetClass");
        MethodSignature methodSig = idFactory.getMethodSignature(
                targetClassType, "stringBuilderManipulation", "void", Collections.emptyList());

        Optional<JavaSootClass> clsOpt = view.getClass(targetClassType);
        if (!clsOpt.isPresent()) {
            System.err.println("Target class not found!");
            return;
        }
        JavaSootClass cls = clsOpt.get();
        System.out.println(cls.getMethods());
        Optional<JavaSootMethod> mthOpt = cls.getMethod(methodSig.getSubSignature());
        if (!mthOpt.isPresent()) {
            System.err.println("Target method not found!");
            return;
        }
        SootMethod sootMethod = mthOpt.get();

        // --- 3. Perform Intra-procedural Analysis ---
        // (make sure body is active)
        Body body = sootMethod.getBody();
        Map<Local, StringBuilder> sbValues = new HashMap<>();

        for (Stmt stmt : body.getStmts()) {

            System.out.println("Analyzing: " + stmt);

            // 3a. Track `new StringBuilder()`
            if (stmt instanceof JAssignStmt) {
                JAssignStmt assign = (JAssignStmt) stmt;
                Value right = assign.getRightOp();
                if (right instanceof JNewExpr) {
                    JNewExpr ne = (JNewExpr) right;
                    if (ne.getType().toString().equals("java.lang.StringBuilder")) {
                        Value left = assign.getLeftOp();
                        if (left instanceof Local) {
                            sbValues.put((Local) left, new java.lang.StringBuilder());
                            System.out.println("  -> Found new StringBuilder: " + left);
                        }
                    }
                }
            }
            // 3b. Track `StringBuilder.append(...)`
            else if (stmt instanceof JInvokeStmt) {
                JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) stmt.asInvokableStmt().getInvokeExpr().get();

                if (invokeExpr.getMethodSignature().getName().equals("append")
                        && invokeExpr.getMethodSignature().getDeclClassType().getClassName().equals("java.lang.StringBuilder")
                ) {

                    Value base = invokeExpr.getBase();
                    if (sbValues.containsKey(base)) {
                        List<Immediate> args = invokeExpr.getArgs();
                        if (!args.isEmpty() && args.get(0) instanceof StringConstant) {
                            StringConstant sc = (StringConstant) args.get(0);
                            sbValues.get(base).append(sc.getValue());
                            System.out.println("  -> Appended to " + base + ": " + sc.getValue());
                            System.out.println("  -> Current value: " + sbValues.get(base));
                        }
                    }
                }
            }
        }
    }
}
