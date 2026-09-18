package com.clinic.exception;

/** Thrown when a doctor ID does not exist. */
public class DoctorNotFoundException extends ClinicException {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    public DoctorNotFoundException(String doctorId) {
        super("ERR-404-DOC", "No doctor found with ID '" + doctorId + "'.");
    }
}
