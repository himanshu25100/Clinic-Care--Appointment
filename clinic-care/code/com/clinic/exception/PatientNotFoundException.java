package com.clinic.exception;

/** Thrown when a patient ID does not exist in the registry. */
public class PatientNotFoundException extends ClinicException {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    private final String patientId;

    public PatientNotFoundException(String patientId) {
        super("ERR-404-PAT", "No patient is registered with ID '" + patientId
                + "'. Register as a new patient first.");
        this.patientId = patientId;
    }

    public String getPatientId() {
        return patientId;
    }
}
