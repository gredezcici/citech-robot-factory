package algo;

/**
 * @author Chao Chen
 */
public class MinimumWindowSequence {



    public static void minWindow(String str1, String str2) {

        // Save the size of str1 and str2
        int sizeStr1 = str1.length();
        int sizeStr2 = str2.length();
        // Initialize length to a very large number (infinity)
        float length = Float.POSITIVE_INFINITY;
        // Initialize pointers to zero and the min_subsequence to an empty string
        int indexS1 = 0;
        int indexS2 = 0;
        int flag = 0;
        int start = 0,
                end = 0;
        String minSubsequence = "";
        // Process every character of str1
        while (indexS1 < sizeStr1) {
            // Check if the character pointed by index_s1 in str1
            // is the same as the character pointed by index_s2 in
            System.out.println("\tIteration no. " + indexS1 + " starts");



            if (str1.charAt(indexS1) == str2.charAt(indexS2)) {
                // If the pointed character is the same
                // in both strings increment index_s2

                System.out.println("\t" + str1.charAt(indexS1) + " is same in both str1 and str2 at index " + indexS1 + " and " + indexS2 + " respectively");
                indexS2 += 1;
                // Check if index_s2 has reached the end of str2
                if (indexS2 == sizeStr2) {
                    // At this point the str1 contains all characters of str2
                    start = indexS1;
                    end = indexS1 + 1;
                    // Initialize start to the index where all characters of
                    // str2 were present in str1
                    indexS2 -= 1;
                    // Decrement pointer index_s2 and start a reverse loop
                    while (indexS2 >= 0) {
                        // Decrement pointer index_s2 until all characters of
                        // str2 are found in str1
                        if (str1.charAt(start) == str2.charAt(indexS2)) {
                            indexS2 -= 1;
                        }

                        // Decrement start pointer everytime to find the
                        // starting point of the required subsequence
                        start -= 1;
                    }
                    start += 1;
                    // Check if length of sub sequence
                    // by start and end pointers is less than current min length
                    if ((end - start) < length) {
                        // Update length if current sub sequence is
                        length = end - start;
                        // Update minimum subsequence string
                        // to this new shorter string
                        for (int i = start; i < end; i++) {
                            minSubsequence += String.valueOf(str1.charAt(i));
                        }
                        System.out.println("\tThe current minsubsequence is " + minSubsequence + " and its length is " + (int) length);
                        minSubsequence = "";

                    }
                    indexS2 = 0;
                }
            }
            System.out.println("\tIteration no. " + indexS1 + " ends");
            // Increment pointer index_s1 to check next character in str1
            indexS1 += 1;
        }

    }

    public static void main(String[] args) {
        // Driver code
        String[] str1 = {
                "abcdebdebde",
                "kfgzreqsdqsnodwmxzkzxwqegkndaa",
                "qwewerrty",
                "aaabbcbq",
                "zxcvnhss",
                "alpha",
                "beta",
                "asd",
                "abcd"
        };
        String[] str2 = {
                "bde",
                "kzed",
                "werty",
                "abc",
                "css",
                "la",
                "ab",
                "as",
                "pp"
        };
        for (int i = 0; i < str1.length; i++) {
            System.out.println(i + 1 + ".\tInput String: " + "(" + str1[i] + ", " + str2[i] + ")");
            minWindow(str1[i], str2[i]);
            System.out.println();

        }
    }
}
