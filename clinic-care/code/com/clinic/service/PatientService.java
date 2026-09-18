package com.clinic.service;

import com.clinic.exception.InvalidInputException;
import com.clinic.exception.PatientNotFoundException;
import com.clinic.model.Patient;
import com.clinic.storage.DataStore;
import com.clinic.util.InputValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MODULE 1 - Patient Management.
 *
 * Registration of new patients, lookup of existing ones, profile updates and
 * search. The service layer owns all the rules; the UI only collects text.
 *
 * Syllabus concepts: encapsulation, collections, exception handling,
 * String methods, recursion.
 */
public class PatientService {

    private final DataStore store;

    public PatientService(DataStore store) {
        this.store = store;
    }

    /**
     * Registers a brand new patient and returns the generated ID card number.
     * Every argument is validated before a single field is stored.
     */
    public Patient register(String name, String phone, String ageText,
                            String gender, String bloodGroup)
            throws InvalidInputException {

        String cleanName = InputValidator.validateName(name);
        String cleanPhone = InputValidator.validatePhone(phone);
        int age = InputValidator.validateAge(ageText);
        String cleanGender = InputValidator.validateGender(gender);
        String cleanBlood = InputValidator.validateBloodGroup(bloodGroup);

        // Business rule: the same phone number cannot register twice.
        Patient existing = findByPhone(cleanPhone);
        if (existing != null) {
            throw new InvalidInputException("Phone",
                    "This phone number is already registered as "
                            + existing.getId() + " (" + existing.getName() + ").");
        }

        Patient patient = new Patient(store.nextPatientId(), cleanName, cleanPhone,
                age, cleanGender, cleanBlood, LocalDate.now());
        store.addPatient(patient);
        return patient;
    }

    /** Looks a patient up, or throws - callers never get a null back. */
    public Patient requirePatient(String rawId)
            throws InvalidInputException, PatientNotFoundException {
        String id = InputValidator.validatePatientId(rawId);
        Patient patient = store.findPatient(id);
        if (patient == null) {
            throw new PatientNotFoundException(id);
        }
        return patient;
    }

    public Patient findByPhone(String phone) {
        for (Patient p : store.allPatients()) {
            if (p.getPhone().equals(phone)) {
                return p;
            }
        }
        return null;
    }

    /** Case-insensitive partial name search, used by the reception desk. */
    public List<Patient> searchByName(String fragment) {
        List<Patient> matches = new ArrayList<>();
        if (fragment == null || fragment.trim().isEmpty()) {
            return matches;
        }
        String needle = fragment.trim().toLowerCase();
        for (Patient p : store.allPatients()) {
            if (p.getName().toLowerCase().contains(needle)) {
                matches.add(p);
            }
        }
        return matches;
    }

    public void updatePhone(String patientId, String newPhone)
            throws InvalidInputException, PatientNotFoundException {
        Patient patient = requirePatient(patientId);
        patient.setPhone(InputValidator.validatePhone(newPhone));
    }

    public void updateAge(String patientId, String newAge)
            throws InvalidInputException, PatientNotFoundException {
        Patient patient = requirePatient(patientId);
        patient.setAge(InputValidator.validateAge(newAge));
    }

    public List<Patient> listAll() {
        return store.allPatients();
    }

    /**
     * Counts senior citizens using recursion instead of a loop.
     * Recursion is on the syllabus, and this is a clean place for it: the list
     * shrinks by one on every call until the empty list ends it.
     */
    public int countSeniorCitizens() {
        return countSeniors(store.allPatients(), 0);
    }

    private int countSeniors(List<Patient> list, int index) {
        if (index >= list.size()) {      // base case
            return 0;
        }
        int thisOne = list.get(index).isSeniorCitizen() ? 1 : 0;
        return thisOne + countSeniors(list, index + 1);   // recursive case
    }
}
