package org.example.v1.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.errors.log-stacktrace:true}")
    private boolean logStacktrace;

    /* ===== helpers ===== */
    private static String rootMsg(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }

    private static String sqlStateOf(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof SQLException se) return se.getSQLState();
            t = t.getCause();
        }
        return null;
    }

    private void warnNoStack(String msg, Throwable ex) {
        if (logStacktrace) log.warn("{} | cause={}", msg, rootMsg(ex), ex);
        else               log.warn("{} | cause={}", msg, rootMsg(ex));
    }

    private void errorNoStack(String msg, Throwable ex) {
        if (logStacktrace) log.error("{} | cause={}", msg, rootMsg(ex), ex);
        else               log.error("{} | cause={}", msg, rootMsg(ex));
    }

    /* ===== handlers ===== */

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorDto> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.warn("DataAccessException: {}", ex.getMessage(), ex);
        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.badRequest().body(currentError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgumentException: {}", ex.getMessage(), ex);
        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.badRequest().body(currentError);
    }

    @ExceptionHandler(InvalidFilterFieldException.class)
    public ResponseEntity<ErrorDto> handleInvalidFilter(InvalidFilterFieldException ex, HttpServletRequest request) {
        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.BAD_REQUEST.value()
        );
        log.warn("InvalidFilterFieldException: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(currentError);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDto> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);

        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        log.error(ex.getClass().getSimpleName() + ": {}", ex.getMessage(), ", cause: " + ex.getCause());
        return ResponseEntity.internalServerError().body(currentError);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDto> handleInternalError(ResponseStatusException ex, HttpServletRequest request){
        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                ex.getStatusCode().value()
        );
        log.error(ex.getClass().getSimpleName() + ": {}", ex.getMessage(), ", cause: " + ex.getCause());
        return ResponseEntity.internalServerError().body(currentError);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorDto> handleNullPointerException(NullPointerException ex, HttpServletRequest request){
        ErrorDto currentError = new ErrorDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
        log.error(ex.getClass().getSimpleName() + ": {}", ex.getMessage(), ", cause: " + ex.getCause());
        return ResponseEntity.internalServerError().body(currentError);
    }

}
