package exception;

/** @author chaochen */
public class UnProcessableCompException extends RuntimeException {
  private static final long serialVersionUID = 6642274765371846626L;

  public UnProcessableCompException(String message) {
    super(message);
  }
}
