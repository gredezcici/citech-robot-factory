package mis;

import java.util.*;

/**
 * @author Chao Chen
 */
public class SampleAlgo {
    public static void main(String[] args) {
//        int[] arr = {2, 4, 3, 3, 2, 6, 8, 9, 10, 0, 1, 2};
//        int[] arr = {1, 5, 5, 2, 6, 5, 7};

//        int[] arr = {0, 1, 0, 0, 0};
//        int distance = maxDistance(arr);
//        System.out.println(distance);
        SampleAlgo instance = new SampleAlgo();
//        String[][] routes = {{"JFK","SFO"},{"JFK","ATL"},{"SFO","ATL"},{"ATL","JFK"},{"ATL","SFO"}};
        String[][] routes = {{"JFK", "KUL"}, {"JFK", "NRT"}, {"NRT", "JFK"}};
        instance.findItinerary(routes);
        int x = instance.largestVariance("lripaa");
        System.out.println(x);
    }

    public int largestVariance(String s) {
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
        }

        int res = 0;
        for (int i = 0; i < 26; i++) {
            if (count[i] == 0) continue;

            for (int j = 0; j < 26; j++) {
                if ((char) (i + 'a') == 'a' && (char) (j + 'a') == 'p') {
                    System.out.println(1);
                }
                if (count[j] == 0) continue;
                char a = (char) (i + 'a');
                char b = (char) (j + 'a');
                if (a == b) continue;
                int countA = 0;
                int countB = 0;
                for (int k = 0; k < s.length(); k++) {
                    if (a == s.charAt(k)) countA++;
                    if (b == s.charAt(k)) countB++;
                    if (countA > 0 && countB > 0) {
                        System.out.println(a + " " + b + " " + k + " " + countA + " " + countB);
                        res = Math.max(res, countA - countB);
                    }
                    if (countB > 0 && countA > 0 && countA - countB < 0) {
                        countA = 0;
                        countB = 0;
                    }
                }
            }
        }
        return res;
    }

    private static int maxDistance(int[] arr) {
        int n = arr.length;
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];
        for (int i = 1; i < n; i++) {
            if (arr[i] <= arr[i - 1]) {
                leftMax[i] = leftMax[i - 1] + 1;
            } else {
                leftMax[i] = 0;
            }
        }
//        System.out.println(Arrays.toString(leftMax));
        for (int i = n - 2; i >= 0; i--) {
            if (arr[i] <= arr[i + 1]) {
                rightMax[i] = rightMax[i + 1] + 1;
            } else {
                rightMax[i] = 0;
            }
        }
//        System.out.println(Arrays.toString(rightMax));
        int ans = 0;
        for (int i = 0; i < n; i++) {
            ans = Math.max(ans, leftMax[i] + rightMax[i] + 1);
        }
        return ans;
    }

    private HashMap<String, List<String>> adjList = new HashMap<>();
    private LinkedList<String> route = new LinkedList<>();
    private int numTickets = 0;
    private int numTicketsUsed = 0;

    public List<String> findItinerary(String[][] tickets) {
        if (tickets == null || tickets.length == 0) return route;
        // build graph
        numTickets = tickets.length;
        for (int i = 0; i < tickets.length; ++i) {
            if (!adjList.containsKey(tickets[i][0])) {
                // create a new list
                List<String> list = new ArrayList<>();
                list.add(tickets[i][1]);
                adjList.put(tickets[i][0], list);
            } else {
                // add to existing list
                adjList.get(tickets[i][0]).add(tickets[i][1]);
            }
        }
        // sort vertices in the adjacency list so they appear in lexical order
        for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
            Collections.sort(entry.getValue());
        }

        // start DFS
        route.add("JFK");
        dfsRoute("JFK");
        return route;
    }

    private void dfsRoute(String v) {
        // base case: vertex v is not in adjacency list
        // v is not a starting point in any itinerary, or we would have stored it
        // thus we have reached end point in our DFS
        if (!adjList.containsKey(v)) return;
        List<String> list = adjList.get(v);
        for (int i = 0; i < list.size(); ++i) {
            String neighbor = list.get(i);
            // remove ticket(route) from graph
            list.remove(i);
            route.add(neighbor);
            numTicketsUsed++;
            dfsRoute(neighbor);
            // we only return when we have used all tickets
            if (numTickets == numTicketsUsed) return;
            System.out.println(route.toString());
            route.removeLast();
            numTicketsUsed--;
            list.add(neighbor);

        }
    }

}
