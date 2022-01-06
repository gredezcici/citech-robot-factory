package db;

/** @author chaochen */
public interface UpdateColumnInterface {
  boolean applyUpdate(Column prev);
}
