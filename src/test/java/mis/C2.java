package mis;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;
import java.util.*;

/**
 * @author Chao Chen
 */
public class C2 extends C1 {
    public C2() {

    }

    InClass inClass;

    public InClass getInClass() {
        return inClass;
    }

    public void setInClass(InClass inClass) {
        this.inClass = inClass;
    }

    public void foo1() {
        foo();
    }

    public void hikari() throws SQLException {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.getConnection();
        PriorityQueue<Integer>[] pq = new PriorityQueue[10];

    }

    public static void main(String[] args) {
        int[] edges = {2, 2, 3, -1};
        int m = new Solution().closestMeetingNode(edges, 0, 1);
        System.out.println(m);
    }

    public static class InClass {
        public int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public InClass(int x, int y) {
            this.x = x;
            this.y = y;
        }

         static void w() {
        }

        private int x;
        private int y;
    }
}

class Solution {
    public static void main(String[] args) {
        C2 c = new C2();

    }

    public int closestMeetingNode(int[] edges, int node1, int node2) {
        Map<Integer, ArrayList<Integer>> map = new HashMap();
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] == -1) continue;
            map.putIfAbsent(i, new ArrayList());
            map.get(i).add(edges[i]);
        }
        int n = edges.length;
        int[][] pqs1 = getDistanceArr(map, node1, n);
        int[][] pqs2 = getDistanceArr(map, node2, n);
        print(pqs1);
        print(pqs2);

        for (int i = n - 1; i >= 0; i--) {
            for (int j = 0; j < n; j++) {
//                System.out.println(i+" "+j +"=== "+(n-1-i)+" "+j);
                if (pqs1[i][j] == 0) continue;
                System.out.println();
                for (int k = 0; k < n; k++)
                    if (pqs2[k][j] == 1) return j;
            }
        }
        return -1;
    }

    private void print(int[][] pq2) {
        for (int i = 0; i < pq2.length; i++) {
            System.out.println(Arrays.toString(pq2[i]));

        }
    }

    private int[][] getDistanceArr(Map<Integer, ArrayList<Integer>> map, int node, int len) {
        Queue<Integer> q = new LinkedList();
        q.offer(node);
        int dis = 0;
        int[][] pqs = new int[len][len];
        while (!q.isEmpty()) {
            // System.out.println(dis);
            // System.out.println("q "+q.toString());

            pqs[dis] = new int[len];
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int cur = q.poll();
                pqs[dis][cur] = 1;
                if (!map.containsKey(cur)) continue;
                for (int nxt : map.get(cur)) {
                    q.offer(nxt);
                }
            }

            // System.out.println(Arrays.toString(pqs[dis]));
            dis++;
        }
        return pqs;
    }
}
