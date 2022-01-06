package service;

import entity.Order;
import exception.UnProcessableCompException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.InventoryRepository;
import repository.OrderRepository;
import util.Calculator;

import java.util.*;

/** @author chaochen */
@Service
public class OrderProcessingService {

  private final InventoryRepository inventoryRepository;

  private final OrderRepository orderRepository;

  @Autowired
  public OrderProcessingService(InventoryRepository inventoryRepo, OrderRepository orderRepo) {
    inventoryRepository = inventoryRepo;
    orderRepository = orderRepo;
  }

  public Order serarchOrder(String orderId) {
    return orderRepository.searchOrder(orderId);
  }

  @SuppressWarnings("unchecked")
  public Order createOrder(Object components) throws UnProcessableCompException {
    if (Optional.ofNullable(components).isEmpty()
        || !(components instanceof List<?>)
        || !validateCompTypes((List<String>) components)) {

      throw new UnProcessableCompException(
          "invalid components:"
              + (Optional.ofNullable(components).isPresent() ? components.toString() : null));
    }
    Map<String, Integer> parts = new HashMap<>();
    for (String code : (List<String>) components) {
      parts.put(code, 1);
    }
    placeOrder(parts);
    double total = calTotalPrice((List<String>) components);
    List<String> desc = getPartsList((List<String>) components);
    Order order = new Order("new", total, desc);
    orderRepository.upsertOrder(order);
    return order;
  }

  private double calTotalPrice(List<String> codes) {
    double sum = 0;
    for (String code : codes) {
      sum = Calculator.add(sum, inventoryRepository.getPriceByCode(code));
    }
    return sum;
  }

  private List<String> getPartsList(List<String> codes) {
    List<String> partsDesc = new ArrayList<>();
    for (String code : codes) {
      String desc = inventoryRepository.getDescByCode(code);
      if (Optional.ofNullable(desc).isPresent()) {
        partsDesc.add(code + ":" + desc);
      }
    }
    return partsDesc;
  }

  private boolean validateCompTypes(List<String> codes) {
    Set<String> types = new HashSet<>();
    for (String code : codes) {
      String type = inventoryRepository.getTypeByCode(code);
      if (Optional.ofNullable(type).isEmpty() || !types.add(type)) return false;
    }
    return types.size() == 4;
  }

  private void placeOrder(Map<String, Integer> codes) throws UnProcessableCompException {
    inventoryRepository.reduceInventoryAvail(codes);
  }
}
