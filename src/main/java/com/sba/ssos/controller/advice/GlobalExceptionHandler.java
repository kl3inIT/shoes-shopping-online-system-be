package com.sba.ssos.controller.advice;

import com.sba.ssos.exception.base.BaseException;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  private final LocaleUtils localeUtils;

  @ExceptionHandler(BaseException.class)
  public ProblemDetail handleBaseException(BaseException ex, HttpServletRequest request) {

    String detail = localeUtils.get(ex.getMessage(), ex.getParams().values().toArray());

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), detail);
    problem.setTitle(ex.getStatus().getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("messageKey", ex.getMessage());
    if (!ex.getParams().isEmpty()) {
      problem.setProperty("params", ex.getParams());
    }
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    log.warn("Validation failed: {}", ex.getMessage());

    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> localeUtils.get(error.getDefaultMessage(), error.getArguments()))
            .orElse("Validation failed");

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);

    problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fe -> localeUtils.get(fe.getDefaultMessage(), fe.getArguments()),
                    (a, b) -> a,
                    LinkedHashMap::new));

    problem.setProperty("errors", fieldErrors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Malformed request body: {}", ex.getMessage());

    String detail = localeUtils.get("error.request.malformed_json");

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Access denied: {}", ex.getMessage());

    String detail = localeUtils.get("error.auth.forbidden");

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    problem.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler({AuthenticationException.class, JwtException.class})
  public ProblemDetail handleAuthenticationError(Exception ex, HttpServletRequest request) {
    log.warn("Authentication error: {}", ex.getMessage());

    String detail = localeUtils.get("error.auth.unauthorized");

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

    String detail = localeUtils.get("error.data.integrity");
    String cause = ex.getMostSpecificCause().getMessage();
    if (cause != null && !cause.isBlank()) {
      detail = detail + " " + cause;
    }

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", ex);

    String detail = localeUtils.get("error.internal.server");

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail);
    problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }
}
