package com.clinic.exception;

/**
 * Wraps low level IOException / SQLException so the rest of the app never
 * has to know whether storage is a CSV file or a database.
 */
public class StorageException extends ClinicException {

    /** Required because Exception implements Serializable. */
    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super("ERR-500-IO", message, cause);
    }
}
