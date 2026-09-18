package com.clinic.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A doctor who consults at the clinic.
 *
 * Syllabus concepts: inheritance, static members, arrays, enums,
 * method overriding, polymorphism.
 */
public class Doctor extends Person {

    /**
     * The eight consulting slots every working day is divided into.
     * static + final: one shared copy for the whole program.
     */
    public static final String[] SLOT_TIMES = {
            "09:00 - 09:30",
            "09:30 - 10:00",
            "10:00 - 10:30",
            "10:30 - 11:00",
            "16:00 - 16:30",
            "16:30 - 17:00",
            "17:00 - 17:30",
            "17:30 - 18:00"
    };

    public static final int SLOTS_PER_DAY = SLOT_TIMES.length;

    private Specialization specialization;
    private String roomNo;
    private boolean onLeave;

    public Doctor(String id, String name, String phone,
                  Specialization specialization, String roomNo, boolean onLeave) {
        super(id, name, phone);
        this.specialization = specialization;
        this.roomNo = roomNo;
        this.onLeave = onLeave;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public boolean isOnLeave() {
        return onLeave;
    }

    public void setOnLeave(boolean onLeave) {
        this.onLeave = onLeave;
    }

    public double getConsultationFee() {
        return specialization.getBaseFee();
    }

    /** The clinic is closed on Sundays, and a doctor on leave sees nobody. */
    public boolean worksOn(LocalDate date) {
        if (onLeave) {
            return false;
        }
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** Converts a slot index into a human readable time, safely. */
    public static String slotLabel(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOTS_PER_DAY) {
            return "??:??";
        }
        return SLOT_TIMES[slotIndex];
    }

    @Override
    public String describe() {
        return String.format("%-6s %-22s %-18s Room %-4s Fee Rs.%-7.2f %s",
                getId(), getName(), specialization.getLabel(), roomNo,
                getConsultationFee(), onLeave ? "[ON LEAVE]" : "");
    }

    @Override
    public String toCsv() {
        return String.join(",",
                getId(), getName(), getPhone(), specialization.name(),
                roomNo, String.valueOf(onLeave));
    }

    public static Doctor fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            return null;
        }
        try {
            return new Doctor(parts[0], parts[1], parts[2],
                    Specialization.fromText(parts[3]), parts[4],
                    Boolean.parseBoolean(parts[5]));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
