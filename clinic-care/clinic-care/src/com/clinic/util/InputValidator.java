package com.clinic.util;

import com.clinic.exception.InvalidInputException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Every piece of text typed by a user passes through here before it becomes
 * data. Nothing else in the system is allowed to trust raw input.
 *
 * Syllabus concepts: static utility class, String methods, regular expressions,
 * throwing checked exceptions, wrapping RuntimeException into a checked one.
 */
public final class InputValidator {

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String NAME_PATTERN = "[A-Za-z][A-Za-z .'-]{1,39}";
    private static final String PHONE_PATTERN = "[6-9][0-9]{9}";
    private static final String PATIENT_ID_PATTERN = "(?i)P-[0-9]{3,}";
    private static final String DOCTOR_ID_PATTERN = "(?i)D-[0-9]{1,}";

    private InputValidator() {
        // utility class - never instantiated
    }

    public static String requireText(String field, String value)
            throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field, field + " cannot be empty.");
        }
        return value.trim();
    }

    public static String validateName(String value) throws InvalidInputException {
        String name = requireText("Name", value);
        if (!name.matches(NAME_PATTERN)) {
            throw new InvalidInputException("Name",
                    "Name must be 2-40 letters and may contain spaces, dots, hyphens.");
        }
        return name;
    }

    public static String validatePhone(String value) throws InvalidInputException {
        String phone = requireText("Phone", value).replaceAll("[\\s-]", "");
        if (!phone.matches(PHONE_PATTERN)) {
            throw new InvalidInputException("Phone",
                    "Phone must be exactly 10 digits and start with 6, 7, 8 or 9.");
        }
        return phone;
    }

    public static int validateAge(String value) throws InvalidInputException {
        String text = requireText("Age", value);
        int age;
        try {
            age = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new InvalidInputException("Age", "Age must be a whole number.");
        }
        if (age < 0 || age > 120) {
            throw new InvalidInputException("Age", "Age must be between 0 and 120.");
        }
        return age;
    }

    public static String validateGender(String value) throws InvalidInputException {
        String g = requireText("Gender", value).toUpperCase();
        if (!g.equals("M") && !g.equals("F") && !g.equals("O")) {
            throw new InvalidInputException("Gender", "Gender must be M, F or O.");
        }
        return g;
    }

    public static String validateBloodGroup(String value) throws InvalidInputException {
        String bg = requireText("Blood group", value).toUpperCase().replace(" ", "");
        if (!bg.matches("(A|B|AB|O)[+-]")) {
            throw new InvalidInputException("Blood group",
                    "Blood group must look like A+, B-, AB+, O- ...");
        }
        return bg;
    }

    public static String validatePatientId(String value) throws InvalidInputException {
        String id = requireText("Patient ID", value).toUpperCase();
        if (!id.matches(PATIENT_ID_PATTERN)) {
            throw new InvalidInputException("Patient ID",
                    "Patient ID looks like P-1001.");
        }
        return id;
    }

    public static String validateDoctorId(String value) throws InvalidInputException {
        String id = requireText("Doctor ID", value).toUpperCase();
        if (!id.matches(DOCTOR_ID_PATTERN)) {
            throw new InvalidInputException("Doctor ID", "Doctor ID looks like D-01.");
        }
        return id;
    }

    /** Parses dd-MM-yyyy and refuses anything in the past. */
    public static LocalDate validateFutureDate(String value) throws InvalidInputException {
        LocalDate date = parseDate(value);
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidInputException("Date", "That date is already in the past.");
        }
        if (date.isAfter(LocalDate.now().plusDays(60))) {
            throw new InvalidInputException("Date",
                    "Appointments open only 60 days in advance.");
        }
        return date;
    }

    public static LocalDate parseDate(String value) throws InvalidInputException {
        String text = requireText("Date", value);
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            // an unchecked exception is converted into our checked one
            throw new InvalidInputException("Date", "Date must be in dd-MM-yyyy format.");
        }
    }

    public static int validateMenuChoice(String value, int min, int max)
            throws InvalidInputException {
        String text = requireText("Choice", value);
        int choice;
        try {
            choice = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new InvalidInputException("Choice", "Please type a number.");
        }
        if (choice < min || choice > max) {
            throw new InvalidInputException("Choice",
                    "Choose a number between " + min + " and " + max + ".");
        }
        return choice;
    }
}
