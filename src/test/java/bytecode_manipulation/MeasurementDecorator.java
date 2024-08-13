package bytecode_manipulation;

import javassist.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Chao Chen
 */
public final class MeasurementDecorator {
    private static final String MEASURE_START_OUTPUT = "start = System.currentTimeMillis();";
    private static final String MEASURE_END_OUTPUT = "end = System.currentTimeMillis();";
    private static final Consumer<CtMethod> CONSUMER = new MethodConsumer();
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();

    private MeasurementDecorator() {
        // prevents init
    }

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
                method.insertAfter("com.waitingforcode.StatsHolder.notifyStats(\""+className+"\", \""+methodName+"\", start, end);");
            } catch (CannotCompileException e) {
                throw new RuntimeException("An error occurred on decorating method "+method.getName(), e);
            }
        }
    }
}
