package db;

/**
 * @author chaochen
 */

public class DBInternalException extends RuntimeException {
    private static final long serialVersionUID = 1597839873881812431L;

    public DBInternalException(String message) {
        super(message);
    }
}
