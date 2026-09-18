package com.clinic.storage;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Prescription;
import com.clinic.model.Specialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single in-memory database of the running application.
 *
 * Implemented as a Singleton because two copies of the clinic's data would be
 * a disaster: the booking thread and the auto-save thread must see the very
 * same lists.
 *
 * Every method that touches a collection is synchronized, so the background
 * auto-save thread can never read a half-written list.
 *
 * Syllabus concepts: Singleton pattern, static members, HashMap / LinkedHashMap,
 * ArrayList, synchronized methods, thread safety, unmodifiable collections.
 */
public final class DataStore {

    // Eager singleton: created when the class is first loaded, so no
    // double-checked-locking puzzle and no race at start-up.
    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, Doctor> doctors = new LinkedHashMap<>();
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<Prescription> prescriptions = new ArrayList<>();

    // ID counters - the next number each generated ID will use
    private int nextPatientNo = 1001;
    private int nextAppointmentNo = 5001;
    private int nextPrescriptionNo = 9001;

    // set to true by any write, cleared by a successful save
    private boolean dirty = false;

    private DataStore() {
        // private constructor: nobody outside can call 'new DataStore()'
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // Patients
    // ------------------------------------------------------------------

    public synchronized void addPatient(Patient p) {
        patients.put(p.getId(), p);
        dirty = true;
    }

    public synchronized Patient findPatient(String id) {
        if (id == null) {
            return null;
        }
        return patients.get(id.trim().toUpperCase());
    }

    public synchronized List<Patient> allPatients() {
        return Collections.unmodifiableList(new ArrayList<>(patients.values()));
    }

    public synchronized int patientCount() {
        return patients.size();
    }

    // ------------------------------------------------------------------
    // Doctors
    // ------------------------------------------------------------------

    public synchronized void addDoctor(Doctor d) {
        doctors.put(d.getId(), d);
        dirty = true;
    }

    public synchronized Doctor findDoctor(String id) {
        if (id == null) {
            return null;
        }
        return doctors.get(id.trim().toUpperCase());
    }

    public synchronized List<Doctor> allDoctors() {
        return Collections.unmodifiableList(new ArrayList<>(doctors.values()));
    }

    // ------------------------------------------------------------------
    // Appointments
    // ------------------------------------------------------------------

    public synchronized void addAppointment(Appointment a) {
        appointments.add(a);
        dirty = true;
    }

    public synchronized List<Appointment> allAppointments() {
        return Collections.unmodifiableList(new ArrayList<>(appointments));
    }

    public synchronized Appointment findAppointment(String id) {
        if (id == null) {
            return null;
        }
        String key = id.trim().toUpperCase();
        for (Appointment a : appointments) {
            if (a.getId().equals(key)) {
                return a;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Prescriptions
    // ------------------------------------------------------------------

    public synchronized void addPrescription(Prescription r) {
        prescriptions.add(r);
        dirty = true;
    }

    public synchronized List<Prescription> allPrescriptions() {
        return Collections.unmodifiableList(new ArrayList<>(prescriptions));
    }

    // ------------------------------------------------------------------
    // ID generation
    // ------------------------------------------------------------------

    public synchronized String nextPatientId() {
        return "P-" + (nextPatientNo++);
    }

    public synchronized String nextAppointmentId() {
        return "A-" + (nextAppointmentNo++);
    }

    public synchronized String nextPrescriptionId() {
        return "R-" + (nextPrescriptionNo++);
    }

    /**
     * After loading from storage the counters must continue past the highest
     * ID already used, otherwise a new patient would overwrite an old one.
     */
    public synchronized void recalculateCounters() {
        for (String id : patients.keySet()) {
            nextPatientNo = Math.max(nextPatientNo, numericPart(id) + 1);
        }
        for (Appointment a : appointments) {
            nextAppointmentNo = Math.max(nextAppointmentNo, numericPart(a.getId()) + 1);
        }
        for (Prescription r : prescriptions) {
            nextPrescriptionNo = Math.max(nextPrescriptionNo, numericPart(r.getId()) + 1);
        }
    }

    private int numericPart(String id) {
        int dash = id.indexOf('-');
        if (dash < 0 || dash == id.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void markClean() {
        dirty = false;
    }

    /** Used by the loader before re-reading files, and by the test harness. */
    public synchronized void clearAll() {
        patients.clear();
        doctors.clear();
        appointments.clear();
        prescriptions.clear();
        nextPatientNo = 1001;
        nextAppointmentNo = 5001;
        nextPrescriptionNo = 9001;
        dirty = false;
    }

    /**
     * First run of the application: there is no doctors.csv yet, so the clinic
     * is seeded with its permanent staff.
     */
    public synchronized void seedDoctorsIfEmpty() {
        if (!doctors.isEmpty()) {
            return;
        }
        addDoctor(new Doctor("D-01", "Dr. Anita Rao", "9811100011",
                Specialization.GENERAL, "101", false));
        addDoctor(new Doctor("D-02", "Dr. Vikram Shah", "9811100022",
                Specialization.PEDIATRICS, "102", false));
        addDoctor(new Doctor("D-03", "Dr. Meera Iyer", "9811100033",
                Specialization.DERMATOLOGY, "103", false));
        addDoctor(new Doctor("D-04", "Dr. Sanjay Gupta", "9811100044",
                Specialization.ORTHOPEDICS, "201", false));
        addDoctor(new Doctor("D-05", "Dr. Farah Khan", "9811100055",
                Specialization.CARDIOLOGY, "202", false));
    }
}
