package com.clinic.model;

import java.time.LocalDate;

/**
 * A registered patient of the clinic.
 *
 * Syllabus concepts: inheritance, method overriding, super keyword.
 */
public class Patient extends Person {

    private int age;
    private String gender;        // M / F / O
    private String bloodGroup;
    private final LocalDate registeredOn;

    public Patient(String id, String name, String phone, int age,
                   String gender, String bloodGroup, LocalDate registeredOn) {
        super(id, name, phone);   // super keyword -> parent constructor
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.registeredOn = registeredOn;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public LocalDate getRegisteredOn() {
        return registeredOn;
    }

    /** Senior citizens get a discount, so the rest of the app asks this. */
    public boolean isSeniorCitizen() {
        return age >= 60;
    }

    @Override
    public String describe() {
        return String.format("%-8s %-22s %3d %-3s %-5s  %s",
                getId(), getName(), age, gender, bloodGroup, getPhone());
    }

    @Override
    public String toCsv() {
        // id,name,phone,age,gender,bloodGroup,registeredOn
        return String.join(",",
                getId(), getName(), getPhone(), String.valueOf(age),
                gender, bloodGroup, registeredOn.toString());
    }

    /** Rebuilds a Patient from one CSV line. Returns null if the line is broken. */
    public static Patient fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 7) {
            return null;
        }
        try {
            return new Patient(parts[0], parts[1], parts[2],
                    Integer.parseInt(parts[3]), parts[4], parts[5],
                    LocalDate.parse(parts[6]));
        } catch (RuntimeException ex) {
            return null;  // corrupt row -> skip it instead of crashing
        }
    }
}
