package algo;

import java.util.Deque;
import java.util.LinkedList;

public class FenwickTree {
    int[] tree;
    int n;

    public FenwickTree(int[] array) {
        n = array.length;
        tree = new int[n + 1];
    }

    public void update(int i, int delta) {
        i++;
        while (i <= n) {
            tree[i] += delta;
            i += (i & -i);
        }
    }

    public int query(int i) {
        i++;
        int sum = 0;
        while (i > 0) {
            sum += tree[i];
            i -= (i & -i);
        }
        return sum;
    }

    public static int largestRectangleArea(int[] heights) {

        Deque<Integer> dq = new LinkedList();
        int n = heights.length;
        int[] heightsNew = new int[n + 2];
        for (int i = 0; i < n; i++) {
            heightsNew[i + 1] = heights[i];
        }
        int ans = 0;
        dq.offer(0);
        for (int i = 1; i < heightsNew.length; i++) {
            while (!dq.isEmpty() && heightsNew[dq.peekLast()] > heightsNew[i]) {
                System.out.println(dq.toString() + " " + ans + " " + i);
                int area = heightsNew[dq.pollLast()] * (i - dq.peekLast() - 1);

                ans = Math.max(ans, area);
                dq.pollLast();
            }
            dq.offerLast(i);
        }
        return ans;
    }

    public static void main(String[] args) {
        int[] h = {2, 1, 5, 6, 2, 3};
        largestRectangleArea(h);
    }
}
