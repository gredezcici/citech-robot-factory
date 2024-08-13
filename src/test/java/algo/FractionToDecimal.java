package algo;

import java.util.HashMap;

/**
 * @author Chao Chen
 */
public class FractionToDecimal {
    public static void main(String[] args) {
        FractionToDecimal instance = new FractionToDecimal();
        int row = 3, col = 3;


    }

    public static String fractionToDecimal(int numerator, int denominator) {
        // Write your code here
        if (numerator == 0) return "0";
        long nL = (long) numerator;
        long dL = (long) denominator;
        int sign = 1;
        if (nL < 0) {
            sign *= -1;
            nL *= -1;
        }
        if (dL < 0) {
            sign *= -1;
            dL *= -1;
        }
        StringBuilder sb = new StringBuilder();
        if (sign < 0) sb.append('-');
        sb.append(nL / dL);
        long reminder = nL % dL;
        if (reminder == 0L) return sb.toString();
        sb.append('.');
        HashMap<Long, Integer> map = new HashMap<>();
        while (reminder != 0) {
            if (map.containsKey(reminder)) break;
            map.put(reminder, sb.length());
            reminder = reminder * 10;
            sb.append(reminder / dL);
            reminder = reminder % dL;
        }
        if (reminder != 0) {
            String repeat = sb.substring(map.get(reminder));
            sb.delete(map.get(reminder), sb.length());
            sb.append('(');
            sb.append(repeat);
            sb.append(')');
        }
        return sb.toString();
    }
}
