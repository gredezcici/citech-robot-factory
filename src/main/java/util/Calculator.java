package util;


import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author chaochen
 */

public class Calculator {

    private Calculator() {
    }

    public static double add(double first, double second) {
        BigDecimal num1 = BigDecimal.valueOf(first);
        BigDecimal num2 = BigDecimal.valueOf(second);
        BigDecimal sum = num1.add(num2).setScale(2, RoundingMode.HALF_DOWN);
        return sum.doubleValue();
    }

}
