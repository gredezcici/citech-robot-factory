package javaparser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class JavaVariableParser {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JavaVariableParser.class);
    private String sourceCode;

    // Constructor

    // Method to parse the source code
    public void parse() {
        // Parse the source code
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);

        // Visit and print the variables
    }

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
                    currentNode.variables.merge(key, value, (v1, v2) -> {
                        List<String> merged = new ArrayList<>(v1);
                        merged.addAll(v2);
                        return merged;
                    });
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
            super.visit(n, arg);
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

        @Override
        public void visit(BinaryExpr n, Void arg) {
            super.visit(n, arg);
            if (n.getOperator() == BinaryExpr.Operator.PLUS) {
                System.out.println("Binary expression: " + n.getLeft() + " + " + n.getRight());
            } else if (n.getOperator() == BinaryExpr.Operator.MINUS) {
                System.out.println("Binary expression: " + n.getLeft() + " - " + n.getRight());
            } else if (n.getOperator() == BinaryExpr.Operator.MULTIPLY) {
                System.out.println("Binary expression: " + n.getLeft() + " * " + n.getRight());
            } else if (n.getOperator() == BinaryExpr.Operator.DIVIDE) {
                System.out.println("Binary expression: " + n.getLeft() + " / " + n.getRight());
            }
        }

        @Deprecated
        private List<String> collectBinaryExprParts(BinaryExpr expr) {
            List<String> leftParts;
            List<String> rightParts;
            if (expr.getLeft() instanceof BinaryExpr) {
                leftParts = collectBinaryExprParts((BinaryExpr) expr.getLeft());
            } else if (expr.getLeft() instanceof StringLiteralExpr) {
                leftParts = new ArrayList<>();
                leftParts.add(expr.getLeft().toString());
            } else if (expr.getLeft() instanceof NameExpr) {
                leftParts = getVariableValues(expr.getLeft().asNameExpr().getNameAsString());
            } else
                leftParts = new ArrayList<>();

            if (expr.getRight() instanceof BinaryExpr) {
                rightParts = collectBinaryExprParts((BinaryExpr) expr.getRight());
            } else if (expr.getRight() instanceof StringLiteralExpr) {
                rightParts = new ArrayList<>();
                rightParts.add(expr.getRight().toString());
            } else if (expr.getRight() instanceof NameExpr) {
                rightParts = getVariableValues(expr.getRight().asNameExpr().getNameAsString());
            } else
                rightParts = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (String l : leftParts) {
                for (String r : rightParts) {
                    values.add(l + r);
                }
            }
            return values;
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
                MethodCallExpr n = expr.asMethodCallExpr();
                if (n.getNameAsString().equals("toString") && n.getScope().isPresent()
                        && n.getScope().get() instanceof NameExpr) {
                    NameExpr scope = (NameExpr) n.getScope().get();
                    vs = getVariableValues(scope.getName().asString());
                } else
                    vs = new ArrayList<>();
            } else {
                vs = new ArrayList<>();
            }

            return vs;
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            if (n.getNameAsString().equals("append") && n.getScope().isPresent()
                    && n.getScope().get() instanceof NameExpr) {
                NameExpr scope = (NameExpr) n.getScope().get();
                if (n.getArguments().size() > 1) {
                    log.error("Method call with more than one argument: " + n);
                    return;
                }
                List<String> oldValues = getVariableValues(scope.getName().asString());
                List<String> newValues;
                Expression expr = n.getArgument(0);
                if (expr.isStringLiteralExpr()) {
                    String appendValue = expr.asStringLiteralExpr().asString();
                    newValues = new ArrayList<>();
                    oldValues.forEach(v -> newValues.add(v + appendValue));
                } else if (expr.isBinaryExpr()) {
                    List<String> values = collectBinaryExpr(expr.asBinaryExpr());
                    newValues = mergeTwoLists(oldValues, values);
                } else if (expr.isNameExpr()) {
                    newValues = mergeTwoLists(oldValues, getVariableValues(expr.asNameExpr().getNameAsString()));
                } else {
                    log.error("Unsupported method call: " + n);
                    newValues = new ArrayList<>();
                }
                currentNode.variables.put(scope.getNameAsString(), newValues);
            }
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
            super.visit(n, arg);
            Expression expr = n.getExpression().orElse(null);
            if (expr != null) {
                List<String> returnValues = processExpression(expr);
                currentNode.variables.putIfAbsent("return", returnValues);
            }
        }

    }
}
