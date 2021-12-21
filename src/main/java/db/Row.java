package db;


import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author chaochen
 */

public class Row {
    private String rowKey;
    private Map<String, Column> columns;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);


    protected Row(String id) {
        this.rowKey = id;
        columns = new HashMap<>();
    }

    protected String getRowKey() {
        return rowKey;
    }

    //column is not accessible from outside, this method guarantees only one thread can changed column at a time
    protected boolean updateColumn(String columnName, UpdateColumnInterface updateOperation) {
        try {
            lock.writeLock().lock();
            return updateSingleColumnWithoutLock(columnName, updateOperation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // not guarantee all columns get updated as expected
    protected void updateMultiColumns(Map<String, UpdateColumnInterface> operations) {
        try {
            lock.writeLock().lock();
            for (String columnName : operations.keySet()) {
                UpdateColumnInterface updateOperation = operations.get(columnName);
                updateSingleColumnWithoutLock(columnName, updateOperation);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // updateSingleColumnWithoutLock can only be used inside write-lock block
    private boolean updateSingleColumnWithoutLock(String columnName, UpdateColumnInterface updateOperation) {
        Column prev = columns.get(columnName);
        if (Optional.ofNullable(prev).isEmpty())
            return false;
        return updateOperation.applyUpdate(prev);
    }

    protected void addColumn(Column column) throws DBInternalException {
        try {
            lock.writeLock().lock();
            if (columns.containsKey(column.getName())) {
                throw new DBInternalException("duplicate column name found:" + column.getName());
            }
            columns.put(column.getName(), column);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected Column readColumn(String columnName) {
        try {
            lock.readLock().lock();
            Column column = readSingleColumnWithoutLock(columnName);
            if (Optional.ofNullable(column).isPresent()) {
                return column;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // readSingleColumnWithoutLock can only be used inside write-lock block. This Method returns either a copy of ori-column or null
    private Column readSingleColumnWithoutLock(String columnName) {
        Column column = columns.getOrDefault(columnName, null);
        if (Optional.ofNullable(column).isPresent()) {
            return new Column(column.getName(), column.getVal());
        }
        return null;
    }

    protected List<Column> readMultiColumns(List<String> names) {
        try {
            lock.readLock().lock();
            List<Column> result = new ArrayList<>();
            for (String name : names) {
                Column column = readSingleColumnWithoutLock(name);
                if (Optional.ofNullable(column).isPresent()) {
                    result.add(column);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getAllColumnNames() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(columns.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
}
