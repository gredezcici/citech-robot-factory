package mis;

import org.apache.hc.core5.concurrent.CompletedFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CME {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
//        try {
//            nums.stream().parallel().forEach(n -> {
//                nums.add(1);
////                if (n == 2) {
////                    // Structural change to the same list during traversal → CME
////                    nums.add(99);
////                }
//            });
//        } catch (ConcurrentModificationException e) {
//            System.out.println("Caught CME: " + e);
//        }
        List<Integer> source = new ArrayList<>();
        List<Integer> source2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            source.add(i);
            source2.add(1000 + i);
        }
        CompletableFuture<String> completedFuture = CompletableFuture.supplyAsync(() -> {
            source.stream().parallel().forEach(i -> System.out.println(i));
            return "source finished";
        }).thenApply(msg -> {
            source2.stream().parallel().forEach(i -> System.out.println(i));
            return "source2 finished";
        });

        completedFuture.get();
        System.out.println(10%10);

    }
}
