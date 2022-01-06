package exception;

/** @author chaochen */
public class InvalidReqException extends RuntimeException {
  private static final long serialVersionUID = -5987820352168221240L;

  public InvalidReqException(String message) {
    super(message);
  }
}
