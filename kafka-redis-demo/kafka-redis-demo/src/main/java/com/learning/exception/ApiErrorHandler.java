package com.learning.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PublishException.class)
    public ProblemDetail handleKafkaUnavailability(PublishException ex) {
        log.error("Kafka publish failed", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable");
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ProblemDetail handleRedisUnavailability(RedisConnectionFailureException ex) {
        log.error("Redis unavailable", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Order storage unavailable");
    }

    @ExceptionHandler(OrderAlreadyExistsException.class)
    public ProblemDetail handlerOrderExists(OrderAlreadyExistsException ex) {
        log.error(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateKey(DataIntegrityViolationException ex) {
        log.error(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Order already exists");
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ProblemDetail handleDbUnavailable(CannotCreateTransactionException ex) {
        log.error(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Order storage unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
    }
}
