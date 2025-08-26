package mis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

public class CME {
    public static void main(String[] args) {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        try {
            nums.stream().parallel().forEach(n -> {
                nums.add(1);
//                if (n == 2) {
//                    // Structural change to the same list during traversal → CME
//                    nums.add(99);
//                }
            });
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught CME: " + e);
        }
    }
}
