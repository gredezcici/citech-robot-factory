package questions;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author Chao Chen
 */
public interface ExchangeService {
    Optional<BigDecimal> rate(String currency);
}
