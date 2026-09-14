package com.placementtracker.exception;

// Generic 400 for "this action doesn't make sense from the entity's current state" - e.g.
// submitting a Drive for approval when it's already APPROVED. Distinct from
// ApplicationNotAllowedException, which is specifically about the student-application flow.
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
