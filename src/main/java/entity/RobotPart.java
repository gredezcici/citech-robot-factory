package entity;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** @author chaochen */
public class RobotPart implements Serializable {
  private static final long serialVersionUID = 7712911755465521220L;
  private Double price;
  private String code;
  private String part;
  private String type;
  private final AtomicInteger available = new AtomicInteger(0);

  public void store(int n) {
    available.accumulateAndGet(n, (stock, delta) -> stock + delta);
  }

  public int getAvailable() {
    return available.get();
  }

  public boolean remove(int num) {
    AtomicBoolean sufficient = new AtomicBoolean(true);
    available.accumulateAndGet(
        num,
        (stock, required) -> {
          if (stock >= required) {
            return stock - required;
          } else {
            sufficient.set(false);
            return stock;
          }
        });
    return sufficient.get();
  }

  public RobotPart(String code, String type, String part, Double price, int available) {
    this.price = price;
    this.code = code;
    this.part = part;
    this.type = type;
    store(Math.abs(available));
  }

  public String getType() {
    return type;
  }

  public Double getPrice() {
    return price;
  }

  public String getCode() {
    return code;
  }

  public String getPart() {
    return part;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public void setPart(String part) {
    this.part = part;
  }
}
