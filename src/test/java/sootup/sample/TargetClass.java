package sootup.sample;

public class TargetClass {
    public void voidReturnMethod() {

        StringBuilder sb = new StringBuilder();
        sb.append("public class VisitorCommon {\n");
        StringBuilder sb2 = new StringBuilder("select");
        if (sb2.indexOf("select") == 0) {
            sb.append("    public void visitSelect() {\n");
            sb.append("        // Implement select logic here\n");
            sb.append("    }\n");
        } else {
            sb.append("    public void visitOther() {\n");
            sb.append("        // Implement other logic here\n");
            sb.append("    }\n");
        }
        String str = "select";
        str += sb.toString();
    }

    public String stringReturnValueMethod() {

        StringBuilder sb = new StringBuilder();
        sb.append("public class VisitorCommon {\n");
        StringBuilder sb2 = new StringBuilder("select");
        if (sb2.indexOf("select") == 0) {
            sb.append("    public void visitSelect() {\n");
            sb.append("        // Implement select logic here\n");
            sb.append("    }\n");
        } else {
            sb.append("    public void visitOther() {\n");
            sb.append("        // Implement other logic here\n");
            sb.append("    }\n");
        }
        String str = "select";
        str += sb.toString();
        return str;
    }
}
