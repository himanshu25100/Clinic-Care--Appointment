package com.clinic.model;

/**
 * Life cycle of an appointment.
 *
 * BOOKED -> COMPLETED  (patient was seen, prescription written)
 * BOOKED -> CANCELLED  (cancelled by patient or clinic)
 * BOOKED -> NO_SHOW    (patient never arrived)
 *
 * COMPLETED / CANCELLED / NO_SHOW are terminal states.
 */
public enum AppointmentStatus {

    BOOKED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public boolean isTerminal() {
        return this != BOOKED;
    }

    /** Guard used by the service layer so illegal transitions are impossible. */
    public boolean canMoveTo(AppointmentStatus next) {
        return this == BOOKED && next != BOOKED;
    }

    public static AppointmentStatus fromText(String text) {
        if (text != null) {
            for (AppointmentStatus s : values()) {
                if (s.name().equalsIgnoreCase(text.trim())) {
                    return s;
                }
            }
        }
        return BOOKED;
    }
}
