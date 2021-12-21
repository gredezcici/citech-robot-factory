package acceptance;


import entity.Order;
import exception.UnProcessableCompException;
import org.citech.citechrobotfactory.CitechRobotFactoryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import service.OrderProcessingService;

import java.util.Arrays;
import java.util.List;

import static entity.OrderStatus.CREATED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * @author chaochen
 */

@SpringBootTest(
        classes = {CitechRobotFactoryApplication.class}
)
public class OrderProcessingSrvTest {
    @Autowired
    OrderProcessingService orderProcessingService;

    @Test
    void should_not_allow_invalid_components() {
        List<String> components = Arrays.asList(new String[]{"B", "A", "G", "I"});
        assertThatThrownBy(() -> orderProcessingService.createOrder(components))
                .isInstanceOf(UnProcessableCompException.class).hasMessageContaining("invalid components");
    }

    @Test
    void should_find_order_in_db_after_created() {
        List<String> components = Arrays.asList(new String[]{"F", "A", "D", "I"});
        Order order = orderProcessingService.createOrder(components);
        assertThat(order.getStatus()).isEqualTo(CREATED);
        Order returnedOrder = orderProcessingService.serarchOrder(order.getOrderId());
        assertThat(order.getPrice()).isEqualTo(returnedOrder.getPrice());
    }
}
