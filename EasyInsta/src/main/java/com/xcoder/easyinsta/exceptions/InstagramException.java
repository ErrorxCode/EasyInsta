package com.xcoder.easyinsta.exceptions;

public class InstagramException extends RuntimeException {
    private final Reasons reason;

    public InstagramException(String message, Reasons reason) {
        super(message);
        this.reason = reason;
    }

    public Reasons getReason() {
        return reason;
    }
}
