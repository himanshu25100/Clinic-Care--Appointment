package com.clinic.model;

/**
 * Departments the clinic runs.
 *
 * Syllabus concepts: enum class, enum constructor, enum fields,
 * enum -> String and String -> enum conversion.
 */
public enum Specialization {

    GENERAL("General Medicine", 300.0),
    PEDIATRICS("Pediatrics", 450.0),
    DERMATOLOGY("Dermatology", 500.0),
    ORTHOPEDICS("Orthopedics", 600.0),
    CARDIOLOGY("Cardiology", 800.0);

    private final String label;
    private final double baseFee;

    // enum constructor - always private, runs once per constant
    Specialization(String label, double baseFee) {
        this.label = label;
        this.baseFee = baseFee;
    }

    public String getLabel() {
        return label;
    }

    public double getBaseFee() {
        return baseFee;
    }

    /** Safe lookup: returns GENERAL instead of throwing on unknown text. */
    public static Specialization fromText(String text) {
        if (text == null) {
            return GENERAL;
        }
        for (Specialization s : values()) {
            if (s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return GENERAL;
    }

    @Override
    public String toString() {
        return label;
    }
}
