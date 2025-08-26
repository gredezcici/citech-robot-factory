package mis;

import java.util.ArrayList;
import java.util.List;

public class JavaTool {

    private static final long MOD = 1_000_000_007L;

    public static int[] productQueries(int n, int[][] queries) {
        // 1) gather exponents of set bits
        List<Integer> exps = new ArrayList<>();
        for (int bit = 0; n > 0; bit++, n >>= 1) {
            if ((n & 1) == 1) exps.add(bit);
        }

        // 2) prefix sums of exponents
        int m = exps.size();
        long[] pref = new long[m + 1];
        for (int i = 0; i < m; i++) pref[i + 1] = pref[i] + exps.get(i);

        // 3) answer queries as 2^(sum exponents) mod MOD
        int[] ans = new int[queries.length];
        for (int i = 0; i < queries.length; i++) {
            int L = queries[i][0], R = queries[i][1];
            long s = pref[R + 1] - pref[L];
            ans[i] = (int) modPow2(s);
        }
        return ans;
    }

    private static long modPow2(long exp) {
        long base = 2, res = 1;
        long e = exp;
        while (e > 0) {
            if ((e & 1) == 1) res = (res * base) % MOD;
            base = (base * base) % MOD;
            e >>= 1;
        }
        return res;
    }

    public static void main(String[] args) {
        int n = 15;
        int[][] queries = {{0, 1}, {2, 2}, {0, 3}};
        productQueries(n,queries);
    }
}
