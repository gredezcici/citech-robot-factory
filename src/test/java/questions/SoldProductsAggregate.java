package questions;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Chao Chen
 */
@AllArgsConstructor
@Value
class SoldProductsAggregate {
    List<SimpleSoldProduct> products;
    BigDecimal total;
}
