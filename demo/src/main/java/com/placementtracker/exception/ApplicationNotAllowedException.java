package com.placementtracker.exception;

public class ApplicationNotAllowedException extends RuntimeException {
    public ApplicationNotAllowedException(String message) {
        super(message);
    }
}
