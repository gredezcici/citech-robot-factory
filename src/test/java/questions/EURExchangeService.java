package questions;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author Chao Chen
 */
public class EURExchangeService implements ExchangeService{
    @Override
    public Optional<BigDecimal> rate(String currency) {
        return Optional.empty();
    }
}
