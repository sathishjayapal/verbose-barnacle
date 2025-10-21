package me.sathish.my_github_cleaner.base.eventracker;

/**
 * Custom exception for EventTracker service operations.
 * This exception is thrown when there are issues with event tracking operations
 * such as network failures, configuration errors, or service unavailability.
 */
public class EventTrackerException extends RuntimeException {

    /**
     * Constructs a new EventTrackerException with the specified detail message.
     *
     * @param message the detail message
     */
    public EventTrackerException(String message) {
        super(message);
    }

    /**
     * Constructs a new EventTrackerException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public EventTrackerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new EventTrackerException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public EventTrackerException(Throwable cause) {
        super(cause);
    }
}
