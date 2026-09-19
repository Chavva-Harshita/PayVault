package com.payvault.transaction.exception;

public class ReceiverNotFoundException extends RuntimeException {

    public ReceiverNotFoundException(String receiverId) {
        super("No valid receiver found for: " + receiverId);
    }
}
