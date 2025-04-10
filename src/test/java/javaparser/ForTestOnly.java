package javaparser;

public class ForTestOnly {
    public int num = 0;
    public String sql1 = "select";
    public StringBuilder sb = new StringBuilder();
    public StringBuilder sb2 = new StringBuilder("sample");

    public String assemble() {
        StringBuilder sb3 = new StringBuilder();
        int signal = 100;
        sb.append("select");
        String condition1 = "c1";
        String condition2 = "c2";
        String names = "Names";
        String a = "A";
        String b = "B";
        String w = condition1;
        sb3.append(condition2);
        condition2 += names;
        sb.append(sql1 + condition1 + "cww");
        String c = a + b + names + sb2.toString();
        if (condition1.equals("c12")) {
            sb.append(" * from 1");
        } else if (condition2.equals("w")) {
            sb.append(" * from 2");
        } else
            sb.append(" * from 3");
        if (condition1.equals("c120")) {
            sb.append(" * from 100");
        } else if (condition2.equals("w0"))
            sb.append(" * from 200");

        for (int i = 0; i < 10; i++) {
            sb2.append(i);
        }
        switch (signal) {
            case 1: {
                sb3.append("Monday");
                break;
            }
            case 2:
                sb3.append("Tuesday");
                break;
            default:
                sb3.append("Weekend");
        }

        String mm = sb + "fw" + a;
        return condition2 + sb;
    }

    public String assemble2() {
        StringBuilder sb3 = new StringBuilder();
        sb.append("select2");
        String condition1 = "c12";
        String condition2 = "c22";
        String names = "Names2";
        String a = "A2";
        String b = "B2";
        sb3.append(condition1);
        condition2 += names;
        if (condition1.equals("c12")) {
            sb.append(" * from 12");
        } else if (condition2.equals("w2")) {
            sb.append(" * from 22");
        } else
            sb.append(" * from 32");
        for (int i = 0; i < 10; i++) {
            sb2.append(i);
        }
        String mmmm = sb2.append("wf").toString();
        if (condition1.equals("c12")) {
            sb3.append(" * from s12");
        } else if (condition2.equals("w2")) {
            sb3.append(" * from s22");
        } else
            sb3.append(" * from s32");
        return sb2.toString();
    }
}
