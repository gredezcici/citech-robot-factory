package bytecode_manipulation;

import javassist.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Chao Chen
 */
public class JavassistTest {
    @Test
    public void generateClass() throws NotFoundException, CannotCompileException, IOException {
        CtClass ctClass = ClassPool.getDefault().makeClass("com.example.MyClass2");
        ctClass.setGenericSignature("<T:Ljava/lang/Object;R:Ljava/lang/Object;>Ljava/lang/Object;");

        ctClass.writeFile("./");
        CtClass mc = ClassPool.getDefault().get("Marker.MClass");

        mc.getClassFile();

    }
    public static void main(String[] args) {
        try {
            // Create a new class pool
            ClassPool pool = ClassPool.getDefault();

            // Create a new class
            CtClass dynamicClass = pool.makeClass("DynamicClass");

            // Add a method to the class
            CtMethod method = CtNewMethod.make("public void sayHello() { System.out.println(\"Hello from DynamicClass!\"); }", dynamicClass);
            dynamicClass.addMethod(method);

            // Write the class file to disk (optional)
//            dynamicClass.writeFile();

            // Load and instantiate the class
            Class<?> clazz = dynamicClass.toClass();
            Object obj = clazz.newInstance();

            // Call the method
            clazz.getMethod("sayHello").invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}



