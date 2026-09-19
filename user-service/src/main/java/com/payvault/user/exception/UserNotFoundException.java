package com.payvault.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("No user found for userId: " + userId);
    }
}
