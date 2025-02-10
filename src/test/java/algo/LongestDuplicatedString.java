package algo;

import java.util.Arrays;

/**
 * @author chaochen
 */
public class LongestDuplicatedString {
    public static int[] suffixArray(String s) {
        int n = s.length();
        Integer[] suffixArray = new Integer[n];

        // Create suffixes and their corresponding indices
        for (int i = 0; i < n; i++) {
            suffixArray[i] = i;
        }

        // Sort the suffixes based on lexicographical order
        Arrays.sort(suffixArray, (a, b) -> s.substring(a).compareTo(s.substring(b)));

        // Convert to primitive int array and return
        return Arrays.stream(suffixArray).mapToInt(Integer::intValue).toArray();
    }

    // Compute LCP array from the suffix array
    public static int[] computeLCP(String s, int[] suffixArray) {
        int n = s.length();
        int[] lcp = new int[n - 1];
        int[] rank = new int[n];

        // Calculate the rank of each suffix
        for (int i = 0; i < n; i++) {
            rank[suffixArray[i]] = i;
        }

        // Calculate the LCP array
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = suffixArray[rank[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) {
                    h++;
                }
                lcp[rank[i] - 1] = h;
                if (h > 0) {
                    h--;
                }
            }
        }
        return lcp;
    }

    // Find the longest repeated substring using the LCP array
    public static String longestDupSubstring(String s) {

        int[] suffixArray = suffixArray(s);
        int[] lcp = computeLCP(s, suffixArray);

        // Find the maximum LCP value and its corresponding index
        int maxLCP = 0;
        int index = 0;
        for (int i = 0; i < lcp.length; i++) {
            if (lcp[i] > maxLCP) {
                maxLCP = lcp[i];
                index = i;
            }
        }

        // If there's no repeated substring
        if (maxLCP == 0) {
            return "";
        }

        // Return the longest repeated substring
        return s.substring(suffixArray[index], suffixArray[index] + maxLCP);
    }

    public static void main(String[] args) {
        String s = "banana";
        String result = longestDupSubstring(s);
        System.out.println(result);
    }
}
