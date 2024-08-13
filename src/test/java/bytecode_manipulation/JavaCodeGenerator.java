package bytecode_manipulation;

import controller.Task;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.SignatureAttribute;

import java.util.Arrays;

/**
 * @author Chao Chen
 */

public class JavaCodeGenerator {
    public static void main(String[] args) throws Exception {

        // Create a new class
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass("com.example.MyClass");
        String signature = "Ljava/lang/Object;db.Column<T;>;";
        String signature2 = "<T:Ljava/lang/Object;>;Ljava/lang/Object;";
        SignatureAttribute attribute = new SignatureAttribute(cc.getClassFile().getConstPool(), signature);
        cc.setGenericSignature(signature2);
//        cc.getClassFile().addAttribute(attribute);
        // Add a private field to the class

        CtField field = new CtField(CtClass.intType, "myField", cc);
        field.setModifiers(Modifier.PRIVATE);
        cc.addField(field);


        // Add a public method to the class

        CtMethod method = new CtMethod(CtClass.voidType, "myMethod", new CtClass[]{}, cc);
        method.setModifiers(Modifier.PUBLIC);
        method.setBody("{ System.out.println(\"Hello World!\"); }");
        cc.addMethod(method);
        byte[] b = cc.toBytecode();

        // Write the class to a file
        cc.writeFile("./");
    }
}

