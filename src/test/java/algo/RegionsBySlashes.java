package algo;

/**
 * @author Chao Chen
 */
public class RegionsBySlashes {
    public static void main(String[] args) {
        RegionsBySlashes regionsBySlashes = new RegionsBySlashes();
        String[] grid = {"/\\", "\\/"};

        int t = regionsBySlashes.regionsBySlashes(grid);
        System.out.println(t);
    }

    public int regionsBySlashes(String[] grid) {
        int n = grid.length;
        int[] parents = new int[n * n * 4];
        for (int i = 0; i < n * n * 4; i++) {
            parents[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int index = 4 * (i * n + j);
                if (grid[i].charAt(j) == '\\') {
                    union(parents, index + 0, index + 1);
                    union(parents, index + 2, index + 3);
                } else if (grid[i].charAt(j) == '/') {
                    union(parents, index + 0, index + 3);
                    union(parents, index + 1, index + 2);
                } else {
                    union(parents, index + 0, index + 1);
                    union(parents, index + 2, index + 3);
                    union(parents, index + 0, index + 3);
                }
                if (i > 0) union(parents, index + 0, index - 4 * n + 2);
                if (j > 0) union(parents, index + 3, index - 4 + 1);
            }
        }
        int ans = 0;
        for (int i = 0; i < n * n * 4; i++) {
            if (parents[i] == i) ans++;

        }

        return ans;
    }

    private int find(int[] parents, int i) {
        while (i != parents[i]) {
            parents[i] = parents[parents[i]];
            i = parents[i];
        }
        return i;
    }

    private void union(int[] parents, int i, int j) {
        int r1 = find(parents, i);
        int r2 = find(parents, j);
        if (r1 != r2)
            parents[r1] = parents[r2];
    }
}
