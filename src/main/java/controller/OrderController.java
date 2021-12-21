package controller;



import entity.Order;
import exception.InvalidReqException;
import exception.UnProcessableCompException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import service.OrderProcessingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chaochen
 */

@RestController
public class OrderController {
    private OrderProcessingService orderProcessingService;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    public OrderController(OrderProcessingService srv) {
        orderProcessingService = srv;
    }

    @PostMapping(value = "/orders", headers = "Content-Type=application/json")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        Object components = payload.get("components");
        logger.info("request payload:{}", payload);
        try {
            Map<String, Object> map = new HashMap<>();
            if (!(components instanceof List<?>)) {
                logger.error("unsupported payload:{}", payload);
                throw new InvalidReqException("invalid request:" + payload.toString());
            }
            Order order = orderProcessingService.createOrder(components);
            map.put("order_id", order.getOrderId());
            map.put("total", order.getPrice());
            logger.info("order created:{}", map);
            return new ResponseEntity<>(map, HttpStatus.CREATED);
        } catch (InvalidReqException ex) {
            throw new InvalidReqException(ex.getMessage());
        } catch (UnProcessableCompException com){
            throw com;
        }
    }
}

