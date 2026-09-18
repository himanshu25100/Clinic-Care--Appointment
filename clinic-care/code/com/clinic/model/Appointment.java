package com.clinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One booked consulting slot: which patient, which doctor, which day, which slot.
 *
 * Syllabus concepts: encapsulation, enums, java.time, Comparable interface.
 */
public class Appointment implements Comparable<Appointment> {

    private final String id;
    private final String patientId;
    private final String doctorId;
    private final LocalDate date;
    private final int slotIndex;          // 0 .. Doctor.SLOTS_PER_DAY-1
    private String reason;
    private AppointmentStatus status;
    private final LocalDateTime bookedAt;
    private double feeCharged;

    public Appointment(String id, String patientId, String doctorId, LocalDate date,
                       int slotIndex, String reason, AppointmentStatus status,
                       LocalDateTime bookedAt, double feeCharged) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.slotIndex = slotIndex;
        this.reason = reason;
        this.status = status;
        this.bookedAt = bookedAt;
        this.feeCharged = feeCharged;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public double getFeeCharged() {
        return feeCharged;
    }

    public void setFeeCharged(double feeCharged) {
        this.feeCharged = feeCharged;
    }

    /** An active booking is one that still occupies a slot. */
    public boolean isActive() {
        return status == AppointmentStatus.BOOKED;
    }

    public String getSlotLabel() {
        return Doctor.slotLabel(slotIndex);
    }

    /** Natural order: earliest date first, then earliest slot. */
    @Override
    public int compareTo(Appointment other) {
        int byDate = this.date.compareTo(other.date);
        if (byDate != 0) {
            return byDate;
        }
        return Integer.compare(this.slotIndex, other.slotIndex);
    }

    public String toCsv() {
        // commas inside the reason would break the file, so they are swapped out
        String safeReason = reason == null ? "" : reason.replace(",", ";");
        return String.join(",",
                id, patientId, doctorId, date.toString(), String.valueOf(slotIndex),
                safeReason, status.name(), bookedAt.toString(), String.valueOf(feeCharged));
    }

    public static Appointment fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 9) {
            return null;
        }
        try {
            return new Appointment(parts[0], parts[1], parts[2],
                    LocalDate.parse(parts[3]), Integer.parseInt(parts[4]), parts[5],
                    AppointmentStatus.fromText(parts[6]),
                    LocalDateTime.parse(parts[7]), Double.parseDouble(parts[8]));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("%-8s %s  %-15s Dr:%-6s %-12s Rs.%.2f  %s",
                id, date, getSlotLabel(), doctorId, status, feeCharged,
                reason == null ? "" : reason);
    }
}
