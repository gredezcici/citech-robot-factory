package repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import db.Column;
import db.InMemDatabase;
import db.UpdateColumnInterface;
import entity.RobotPart;
import exception.UnProcessableCompException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** @author chaochen */
@Component
public class InventoryRepository {

  private static final String PRICE = "price";
  private static final String CODE = "code";
  private static final String PART = "part";
  private static final String TYPE = "type";
  private static final String AVAILABLE = "available";
  private static final Logger logger = LoggerFactory.getLogger(InventoryRepository.class);
  // Cache
  private final ConcurrentHashMap<String, String> description = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> types = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>();

  private static final String DB_NAME = "Robot_Part";
  private final InMemDatabase robotPartInventory = new InMemDatabase(DB_NAME);

  private final ReentrantLock transactionLock = new ReentrantLock(true);

  public InventoryRepository() throws IOException {
    loadStorage();
  }

  public String getDescByCode(String code) {
    return description.getOrDefault(code, null);
  }

  public String getTypeByCode(String code) {
    return types.getOrDefault(code, null);
  }

  public double getPriceByCode(String code) {
    return prices.get(code);
  }

  public String getTypeFromDB(String code) {
    Optional<Column> columnOptional = robotPartInventory.getColumnFromRow(code, TYPE);
    if (columnOptional.isPresent()) return (String) columnOptional.get().getVal();
    return "";
  }

  public String getPartByCodeFromDB(String code) {
    return (String) robotPartInventory.getColumnFromRow(code, PART).get().getVal();
  }

  public int getAvailableFromDB(String code) {
    return (Integer) robotPartInventory.getColumnFromRow(code, AVAILABLE).get().getVal();
  }

  /*
   * col.setVal(cur) is no need to be synchronized,the reference of column cannot be accessed from outside
   *  */
  @SuppressWarnings("unchecked")
  public boolean reduceSinglePartInventoryAvail(String code, int num) {
    return robotPartInventory.updateColumn(
        code,
        AVAILABLE,
        col -> {
          int prevAvail = (int) col.getVal();
          int absNum = Math.abs(num);
          if (prevAvail - absNum >= 0) {
            int cur = prevAvail - absNum;
            col.setVal(cur);
            return true;
          } else {
            return false;
          }
        });
  }

  @SuppressWarnings("unchecked")
  public void increaseSinglePartInventoryAvail(String code, int num) {
    robotPartInventory.updateColumn(
        code,
        AVAILABLE,
        col -> {
          int prevAvail = (int) col.getVal();
          int absNum = Math.abs(num);
          int cur = prevAvail + absNum;
          col.setVal(cur);
          return true;
        });
  }

  public void reduceInventoryAvail(Map<String, Integer> codes) {
    List<String> records = new ArrayList<>();
    try {
      transactionLock.lock();
      for (Map.Entry<String, Integer> entry : codes.entrySet()) {
        boolean res = reduceSinglePartInventoryAvail(entry.getKey(), entry.getValue());
        if (res) {
          records.add(entry.getKey());
        } else {
          throw new UnProcessableCompException("part: " + entry.getKey() + " is out of stock");
        }
      }
    } catch (UnProcessableCompException ex) {
      logger.error("update Inventory Error:{}", ex.getMessage());
      rollbackAvail(records, codes);
      throw ex;
    } finally {
      transactionLock.unlock();
    }
  }

  public void insertPartIntoDB(RobotPart part) {
    robotPartInventory.insertNewRow(part.getCode());
    robotPartInventory.addColumnToRow(part.getCode(), new Column<String>(CODE, part.getCode()));
    robotPartInventory.addColumnToRow(part.getCode(), new Column<String>(TYPE, part.getType()));
    robotPartInventory.addColumnToRow(part.getCode(), new Column<String>(PART, part.getPart()));
    robotPartInventory.addColumnToRow(
        part.getCode(), new Column<Integer>(AVAILABLE, part.getAvailable()));
    robotPartInventory.addColumnToRow(
        part.getCode(), new Column<Double>(PRICE, part.getPrice().doubleValue()));
  }

  public void updatePartExceptAvail(RobotPart part) {
    Map<String, UpdateColumnInterface> operations = new HashMap<>();
    operations.put(TYPE, prev -> prev.setVal(part.getType()));
    operations.put(PART, prev -> prev.setVal(part.getPart()));
    operations.put(PRICE, prev -> prev.setVal(part.getPrice()));
    robotPartInventory.updateMultiColumnsOfRow(part.getCode(), operations);
    refreshCache(part.getCode());
  }

  private void refreshCache(String code) {
    description.put(code, (String) robotPartInventory.getColumnFromRow(code, PART).get().getVal());
    types.put(code, (String) robotPartInventory.getColumnFromRow(code, TYPE).get().getVal());
    prices.put(code, (Double) robotPartInventory.getColumnFromRow(code, PRICE).get().getVal());
  }

  private void rollbackAvail(List<String> records, Map<String, Integer> codes) {
    logger.info("try to roll back available deduction");
    for (String code : records) {
      increaseSinglePartInventoryAvail(code, codes.get(code));
    }
  }

  private void loadStorage() throws IOException {
    Resource resource = new ClassPathResource("stock.json");
    InputStream inputStream = resource.getInputStream();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode stock = objectMapper.readTree(FileCopyUtils.copyToByteArray(inputStream)).at("/stock");
    if (stock.isArray()) {
      for (JsonNode part : stock) {
        String code = part.at("/code").asText();
        String type = part.at("/type").asText();
        String desc = part.at("/part").asText();
        int avail = part.at("/available").asInt(0);
        double price = part.at("/price").asDouble(0.0);
        logger.info(
            "try to add new row into db, code: {}, type: {}, desc: {}, available: {}, price: {}",
            code,
            type,
            desc,
            avail,
            price);
        RobotPart robotPart = new RobotPart(code, type, desc, price, avail);
        insertPartIntoDB(robotPart);
        types.put(code, type);
        prices.put(code, price);
        description.put(code, desc);
      }
    }
  }
}
