package com.xcoder.easyinsta.exceptions;

public class IGLoginException extends Exception {
    private final Reasons reason;

    public IGLoginException(String message, Reasons reason) {
        super(message);
        this.reason = reason;
    }

    public Reasons getReason() {
        return reason;
    }
}
