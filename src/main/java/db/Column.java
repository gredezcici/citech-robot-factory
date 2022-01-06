package db;

/** @author chaochen */
public class Column<T> {
  private String name;
  private Class<?> clz;
  private T val;

  public Column(String name, T val) {
    this.name = name;
    this.val = val;
    clz = val.getClass();
  }

  public String getName() {
    return name;
  }

  public Class<?> getClz() {
    return clz;
  }

  public T getVal() {
    return val;
  }

  public boolean setVal(T val) {
    if (val.getClass() == clz) {
      this.val = val;
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return name + ":" + val;
  }
}
