package com.payvault.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        // Deliberately generic: never reveal whether the email exists or the
        // password was wrong, since that distinction helps an attacker
        // enumerate valid accounts.
        super("Invalid email or password");
    }
}
