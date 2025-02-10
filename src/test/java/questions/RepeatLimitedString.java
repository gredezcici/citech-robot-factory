package questions;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * @author Chao Chen
 */
public class RepeatLimitedString {

    @Test
    public void test() {
        String ans = repeatLimitedString("xyutfpopdynbadwtvmxiemmusevduloxwvpkjioizvanetecnuqbqqdtrwrkgt", 1);
        System.out.println(ans);
    }

    public String repeatLimitedString(String s, int repeatLimit) {
        int[] count = new int[26];
        for (char ch : s.toCharArray()) {
            count[ch - 'a']++;
        }
        System.out.println(Arrays.toString(count));
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);
        for (int i = 25; i >= 0; i--) {
            if (count[i] == 0) continue;
            pq.offer(new int[]{i, count[i]});
        }

        StringBuilder sb = new StringBuilder();
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            if (sb.length() > 0 && cur[0] == sb.charAt(sb.length() - 1) - 'a') {
                if (pq.isEmpty()) break;
                int[] prev = cur;
                cur = pq.poll();
                // System.out.println((char)('a'+prev[0])+"==="+(char)('a'+cur[0]));
                sb.append((char) ('a' + cur[0]));
                cur[1]--;
                pq.offer(prev);
                if (cur[1] > 0)
                    pq.offer(cur);
            } else {
                for (int i = 0; i < Math.min(repeatLimit, cur[1]); i++) {
                    // System.out.println("***"+(char)('a'+cur[0]));
                    sb.append((char) ('a' + cur[0]));
                }
                cur[1] -= repeatLimit;
                if (cur[1] > 0) {
                    pq.offer(cur);
                }
            }
        }
        return sb.toString();

    }
}
