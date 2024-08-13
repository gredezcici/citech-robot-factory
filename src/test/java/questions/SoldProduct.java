package questions;

import lombok.Value;

import java.math.BigDecimal;

/**
 * @author Chao Chen
 */
@Value
public class SoldProduct {
    String name;
    BigDecimal price;
    String currency;
}
