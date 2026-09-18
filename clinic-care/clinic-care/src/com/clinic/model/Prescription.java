package com.clinic.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The record a doctor writes after a consultation. This is what a returning
 * patient reads when they ask for their history.
 *
 * Syllabus concepts: collections (ArrayList), String methods and splitting,
 * defensive copying, static nested class.
 */
public class Prescription {

    /**
     * Static nested class - one medicine line inside a prescription.
     * Nested because a Medicine has no meaning outside a Prescription.
     */
    public static class Medicine {
        private final String name;
        private final String dosage;    // e.g. "1-0-1"
        private final int days;

        public Medicine(String name, String dosage, int days) {
            this.name = name;
            this.dosage = dosage;
            this.days = days;
        }

        public String getName() {
            return name;
        }

        public String getDosage() {
            return dosage;
        }

        public int getDays() {
            return days;
        }

        @Override
        public String toString() {
            return String.format("%-20s %-8s for %d day(s)", name, dosage, days);
        }

        /** Packed form used inside the CSV cell: name|dosage|days */
        public String pack() {
            return name.replace("|", "-") + "|" + dosage.replace("|", "-") + "|" + days;
        }

        public static Medicine unpack(String packed) {
            String[] p = packed.split("\\|");
            if (p.length < 3) {
                return null;
            }
            try {
                return new Medicine(p[0], p[1], Integer.parseInt(p[2]));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    private final String id;
    private final String appointmentId;
    private final String patientId;
    private final String doctorId;
    private final LocalDate issuedOn;
    private final String diagnosis;
    private final List<Medicine> medicines;
    private final String advice;
    private final LocalDate followUpOn;   // may be null

    public Prescription(String id, String appointmentId, String patientId, String doctorId,
                        LocalDate issuedOn, String diagnosis, List<Medicine> medicines,
                        String advice, LocalDate followUpOn) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.issuedOn = issuedOn;
        this.diagnosis = diagnosis;
        // defensive copy: nobody outside can mutate our list behind our back
        this.medicines = new ArrayList<>(medicines);
        this.advice = advice;
        this.followUpOn = followUpOn;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public List<Medicine> getMedicines() {
        return new ArrayList<>(medicines);
    }

    public String getAdvice() {
        return advice;
    }

    public LocalDate getFollowUpOn() {
        return followUpOn;
    }

    public String toCsv() {
        StringBuilder packed = new StringBuilder();
        for (int i = 0; i < medicines.size(); i++) {
            if (i > 0) {
                packed.append(";");
            }
            packed.append(medicines.get(i).pack());
        }
        return String.join(",",
                id, appointmentId, patientId, doctorId, issuedOn.toString(),
                diagnosis.replace(",", ";"), packed.toString(),
                advice == null ? "" : advice.replace(",", ";"),
                followUpOn == null ? "NONE" : followUpOn.toString());
    }

    public static Prescription fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 9) {
            return null;
        }
        try {
            List<Medicine> meds = new ArrayList<>();
            if (!parts[6].trim().isEmpty()) {
                String[] chunks = parts[6].split(";");
                for (String chunk : chunks) {
                    Medicine m = Medicine.unpack(chunk);
                    if (m != null) {
                        meds.add(m);
                    }
                }
            }
            LocalDate followUp = "NONE".equals(parts[8]) || parts[8].trim().isEmpty()
                    ? null : LocalDate.parse(parts[8]);
            return new Prescription(parts[0], parts[1], parts[2], parts[3],
                    LocalDate.parse(parts[4]), parts[5], meds, parts[7], followUp);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
