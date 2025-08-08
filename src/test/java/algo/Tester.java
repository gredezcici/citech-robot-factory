package algo;

import java.util.*;

/**
 * @author Chao Chen
 */
public class Tester {
    public static int[] findCorruptPair(int[] nums) {

        // Write your code here
        for (int i = 0; i < nums.length; i++) {
            if (nums[nums[i] - 1] != nums[i]) {
                int idx = nums[i] - 1;
                int temp = nums[i];
                nums[i] = nums[idx];
                nums[idx] = temp;
            }
        }
        int[] ans = new int[2];
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] - 1 != i) {
                ans[0] = nums[i];
                ans[1] = i + 1;
//                break;
            }
        }
        return ans;
    }

    public static void main(String[] args) {
        int[] a = {3, 1, 2, 5, 2};
        int[] b = findCorruptPair(a);
        System.out.println(Arrays.toString(b));
        int[] c = {0, -5, 1, 3, 5, 4};
        System.out.println(firstKMissingNumbers(c, 3));
        System.out.println(convert("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 5));
        int[] f = {3,3,3,1,2,1,1,2,3,3,4};
        System.out.println(totalFruit(f));
    }

    public static List<Integer> firstKMissingNumbers(int[] arr, int k) {
        List<Integer> ans = new ArrayList<>();
        // Your code will replace this placeholder return statement
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > 0 && arr[i] <= arr.length && arr[i] - 1 != i) {
                int idx = arr[i];
                int temp = arr[i];
                arr[i] = arr[idx];
                arr[idx] = temp;
            }

            System.out.println(Arrays.toString(arr));
        }

        for (int i = 0; i < arr.length || k > 0; i++) {
            if (i < arr.length) {
                ans.add(i);
                k--;
            } else if (k > 0) {

            }
        }


        return new ArrayList<>();
    }

    public static String convert(String s, int numRows) {
        if (numRows == 1) return s;
        int c = numRows + numRows - 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numRows; i++) {
            for (int j = i; j < s.length(); j += c) {
                sb.append(s.charAt(j));
                if (i == 0 || i == numRows - 1) continue;
                int n = 2 * (numRows - 1 - i) + j;
                if (n < s.length()) sb.append(s.charAt(n));
            }
        }
        return sb.toString();
    }

    public static void sortColors(int[] nums) {
        int l = 0;
        int r = nums.length - 1;
        while (l < nums.length && nums[l++] == 0) ;
        while (r >= 0 && nums[r--] == 2) ;
        for (int i = l; i <= r; i++) {
            while (i < r && nums[i] == 2) {
                swap(nums, i, r--);

            }
            while (i > l && nums[i] == 0) {
                swap(nums, i, l++);
            }
        }
    }

    private static void swap(int[] nums, int l, int r) {
        int t = nums[l];
        nums[l] = nums[r];
        nums[r] = t;
    }
    public static int totalFruit(int[] fruits) {
//[1,2,3,4,1,2,1,3,4]
//        slidingwindow
        int n = fruits.length;
        HashMap<Integer,Integer> seen = new HashMap();
        HashSet<Integer> types = new HashSet<>();
        int start = 0;
        int total = 0;
        int count = 0;
        for(int i =0;i<n;i++){
            count++;
            if(!seen.containsKey(fruits[i])&&seen.size()==2){
                while(seen.size()==2&&seen.containsKey(fruits[start])){
                   int rem = seen.get(fruits[start])-1;
                   if(rem==0)seen.remove(fruits[start]);
                   else seen.put(fruits[i],rem);
                   count--;
                   start++;
                }
            }
            total =Math.max(total,count);
            seen.put(fruits[i],seen.getOrDefault(fruits[i],0)+1);
        }
        return total;
    }
}
