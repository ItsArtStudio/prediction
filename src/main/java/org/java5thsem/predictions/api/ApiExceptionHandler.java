package org.java5thsem.predictions.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception) {
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request validation failed");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request validation failed");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request body is missing or invalid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Parameter '%s' has an invalid value".formatted(exception.getName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_PREDICTION,
                "A prediction already exists for this user and match");
    }

    private static ProblemDetail problem(HttpStatus status, ErrorCode code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(code.name());
        detail.setProperty("code", code.name());
        return detail;
    }
}
