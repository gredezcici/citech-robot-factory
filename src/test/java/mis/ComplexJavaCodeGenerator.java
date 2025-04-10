package mis;

import javassist.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
@Slf4j
public class ComplexJavaCodeGenerator {

    private static final String[] TYPES = {"int", "String", "boolean", "double", "float"};
    private static final String[] METHODS = {"calculateTotal", "processData", "generateReport", "sendEmail", "applyDiscount"};
    private static final String[] OPERATORS = {"+", "-", "*", "/", "%", "&&", "||"};
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        try {
            // Set up the JavaAssist ClassPool for code generation and manipulation
            ClassPool pool = ClassPool.getDefault();
            CtClass generatedClass = pool.makeClass("GeneratedBusinessLogic");

            // Generate a random method
            CtMethod method = generateRandomMethod(pool);
            generatedClass.addMethod(method);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CtMethod generateRandomMethod(ClassPool pool) throws NotFoundException, CannotCompileException {
        // Define the method signature
        String methodName = METHODS[RANDOM.nextInt(METHODS.length)];
        CtClass voidType = pool.get("void");
        CtMethod method = new CtMethod(voidType, methodName, null, pool.get("java.lang.Exception"));

        // Add business logic to the method
        StringBuilder body = new StringBuilder();

        // Generate variables
        for (int i = 0; i < 3; i++) {
            body.append(generateRandomVariable()).append("\n");
        }

        // Generate some random logic operations
        body.append(generateRandomLogic()).append("\n");

        // Add random method invocation
        body.append(generateRandomMethodInvocation()).append("\n");

        // Add conditional branching
        body.append(generateRandomCondition()).append("\n");

        // Complete the method body
        method.setBody(body.toString());

        return method;
    }

    public static String generateRandomVariable() {
        String type = TYPES[RANDOM.nextInt(TYPES.length)];
        String variableName = "var" + RANDOM.nextInt(1000);
        return type + " " + variableName + " = " + generateRandomValue(type) + ";";
    }

    public static String generateRandomValue(String type) {
        switch (type) {
            case "int":
                return String.valueOf(RANDOM.nextInt(100));
            case "String":
                return "\"SampleString" + RANDOM.nextInt(100) + "\"";
            case "boolean":
                return RANDOM.nextBoolean() ? "true" : "false";
            case "double":
                return String.valueOf(RANDOM.nextDouble() * 100);
            case "float":
                return String.valueOf(RANDOM.nextFloat() * 100);
            default:
                return "null";
        }
    }

    public static String generateRandomLogic() {
        String operand1 = "var" + RANDOM.nextInt(1000);
        String operand2 = "var" + RANDOM.nextInt(1000);
        String operator = OPERATORS[RANDOM.nextInt(OPERATORS.length)];
        return operand1 + " = " + operand1 + " " + operator + " " + operand2 + ";";
    }

    public static String generateRandomMethodInvocation() {
        String methodName = METHODS[RANDOM.nextInt(METHODS.length)];
        return methodName + "();";
    }

    public static String generateRandomCondition() {
        String conditionType = RANDOM.nextBoolean() ? "if" : "switch";
        if (conditionType.equals("if")) {
            return "if (var" + RANDOM.nextInt(1000) + " > 50) {\n" +
                    "            // Business logic\n" +
                    "        } else {\n" +
                    "            // Alternative logic\n" +
                    "        }";
        } else {
            return "switch (var" + RANDOM.nextInt(1000) + ") {\n" +
                    "            case 1:\n" +
                    "                // Case logic\n" +
                    "                break;\n" +
                    "            default:\n" +
                    "                // Default case\n" +
                    "        }";
        }
    }

}
