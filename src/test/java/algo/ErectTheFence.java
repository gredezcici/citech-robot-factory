package algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chao Chen
 */
public class ErectTheFence {
    public static void main(String[] args) {
        ErectTheFence erectTheFence = new ErectTheFence();
        int[][] trees = {{1, 1}, {2, 2}, {2, 0}, {2, 4}, {3, 3}, {4, 2}};
        int[][] ans = erectTheFence.erectTheFence(trees);
        for (int i = 0; i < ans.length; i++)
            System.out.println(Arrays.toString(ans[i]));
    }

    public int[][] erectTheFence(int[][] trees) {
        ArrayList<int[]> list = new ArrayList<>();
        int p = 0;
        Arrays.sort(trees, (a, b) -> a[0] - b[0]);
        int[] start = trees[0];
//        for (int i = 0; i < trees.length; i++)
//            System.out.println(Arrays.toString(trees[i]));
        Arrays.sort(trees, (a, b) ->
                (a[1] - trees[0][1]) * (b[0] - trees[0][0]) - (b[1] - trees[0][1]) * (a[0] - trees[0][0])
        );

        for (int i = 0; i < trees.length; i++)
            System.out.println(Arrays.toString(trees[i]));

        list.add(trees[0]);
        for (int i = 1; i < trees.length; ) {
            System.out.println("****");
            for (int m = 0; m < list.size(); m++)
                System.out.print(Arrays.toString(list.get(m)));
            System.out.println("wwwwwww");
            Map[] maps = new HashMap[10];
            if (list.size() >= 2) {
                System.out.println("====" + p + " " + Arrays.toString(trees[i]));
                int[] v0 = list.get(p - 1);
                int[] v1 = list.get(p);
                int[] v2 = trees[i];
                int k = (v1[1] - v0[1]) * (v2[0] - v1[0]) - (v2[1] - v1[1]) * (v1[0] - v0[0]);
                int sign = (v1[0] - v0[0]) * (v2[0] - v1[0]) >= 0 ? 1 : -1;
                k *= sign;
                if (k < 0) {
                    list.remove(p);
                    p--;
                } else {
                    list.add(v2);
                    p++;
                    i++;
                }
            } else {

                list.add(trees[i]);
                p++;
                i++;
            }

        }
        int[][] ans = new int[list.size()][2];
        int k = 0;
        for (int[] v : list) {
            ans[k++] = v;
        }
        return ans;
    }
}
