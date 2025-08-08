package mis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Chao Chen
 */

public class Tools {
    public static void main(String[] args) {
//        HashSet<Integer> set1 = new HashSet<>(Arrays.asList(1,2,3,4,5,6));
//        HashSet<Integer> set2 = new HashSet<>(Arrays.asList(1,2,3,4,5,6,9,8));
//        set1.addAll(set2);
//        System.out.println(set1.toString());
//        PriorityQueue<Long> pq = new PriorityQueue<>();
//        System.out.println(pq.remove());
//        System.out.println(pq.poll());
        int mask = 0;
        String w = "lc";
        for (int i = 0; i < 2; i++)
            mask = ((mask << (w.charAt(i) - 'a')) | 1);

        System.out.println(Integer.toBinaryString(mask));
        System.out.println(maxDiff(555));

    }

    public static int maxDiff(int num) {
        String str = Integer.toString(num);
        StringBuilder maxStr = new StringBuilder(str);
        StringBuilder minStr = new StringBuilder(str);

        // Get max number by replacing first non-'9' digit
        char maxDigit = ' ';
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != '9') {
                maxDigit = str.charAt(i);
                break;
            }
        }

        if (maxDigit != ' ') {
            for (int i = 0; i < maxStr.length(); i++) {
                if (maxStr.charAt(i) == maxDigit) {
                    maxStr.setCharAt(i, '9');
                }
            }
        }

        // Get min number
        char minDigit = str.charAt(0);
        char replace = '1';

        if (minDigit == '1') {
            for (int i = 1; i < str.length(); i++) {
                if (str.charAt(i) != '0' && str.charAt(i) != '1') {
                    minDigit = str.charAt(i);
                    replace = '0';
                    break;
                }
            }
        }

        for (int i = 0; i < minStr.length(); i++) {
            if (minStr.charAt(i) == minDigit) {
                minStr.setCharAt(i, replace);
            }
        }

        int maxVal = Integer.parseInt(maxStr.toString());
        int minVal = Integer.parseInt(minStr.toString());

        return maxVal - minVal;
    }
}


