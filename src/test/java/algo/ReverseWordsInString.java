package algo;

/**
 * @author Chao Chen
 */
public class ReverseWordsInString {
    public String reverseWords(String s) {
        boolean inword = false;
        int len = 0;
        char[] temp = new char[s.length()];
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                if (inword) {

                    temp[len++] = ' ';
                    inword = false;
                }
            } else {

                temp[len++] = s.charAt(i);
                inword = true;
            }
        }


        len = len == 0 || temp[len - 1] != ' ' ? len : len - 1;
        char[] arr = new char[len];
        int k = 0;
        int start = s.length() - 1;

        while (start >= 0) {
            while (start >= 0 && s.charAt(start) == ' ') {
                start--;
            }
            int right = start;
            if (right < 0) break;
            while (start >= 0 && s.charAt(start) != ' ') {
                start--;
            }
            int left = start + 1;
            System.out.println(s.substring(left, right + 1));
            while (k < len && left <= right) {
                arr[k++] = s.charAt(left++);
            }
            if (k < len) arr[k++] = ' ';

        }
        return new String(arr);
    }

    public static void main(String[] args) {
        ReverseWordsInString instance = new ReverseWordsInString();
        System.out.println(instance.reverseWords("  hello world  "));
    }
}
