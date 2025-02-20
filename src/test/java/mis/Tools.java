package mis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Chao Chen
 */

public class Tools {
    public static void main(String[] args) {
        HashSet<Integer> set1 = new HashSet<>(Arrays.asList(1,2,3,4,5,6));
        HashSet<Integer> set2 = new HashSet<>(Arrays.asList(1,2,3,4,5,6,9,8));
        set1.addAll(set2);
        System.out.println(set1.toString());
        PriorityQueue<Long> pq = new PriorityQueue<>();
        System.out.println(pq.remove());
        System.out.println(pq.poll());


    }
}
