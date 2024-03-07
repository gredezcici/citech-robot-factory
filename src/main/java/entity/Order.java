package entity;

import java.io.Serializable;
import java.util.List;

/** @author chaochen */
public class Order implements Serializable {
  private  static final long serialVersionUID = -6664912267008556466L;
  private String orderId;
  private double price;
  private List<String> components;
  private OrderStatus status;

  public Order(String orderId, double price, List<String> components) {
    this.orderId = orderId;
    this.price = price;
    this.components = components;
    status = OrderStatus.CREATED;
  }

  public  String getOrderId() {
    return orderId;
  }

  public double getPrice() {
    return price;
  }

  public List<String> getComponents() {
    return components;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public void setComponents(List<String> components) {
    this.components = components;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }
}
