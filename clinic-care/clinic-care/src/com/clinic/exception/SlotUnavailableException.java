package com.clinic.exception;

/**
 * Thrown when the requested consulting slot cannot be booked:
 * already taken, in the past, a Sunday, or the doctor is on leave.
 */
public class SlotUnavailableException extends ClinicException {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    public SlotUnavailableException(String message) {
        super("ERR-409-SLOT", message);
    }
}
