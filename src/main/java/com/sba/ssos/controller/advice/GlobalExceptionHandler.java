package com.sba.ssos.controller.advice;

import com.sba.ssos.exception.base.BaseException;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  private final LocaleUtils localeUtils;

  @ExceptionHandler(BaseException.class)
  public ProblemDetail handleBaseException(BaseException ex, HttpServletRequest request) {
    String detail = localeUtils.get(ex.getMessage(), ex.getParams().values().toArray());
    ProblemDetail problem =
        createProblem(HttpStatus.valueOf(ex.getStatus().value()), detail, request, ex.getMessage());
    if (!ex.getParams().isEmpty()) {
      problem.setProperty("params", ex.getParams());
    }
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    log.warn("Validation failed: {}", ex.getMessage());

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fieldError -> localeUtils.get(fieldError.getDefaultMessage()),
                    (first, second) -> first,
                    LinkedHashMap::new));

    String detail =
        fieldErrors.values().stream().findFirst().orElse(localeUtils.get("error.validation.failed"));
    ProblemDetail problem =
        createProblem(HttpStatus.BAD_REQUEST, detail, request, "error.validation.failed");
    problem.setProperty("errors", fieldErrors);
    return problem;
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ProblemDetail handleHandlerMethodValidation(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    log.warn("Handler method validation failed: {}", ex.getMessage());

    Map<String, String> errors = new LinkedHashMap<>();
    ex.getParameterValidationResults()
        .forEach(
            validationResult ->
                validationResult
                    .getResolvableErrors()
                    .forEach(
                        error ->
                            errors.putIfAbsent(
                                validationResult.getMethodParameter().getParameterName(),
                                localeUtils.get(error.getDefaultMessage()))));

    String detail =
        errors.values().stream().findFirst().orElse(localeUtils.get("error.validation.failed"));
    ProblemDetail problem =
        createProblem(HttpStatus.BAD_REQUEST, detail, request, "error.validation.failed");
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    log.warn("Constraint violation: {}", ex.getMessage());

    Map<String, String> errors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    violation -> violation.getMessage(),
                    (first, second) -> first,
                    LinkedHashMap::new));

    String detail =
        errors.values().stream().findFirst().orElse(localeUtils.get("error.validation.failed"));
    ProblemDetail problem =
        createProblem(HttpStatus.BAD_REQUEST, detail, request, "error.validation.failed");
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Malformed request body: {}", ex.getMessage());
    return createProblem(
        HttpStatus.BAD_REQUEST,
        localeUtils.get("error.request.malformed_json"),
        request,
        "error.request.malformed_json");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    log.warn("Request argument type mismatch: {}", ex.getMessage());
    return createProblem(
        HttpStatus.BAD_REQUEST,
        localeUtils.get("error.validation.failed"),
        request,
        "error.validation.failed");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ProblemDetail handleMissingRequestParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    log.warn("Missing request parameter: {}", ex.getMessage());
    return createProblem(
        HttpStatus.BAD_REQUEST,
        localeUtils.get("error.validation.failed"),
        request,
        "error.validation.failed");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Bad request argument: {}", ex.getMessage());
    return createProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), request, "error.validation.failed");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Access denied: {}", ex.getMessage());
    return createProblem(
        HttpStatus.FORBIDDEN, localeUtils.get("error.auth.forbidden"), request, "error.auth.forbidden");
  }

  @ExceptionHandler({AuthenticationException.class, JwtException.class})
  public ProblemDetail handleAuthenticationError(Exception ex, HttpServletRequest request) {
    log.warn("Authentication error: {}", ex.getMessage());
    return createProblem(
        HttpStatus.UNAUTHORIZED,
        localeUtils.get("error.auth.unauthorized"),
        request,
        "error.auth.unauthorized");
  }

  @ExceptionHandler(MultipartException.class)
  public ProblemDetail handleMultipart(MultipartException ex, HttpServletRequest request) {
    log.warn("Multipart error: {}", ex.getMessage());
    ProblemDetail problem =
        createProblem(
            HttpStatus.BAD_REQUEST,
            localeUtils.get("error.validation.failed"),
            request,
            "error.validation.failed");
    problem.setProperty("cause", ex.getMessage());
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return createProblem(
        HttpStatus.CONFLICT, localeUtils.get("error.data.integrity"), request, "error.data.integrity");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", ex);
    return createProblem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        localeUtils.get("error.internal.server"),
        request,
        "error.internal.server");
  }

  private ProblemDetail createProblem(
      HttpStatus status, String detail, HttpServletRequest request, String messageKey) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("messageKey", messageKey);
    return problem;
  }
}
