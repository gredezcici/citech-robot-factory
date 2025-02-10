package algo;

import java.util.Arrays;

/**
 * @author chaochen
 */
public class GridGame {
    public long gridGame(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = Math.max(dp[i][j - 1], dp[i - 1][j]) + grid[i - 1][j - 1];
            }
        }
        for (int i = 0; i <= m; i++) {
            System.out.println(Arrays.toString(dp[i]));
        }

        int r = m;
        int c = n;
        while (true) {
            grid[r - 1][c - 1] = 0;
            if (r == 1 && c == 1) {
                break;
            }

            if (dp[r][c - 1] > dp[r - 1][c]) {
                c--;
            } else {
                r--;
            }
        }
        for (int[] ints : grid) {
            System.out.println(Arrays.toString(ints));
        }
        int[][] dp2 = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp2[i][j] = Math.max(dp2[i][j - 1], dp2[i - 1][j]) + grid[i - 1][j - 1];
            }
        }
        return dp2[m][n];
    }

    public static void main(String[] args) {
        int[][] grid = {{20, 3, 20, 17, 2, 12, 15, 17, 4, 15}, {20, 10, 13, 14, 15, 5, 2, 3, 14, 3}};
        new GridGame().gridGame(grid);
    }
}