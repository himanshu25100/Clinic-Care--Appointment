package com.clinic.exception;

/**
 * Root of every business error in the application.
 *
 * It extends Exception (not RuntimeException) on purpose: these are
 * *expected* problems - a wrong ID, a taken slot - that the caller is
 * required to handle. The compiler enforces that with checked exceptions.
 *
 * Syllabus concepts: user-defined exceptions, exception hierarchy,
 * constructor chaining with super().
 */
public class ClinicException extends Exception {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public ClinicException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClinicException(String errorCode, String message, Throwable cause) {
        super(message, cause);   // keeps the original exception as the cause
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** What the console prints to the user. */
    public String displayMessage() {
        return "[" + errorCode + "] " + getMessage();
    }
}
