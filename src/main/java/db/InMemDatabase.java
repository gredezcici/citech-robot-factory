package db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chaochen
 */

public class InMemDatabase {
    private String dbName;
    private ConcurrentHashMap<String, Row> rows = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(InMemDatabase.class);

    public InMemDatabase(String name) {
        this.dbName = name;
    }

    public String getDbName() {
        return dbName;
    }

    // it will not do anything if key exists
    public void insertNewRow(String key) {
        logger.debug("new row inserted {}", key);
        rows.computeIfAbsent(key, row -> new Row(key));
    }

    // add a clone column to row
    public void addColumnToRow(String rowKey, Column column) throws DBInternalException {
        logger.debug("add column info:{} to row {}", rowKey, column);
        if (!rows.containsKey(rowKey))
            return;
        rows.get(rowKey).addColumn(new Column<>(column.getName(), column.getVal()));
    }

    public Column getColumnFromRow(String rowKey, String columnName) {
        logger.debug("get column {} from row {}", columnName, rowKey);
        if (!rows.containsKey(rowKey))
            return null;
        return rows.get(rowKey).readColumn(columnName);
    }

    public List<Column> getMultiColumnsFromRow(String rowKey, List<String> names) {
        logger.debug("get multi columns:{} from row {}", names, rowKey);
        if (!rows.containsKey(rowKey))
            return new ArrayList<>();
        return rows.get(rowKey).readMultiColumns(names);
    }

    public List<String> getAllColumnNamesInRow(String rowKey) {
        logger.debug("get row names from row {}", rowKey);
        if (!rows.containsKey(rowKey))
            return new ArrayList<>();
        return rows.get(rowKey).getAllColumnNames();
    }

    public void updateMultiColumnsOfRow(String rowKey, Map<String, UpdateColumnInterface> operations) {
        logger.debug("update multi columns:{}, row {}", operations.keySet(), rowKey);
        if (!rows.containsKey(rowKey))
            return;
        rows.get(rowKey).updateMultiColumns(operations);
    }

    public boolean updateColumn(String rowKey, String columnName, UpdateColumnInterface updateOperation) throws DBInternalException {
        logger.debug("update single column:{}, row {}", rowKey, columnName);
        if (!rows.containsKey(rowKey))
            return false;
        return rows.get(rowKey).updateColumn(columnName, updateOperation);
    }
}
