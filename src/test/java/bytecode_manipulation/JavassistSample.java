package bytecode_manipulation;

import javassist.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Chao Chen
 */
public class JavassistSample {
    @Test
    public void testJavassist() {

    }

    private static final String MEASURE_START_OUTPUT = "start = System.currentTimeMillis();";
    private static final String MEASURE_END_OUTPUT = "end = System.currentTimeMillis();";
    private static final Consumer<CtMethod> CONSUMER = new MethodConsumer();
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();


    public static CtClass decorateClass(String className) throws NotFoundException, CannotCompileException, IOException, IOException {
        CtClass classToChange = CLASS_POOL.get(className);
        Stream.of(classToChange.getDeclaredMethods()).forEach(CONSUMER);
        classToChange.writeFile();
        classToChange.detach();
        return classToChange;
    }

    private static final class MethodConsumer implements Consumer<CtMethod> {

        @Override
        public void accept(CtMethod method) {
            try {
                // a must-to-do, otherwise start and end variables will remain unknown
                method.addLocalVariable("start", CtClass.longType);
                method.addLocalVariable("end", CtClass.longType);

                String methodName = method.getName();
                String className = method.getDeclaringClass().getName();

                // decorate the class at the begin and at the end with the call to static notifyStats() method
                // also defines the values for previously declared variables
                method.insertBefore(MEASURE_START_OUTPUT);
                method.insertAfter(MEASURE_END_OUTPUT);

                method.insertAfter("com.waitingforcode.StatsHolder.notifyStats(\"" + className + "\", \"" + methodName + "\", start, end);");
            } catch (CannotCompileException e) {
                throw new RuntimeException("An error occurred on decorating method " + method.getName(), e);
            }
        }
    }

    public static class TestData {
        private int i = 0;
        private String myString = "abc";
        private long value = -1;
    }

    // test Javassist
    public static void main(String[] args) {
        try {

            ClassPool cp = ClassPool.getDefault();
            CtClass clazz = cp.get("bytecode_manipulation.JavassistSample$TestData");

            for (CtField field : clazz.getDeclaredFields()) {
                String camelCaseField = field.getName().substring(0, 1).toUpperCase()
                        + field.getName().substring(1);

                // We don't need to mess with implementation here. CtnewMethod has a
                // commodity method to implement a getter directly
                CtMethod fieldGetter = CtNewMethod.getter("get" + camelCaseField, field);
                clazz.addMethod(fieldGetter);

                // Just for the sake of an example, we'll define the setter by actually
                // providing the implementation, not using the commodity method offered
                // by CtNewMethod
//                CtMethod fieldSetter = CtNewMethod.make(
//                        "public void set" + camelCaseField + " \n" +
//                                "    (" + field.getType().getName() + " param) { \n" +
//                                "    this." + field.getName() + " = param; \n" +
//                                "}",
//                        clazz);
                CtMethod fieldSetter = CtNewMethod.setter("set" + camelCaseField, field);
                clazz.addMethod(fieldSetter);
            }

            // Save class and make it available
            cp.toClass(clazz, null, Thread.currentThread().getContextClassLoader(), null);

            // Now instantiate a new TestData
            TestData td = new TestData();

            // Get the value of the field 'myString' using the newly defined getter
            Method getter = td.getClass().getDeclaredMethod("getMyString");
            System.out.println(getter.invoke(td));

            // Change the value of field 'myString' using newly defined setter
            Method setter = td.getClass().getDeclaredMethod("setMyString", String.class);
            setter.invoke(td, "xyz");

            // Get the value again
            System.out.println(getter.invoke(td));

        } catch (NotFoundException | CannotCompileException | NoSuchMethodException
                 | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
