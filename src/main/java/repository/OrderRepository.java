package repository;

import db.Column;
import db.InMemDatabase;
import entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chaochen
 */

@Component
public class OrderRepository {
    private static final String ORDER_ID = "orderId";
    private static final String PRICE = "price";
    private static final String COMPONENTS = "components";
    private static final String STATUS = "status";
    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);
    private static final String DB_NAME = "Robot_Part";
    private final InMemDatabase orderDB = new InMemDatabase(DB_NAME);

    public void upsertOrder(Order order) {
        if (order.getOrderId().equals("new")) {
            String uniqueID = UUID.randomUUID().toString();
            order.setOrderId(uniqueID);
        }
        orderDB.insertNewRow(order.getOrderId());
        orderDB.addColumnToRow(order.getOrderId(), new Column<>(ORDER_ID, order.getOrderId()));
        orderDB.addColumnToRow(order.getOrderId(), new Column<>(PRICE, order.getPrice()));
        orderDB.addColumnToRow(order.getOrderId(), new Column<>(COMPONENTS, order.getComponents().toString()));
        orderDB.addColumnToRow(order.getOrderId(), new Column<>(STATUS, order.getStatus().name()));
        orders.put(order.getOrderId(), order);
        logger.info("order {} stored", order.getOrderId());
    }

    public Order searchOrder(String id) {
        return orders.getOrDefault(id, null);
    }
}
