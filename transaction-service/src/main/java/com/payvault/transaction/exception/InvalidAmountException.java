package com.payvault.transaction.exception;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException() {
        super("amount must be greater than zero");
    }
}
