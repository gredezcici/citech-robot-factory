package algo;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Chao Chen
 */
public class SlidingWindowMaximum {
    public static void main(String[] args) {
        String S = "abcdebdde", T = "bde";
        HashMap<Character, Integer> map = new HashMap<>();
        for (char ch : T.toCharArray()) {
            map.put(ch, map.getOrDefault(ch, 0) + 1);
        }
        int start = 0;
        int len = Integer.MAX_VALUE;
        int size = map.size();
        int begin = 0;
        int end = 0;
        for (int i = 0; i < S.length(); i++) {
            char cur = S.charAt(i);
            if (map.containsKey(cur) && map.get(cur) > 0) {
                map.put(cur, map.get(cur) - 1);
                if (map.get(cur) == 0) size--;
            }
            while (size == 0) {
                char windowStart = S.charAt(start);
                if (map.containsKey(windowStart)) {
                    map.put(windowStart, map.get(windowStart) + 1);
                    if (map.get(windowStart) > 0) size++;
                }
                if (i - start + 1 < len) {
                    begin = start;
                    end = i;
                    len = i - start + 1;
                    System.out.println(begin + "  " + end + " " + len);
                }
                start++;
            }
        }
        System.out.println(S.substring(begin, end + 1));
    }
}
