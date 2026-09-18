package com.clinic.exception;

/** Thrown by the validation layer when user input fails a rule. */
public class InvalidInputException extends ClinicException {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidInputException(String field, String message) {
        super("ERR-400-INP", message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
