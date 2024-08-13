package questions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Chao Chen
 */
public class SoldProductsAggregator {

    private final EURExchangeService exchangeService;

    SoldProductsAggregator(EURExchangeService EURExchangeService) {
        this.exchangeService = EURExchangeService;
    }

    SoldProductsAggregate aggregate(Stream<SoldProduct> products) {
        Pair pair = new Pair();
        List<SimpleSoldProduct> productList = new ArrayList<>();
        Stream<SoldProduct> productStream = Optional.ofNullable(products).orElse(Stream.empty());
        pair.products = productList;
        pair.total = BigDecimal.ZERO;
        EURExchangeService exchange = new EURExchangeService();
        productStream.forEach(p -> {
            BigDecimal price = p.getPrice();
            String currency = p.getCurrency();
            String name = p.getName();
            Optional<BigDecimal> euroExchangeRateOpt = exchange.rate(currency);
            if (euroExchangeRateOpt.isPresent()) {
                BigDecimal rate = euroExchangeRateOpt.get();
                if (rate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal priceInEuro = price.multiply(rate);
                    SimpleSoldProduct simpleSoldProduct = new SimpleSoldProduct(name, priceInEuro);
                    pair.products.add(simpleSoldProduct);
                    pair.total = pair.total.add(priceInEuro);

                }
            }
        });
        SoldProductsAggregate soldProductsAggregate = new SoldProductsAggregate(pair.products, pair.total);
        return soldProductsAggregate;

    }

    class Pair {
        List<SimpleSoldProduct> products;
        BigDecimal total;
    }


}
