package algo;

import java.util.Arrays;

/**
 * @author Chao Chen
 */
public class LatestDayToCross {
    public static void main(String[] args) {
        LatestDayToCross instance = new LatestDayToCross();
        int row = 3, col = 3;
//        int[][] cells = {{1, 1}, {2, 1}, {1, 2}, {2, 2}};
        int[][] cells = {{1, 2}, {2, 1}, {3, 3}, {2, 2}, {1, 1}, {1, 3}, {2, 3}, {3, 2}, {3, 1}};
        int ans = instance.latestDayToCross(row, col, cells);
        System.out.println(ans);

    }

    public int latestDayToCross(int row, int col, int[][] cells) {
        int[][] dir = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        int[] parents = new int[row * col + 2];
        for (int i = 0; i < row * col + 2; i++) {
            parents[i] = i;
        }
        int[][] stones = new int[row][col];
        for (int[] s : stones) {
            Arrays.fill(s, 1);
        }
        for (int i = cells.length - 1; i >= 0; i--) {
            int x = cells[i][0];
            int y = cells[i][1];
            stones[x - 1][y - 1] = 0;
            System.out.println(x + " " + y);
            if (x == row) {
                int root1 = find(parents, row * col + 1);
                int root2 = find(parents, (x - 1) * col + y);
                parents[root1] = parents[root2];
            } else if (x == 1) {
                int root1 = find(parents, 0);

                int root2 = find(parents, (x - 1) * col + y);
                parents[root1] = parents[root2];
            }
            for (int j = 0; j < 4; j++) {
                int x_n = x + dir[j][0];
                int y_n = y + dir[j][1];
//                System.out.println(x_n + " " + y_n);
                if (x_n < 1 || x_n > row || y_n > col || y_n < 1 || stones[x_n - 1][y_n - 1] == 1) continue;
                int root1 = find(parents, (x_n - 1) * col + y_n);
                int root2 = find(parents, (x - 1) * col + y);
                parents[root2] = parents[root1];

            }
//            System.out.println(Arrays.toString(parents));
            if (find(parents, 0) == find(parents, row * col + 1)) return i;

        }
        return -1;

    }

    private int find(int[] parents, int i) {
        while (i != parents[i]) {
            parents[i] = parents[parents[i]];
            i = parents[i];
        }
        return i;
    }
}
