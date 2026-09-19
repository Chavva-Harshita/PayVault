package com.payvault.transaction.exception;

import com.payvault.transaction.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(InvalidAmountException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", ex.getMessage(), request);
    }

    @ExceptionHandler(SelfTransferException.class)
    public ResponseEntity<ErrorResponse> handleSelfTransfer(SelfTransferException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "SELF_TRANSFER_NOT_ALLOWED", ex.getMessage(), request);
    }

    @ExceptionHandler(ReceiverNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReceiverNotFound(ReceiverNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RECEIVER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", ex.getMessage(), request);
    }

    @ExceptionHandler(WalletUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleWalletUnavailable(WalletUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "WALLET_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenTransactionAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenTransactionAccessException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateRequestInProgressException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInProgress(DuplicateRequestInProgressException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ErrorResponse> handleKeyReused(IdempotencyKeyReusedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingUserContextException.class)
    public ResponseEntity<ErrorResponse> handleMissingUser(MissingUserContextException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleMissingKey(MissingIdempotencyKeyException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
