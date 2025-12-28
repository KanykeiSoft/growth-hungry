package com.example.growth_hungry.api;


import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 400 validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of("field", fe.getField(), "msg", fe.getDefaultMessage()))
                .toList();

        return new ErrorResponse("VALIDATION_ERROR", "Invalid input", details);
    }

    // 409 username exists
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(UsernameAlreadyExistsException ex) {
        return ErrorResponse.of("CONFLICT", ex.getMessage());
    }

    // 401 bad creds
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCreds(BadCredentialsException ex) {
        return ErrorResponse.of("BAD_CREDENTIALS", "Username or password is incorrect");
    }

    // 409 email exists
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailConflict(EmailAlreadyExistsException ex) {
        return ErrorResponse.of("EMAIL_EXISTS", ex.getMessage());
    }

    // ✅ если ты кидаешь new ResponseStatusException(...)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(status.name(), ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()));
    }

    // ✅ иногда Spring кидает ErrorResponseException (Spring 6+)
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(ErrorResponseException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(status.name(), ex.getMessage()));
    }

    // ✅ ГЛАВНОЕ: ловим все остальные ошибки => вместо пустого 500 ты увидишь message и stacktrace в IntelliJ
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAny(Exception ex) {
        ex.printStackTrace(); // <-- увидишь реальную причину 500 в консоли
        String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
        return ErrorResponse.of("INTERNAL_ERROR", msg);
    }
}

