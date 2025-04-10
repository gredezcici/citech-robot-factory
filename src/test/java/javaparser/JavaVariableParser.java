package javaparser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import groovy.util.logging.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class JavaVariableParser {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JavaVariableParser.class);
    private String sourceCode;


    // Class to store values of the traced variables
    public static class Node {
        String name;
        HashMap<String, List<String>> variables = new HashMap<>();
        Node parent;
        List<Node> children = new ArrayList<>();
    }

    public void printVariables(Node node, int level) {
        if (node == null) {
            return;
        }
        String indent = "  ".repeat(level);
        node.variables.forEach((key, value) -> {
            System.out.println(indent + "Variable: " + key + " Values: " + value);
        });
        for (Node child : node.children) {
            printVariables(child, level + 1);
        }
    }

    public List<String> traceVariable(Path sourceCode) {
        // Parse the source code
        List<String> values = new ArrayList<>();
        CompilationUnit cu = null;
        VariableVisitor variableVisitor = new VariableVisitor();
        try {
            cu = StaticJavaParser.parse(sourceCode);
            cu.accept(variableVisitor, null);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (ParseProblemException e) {
            String content = null;
            try {
                content = Files.readString(sourceCode, Charset.forName("GBK"));
                cu = StaticJavaParser.parse(content);
                cu.accept(variableVisitor, null);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
        printVariables(variableVisitor.getRoot(), 0);
        return values;
    }

    // Visitor class to visit variables
    private static class VariableVisitor extends VoidVisitorAdapter<Void> {

        private Node currentNode = new Node();

        public Node getRoot() {
            return currentNode;
        }

        @Override
        public void visit(BlockStmt n, Void arg) {
            createNewNode(n.toString());
            super.visit(n, arg);
            currentNode = currentNode.parent;
        }

        private void createNewNode(String name) {
            Node newNode = new Node();
            newNode.name = name;
            newNode.parent = currentNode;
            currentNode.children.add(newNode);
            currentNode = newNode;
        }

        @Override
        public void visit(IfStmt n, Void arg) {
            if (!(n.getThenStmt() instanceof BlockStmt)) {
                createNewNode(n.toString());
                n.getThenStmt().accept(this, arg);
                currentNode = currentNode.parent;
            } else {
                n.getThenStmt().accept(this, arg);
            }
            if (n.getElseStmt().isPresent()) {
                if (!(n.getElseStmt().get() instanceof IfStmt)) {
                    createNewNode(n.toString());
                    n.getElseStmt().get().accept(this, arg);
                    currentNode = currentNode.parent;
                } else
                    n.getElseStmt().get().accept(this, arg);
            }
            mergeValuesToParent();

        }

        @Override
        public void visit(SwitchEntry n, Void arg) {
//            createNewNode(n.toString());
            n.getStatements().forEach(stmt -> {
                if (stmt instanceof BlockStmt)
                    stmt.accept(this, arg);
                else {
                    createNewNode(stmt.toString());
                    stmt.accept(this, arg);
                    currentNode = currentNode.parent;
                }
            });
//            currentNode = currentNode.parent;
        }

        public void mergeValuesToParent() {
            for (Node child : new ArrayList<>(currentNode.children)) {
                child.variables.forEach((key, value) -> {
                    if (currentNode.variables.containsKey(key)) {
                        List<String> merged = new ArrayList<>(currentNode.variables.get(key));
                        merged.addAll(value);
                        currentNode.variables.put(key, merged);
                    } else {
                        String undefinedKey = "undefined";
                        currentNode.variables.putIfAbsent(undefinedKey, new ArrayList<>());
                        currentNode.variables.get(undefinedKey).addAll(value);
                    }

                });
                child.parent = null;
                currentNode.children.remove(child);
            }
        }

        @Override
        public void visit(VariableDeclarator n, Void arg) {
            super.visit(n, arg);
            if (n.getType().asString().equals("String") || n.getType().asString().equals("StringBuilder")
                    || n.getType().asString().equals("StringBuffer")) {
                String variableName = n.getNameAsString();
                List<String> values = new ArrayList<>();

                if (n.getInitializer().isPresent()) {
                    Expression initExpr = n.getInitializer().get();
                    if (initExpr instanceof ObjectCreationExpr) {
                        ObjectCreationExpr creationExpr = (ObjectCreationExpr) initExpr;
                        if (creationExpr.getArguments().size() == 1) {
                            values.add(creationExpr.getArgument(0).asStringLiteralExpr().asString());
                        } else
                            values.add("");
                    } else if (initExpr instanceof BinaryExpr) {
                        values = collectBinaryExpr(initExpr.asBinaryExpr());
                    } else if (initExpr instanceof NameExpr) {
                        values = getClonedVariableValuesClone(initExpr.asNameExpr().getNameAsString());
                    } else if (initExpr instanceof MethodCallExpr) {
                        values = processMethodCallExprCommon(initExpr.asMethodCallExpr());
                    } else {
                        values.add(initExpr.asStringLiteralExpr().getValue());
                    }
                } else {
                    values.add("");
                }
                currentNode.variables.putIfAbsent(variableName, values);

            }
        }

        @Override
        public void visit(AssignExpr n, Void arg) {
            if (n.getOperator() == AssignExpr.Operator.ASSIGN || n.getOperator() == AssignExpr.Operator.PLUS) {
                String variableName = n.getTarget().toString();
                Expression value = n.getValue();
                if (value instanceof BinaryExpr) {
                    BinaryExpr binaryExpr = (BinaryExpr) value;
                    if (binaryExpr.getOperator() == BinaryExpr.Operator.PLUS) {
                        List<String> parts = collectBinaryExpr(binaryExpr);
                    }
                } else if (n.getOperator() == AssignExpr.Operator.PLUS) {
                    List<String> parts = new ArrayList<>();
                    parts.add(variableName);
                    parts.add(value.toString());
                }
            }
        }

        public List<String> getVariableValues(String variableName) {
            return searchVariableValues(currentNode, variableName);
        }

        public List<String> getClonedVariableValuesClone(String variableName) {
            return new ArrayList<>(searchVariableValues(currentNode, variableName));
        }

        private List<String> searchVariableValues(Node node, String variableName) {
            while (node != null) {
                if (node.variables.containsKey(variableName)) {
                    return node.variables.get(variableName);
                }
                node = node.parent;
            }
            return Collections.emptyList();
        }


        private List<String> collectBinaryExpr(BinaryExpr expr) {
            List<String> leftParts = processExpression(expr.getLeft());
            List<String> rightParts = processExpression(expr.getRight());
            List<String> values = new ArrayList<>();
            if (leftParts.isEmpty() || rightParts.isEmpty()) {
                return leftParts.isEmpty() ? rightParts : leftParts;
            }
            for (String l : leftParts) {
                for (String r : rightParts) {
                    values.add(l + r);
                }
            }
            return values;
        }

        private List<String> processExpression(Expression expr) {
            List<String> vs;
            if (expr instanceof BinaryExpr) {
                vs = collectBinaryExpr(expr.asBinaryExpr());
            } else if (expr instanceof StringLiteralExpr) {
                vs = new ArrayList<>();
                vs.add(expr.asStringLiteralExpr().asString());
            } else if (expr instanceof NameExpr) {
                vs = getVariableValues(expr.asNameExpr().getNameAsString());
            } else if (expr instanceof MethodCallExpr) {
                vs = processMethodCallExprCommon(expr.asMethodCallExpr());
            } else {
                vs = new ArrayList<>();
            }

            return vs;
        }

        private List<String> processMethodCallExprCommon(MethodCallExpr methodCallExpr) {

            if (methodCallExpr.getScope().isEmpty()
                    && (methodCallExpr.getScope().get() instanceof MethodCallExpr || methodCallExpr.getScope().get() instanceof NameExpr))
                return new ArrayList<>();
            if (methodCallExpr.getNameAsString().equals("toString")) {
                if (methodCallExpr.getScope().get() instanceof NameExpr) {
                    String variableName = methodCallExpr.getScope().get().asNameExpr().getNameAsString();
                    return getVariableValues(variableName);
                } else {
                    return processExpression(methodCallExpr.getScope().get().asMethodCallExpr());
                }
            } else if (methodCallExpr.getNameAsString().equals("append")) {
                List<String> arr = new ArrayList<>();
                while (methodCallExpr != null && methodCallExpr.getNameAsString().equals("append")) {
                    Expression argExpr = methodCallExpr.getArgument(0);
                    List<String> vs;
                    if (argExpr.isMethodCallExpr()) {
                        vs = processMethodCallExprCommon(argExpr.asMethodCallExpr());
                    } else {
                        vs = processExpression(argExpr);
                    }
                    if (arr.isEmpty()) {
                        arr = new ArrayList<>(vs);
                    } else {
                        arr = mergeTwoLists(vs, arr);
                    }
                    if (methodCallExpr.getScope().isPresent() && methodCallExpr.getScope().get() instanceof MethodCallExpr) {
                        methodCallExpr = (MethodCallExpr) methodCallExpr.getScope().get();
                    } else {
                        methodCallExpr = null;
                    }
                }
                return arr;
            }
            return new ArrayList<>();
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            MethodCallExpr expr = n;
            if (!expr.getScope().isPresent()) {
                log.error("Method call without scope: " + n);
                return;
            }
            while (!(expr.getScope().get() instanceof NameExpr)) {
                if (!(expr.getScope().get() instanceof MethodCallExpr)) {
                    log.error("Unsupported scope type: " + expr.getScope().get());
                    return;
                }
                expr = (MethodCallExpr) expr.getScope().get();
            }
            List<String> newValues = processMethodCallExprCommon(n);
            currentNode.variables.put(expr.getScope().get().asNameExpr().getNameAsString(), newValues);
        }

        private List<String> mergeTwoLists(List<String> list1, List<String> list2) {
            List<String> merged = new ArrayList<>();
            for (String l1 : list1) {
                for (String l2 : list2) {
                    merged.add(l1 + l2);
                }
            }
            return merged;
        }

        @Override
        public void visit(ReturnStmt n, Void arg) {
            Expression expr = n.getExpression().orElse(null);
            if (expr != null) {
                List<String> returnValues = processExpression(expr);
                currentNode.variables.putIfAbsent("return", returnValues);
            }
        }
    }
}
