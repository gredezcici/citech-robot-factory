package util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;


/** @author chaochen */
public class Calculator {

  private Calculator() {}

  public static double add(double first, double second) {
    BigDecimal num1 = BigDecimal.valueOf(first);
    BigDecimal num2 = BigDecimal.valueOf(second);
    BigDecimal sum = num1.add(num2).setScale(2, RoundingMode.HALF_DOWN);
    return sum.doubleValue();
  }

  static void fun() throws Exception {
    try {
      throw new Exception("demo");
    } catch (Exception e) {
      System.out.println("Caught inside fun().");
      throw e; // rethrowing the exception
    }
  }

  public static String findSubstring(String str, String pattern) {
    // TODO: Write your code here
    Map<Character, Integer> freqMap = new HashMap();
    for (char ch : pattern.toCharArray()) {
      freqMap.put(ch, freqMap.getOrDefault(ch, 0) + 1);
    }
    int min = Integer.MAX_VALUE;
    int matched = 0;
    int start = 0;
    int[] range = new int[2];
    for (int i = 0; i < str.length(); i++) {
      char right = str.charAt(i);

      if (freqMap.containsKey(right)) {
        freqMap.put(right, freqMap.get(right) - 1);
        if (freqMap.get(right) == 0) matched++;
      }
      while (matched == freqMap.size()) {
        if (min > i - start + 1) {
          range[0] = start;
          range[1] = i;
          min = i - start + 1;
          System.out.println(min + " " + range[0] + " " + range[1]);
        }
        char left = str.charAt(start++);
        if (freqMap.containsKey(left)) {
          if (freqMap.get(left) == 0) matched--;
          freqMap.put(left, freqMap.get(left) + 1);
        }
      }
    }
    return str.substring(range[0], range[1] + 1);
  }

  public static void main(String[] args) {
    int n =10;

  }
  public static int numberOf1Bits(int n){
    // write your code here
    int count=0;
    for(int i=0;i<32;i++){
      if(((n>>i)&1)==1)count++;
    }
    return count;
  }
}
