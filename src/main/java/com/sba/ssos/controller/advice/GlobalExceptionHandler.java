package com.sba.ssos.controller.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex) {

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setTitle("Internal Error");
    problem.setDetail("An unexpected error occurred");
    //        problem.setProperty("errorCode", ErrorCode.INTERNAL_ERROR.getCode());

    return problem;
  }
}
