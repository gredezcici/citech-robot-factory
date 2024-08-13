package questions;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Chao Chen
 */
@Value
class SoldProductsAggregate {
    List<SimpleSoldProduct> products;
    BigDecimal total;
}
