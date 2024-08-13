package aop;

import entity.ErrorMessage;
import exception.InvalidReqException;
import exception.UnProcessableCompException;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;

/** @author chaochen */
@RestControllerAdvice

public class RestControllerExceptionHandler {

  @ExceptionHandler(UnProcessableCompException.class)
  @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
  public ErrorMessage unProcessableException(UnProcessableCompException ex, WebRequest request) {
    return new ErrorMessage(
        HttpStatus.UNPROCESSABLE_ENTITY.value(),
        new Date(),
        ex.getMessage(),
        request.getDescription(false));
  }

  @ExceptionHandler(InvalidReqException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ErrorMessage invalidReqException(InvalidReqException ex, WebRequest request) {
    return new ErrorMessage(
        HttpStatus.BAD_REQUEST.value(), new Date(), ex.getMessage(), request.getDescription(false));
  }
}
