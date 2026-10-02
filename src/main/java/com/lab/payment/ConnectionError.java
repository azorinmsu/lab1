package com.lab.payment;

public class ConnectionError extends RuntimeException {
    public ConnectionError(String message) {
        super(message);
    }
}
