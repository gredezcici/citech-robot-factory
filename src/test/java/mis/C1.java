package mis;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Exchanger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chao Chen
 */
class GenericSample {
    public <T> void MyGenericMethod(T arg1) {
        return;
    }

    public <T, K> K MyGenericMethodWithTwoTypes(T arg1) {
        //Some operation
        return null;
    }
}

public class C1 {
    BigDecimal total;

    public BigDecimal getTotal() {
        return total;
    }

    public C1() {
        total = BigDecimal.ZERO;
    }

    public static void foo() {
        System.out.println("foo");
    }

    public static void main(String[] args) {

        int[][] graph = {{1, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 1, 0, 0, 0, 0, 0, 0, 1}, {0, 0, 1, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 1, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 1, 0, 1, 1, 1}, {0, 0, 0, 0, 0, 1, 0, 0, 1}, {0, 0, 0, 0, 1, 0, 1, 1, 0}, {0, 0, 0, 0, 1, 0, 1, 1, 0}, {0, 1, 0, 1, 1, 1, 0, 0, 1}};
        int[] initial = {8, 4, 2, 0};
        C1 ins = new C1();
        int ans = ins.minMalwareSpread(graph, initial);
        System.out.println(ans);
    }

    public static class InstanceHolder {
        public static C1 instance = new C1();
    }

    public static C1 getInstance() {
        return InstanceHolder.instance;
    }

    public class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode() {
        }

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    private int dfs(Map<Integer, List<Integer>> map, int prev, int cur, int seats) {
        int sum = 1;
        for (int next : map.getOrDefault(cur, new ArrayList<>())) {
            if (next == prev) continue;
            sum += dfs(map, cur, next, seats);
        }
        map.get(1).stream().mapToInt(Integer::intValue).toArray();

        return sum;
    }



    public int minMalwareSpread(int[][] graph, int[] initial) {
        int n = graph.length;
        int[] count = new int[n];
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            count[i] = 1;
        }
        boolean[] infected = new boolean[n];

        for (int mal : initial) infected[mal] = true;
        for (int i = 0; i < n; i++) {
            if (infected[i]) continue;
            for (int j = 0; j < n; j++) {
                if (!infected[j] && graph[i][j] == 1) {
                    union(parent,count, i, j);
                }
            }
        }
        System.out.println(Arrays.toString(parent));
        System.out.println(Arrays.toString(count));
//        Arrays.sort(initial);
//        int[] malwareCount = new int[n];
//        HashMap<Integer, Set<Integer>> map = new HashMap();
//        for (int i = 0; i < initial.length; i++) {
//            HashSet<Integer> set = new HashSet();
//            for (int j = 0; j < n; j++) {
//                if (!infected[j] && graph[i][j] == 1) {
//                    int root = find(parent, j);
//                    set.add(root);
//                    malwareCount[root]++;
//                }
//                map.put(initial[i], set);
//            }
//        }
        Set<Integer>[] component = new Set[n];
        int[] malcount= new int[n];
        for (int u : initial) {
            component[u] = new HashSet<>();
            for (int v = 0; v < n; v++) {
                if (!infected[v] && graph[u][v] == 1) component[u].add(find(parent, v));
            }
            for (int v : component[u]) malcount[v]++;
        }
        int max = 0;
        int ans = initial[0];
        for(int u: initial) {
            int save = 0;
            for(int v: component[u]) {
                if(malcount[v] == 1) save += count[v];
            }
            if(save > max || save == max && u < ans) {
                max = save;
                ans = u;
            }
        }

//        for (int i : initial) {
//            int size = 0;
//            for (int j : map.get(i)) {
//                int root = find(parent, i);
//                if (malwareCount[root] == 1) {
//                    size += count[root];
//                }
//            }
//            if (size > max) {
//                max = size;
//                ans = i;
//            }
//        }

        return ans;

    }

    public int find(int[] parent, int i) {
        while (i != parent[i]) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    public void union(int[] parent,int[] count, int i, int j) {
        int x = find(parent, i);
        int y = find(parent, j);
        if (x != y) {
            parent[y] = parent[x];
            count[x] += count[y];
        }
    }

}
