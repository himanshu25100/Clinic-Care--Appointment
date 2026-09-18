package com.clinic.service;

import com.clinic.exception.InvalidInputException;
import com.clinic.exception.PatientNotFoundException;
import com.clinic.model.Appointment;
import com.clinic.model.Prescription;
import com.clinic.storage.DataStore;
import com.clinic.util.InputValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MODULE 3 - Medical Records.
 *
 * A doctor writes a prescription against a completed appointment; a returning
 * patient reads back everything ever written for them. This is the module that
 * answers "check my prescriptions from the past".
 *
 * Syllabus concepts: ArrayList, LinkedHashSet, Comparator, string parsing,
 * custom exceptions, interfaces (Comparator is one).
 */
public class RecordService {

    private final DataStore store;
    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public RecordService(DataStore store, PatientService patientService,
                         AppointmentService appointmentService) {
        this.store = store;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    /**
     * Writes a prescription for a finished consultation.
     *
     * @param medicineText lines of the form "Paracetamol 500mg|1-0-1|5", one per
     *                     medicine, separated by ';'
     */
    public Prescription writePrescription(String appointmentId, String diagnosis,
                                          String medicineText, String advice,
                                          String followUpText)
            throws InvalidInputException {

        Appointment appointment = store.findAppointment(appointmentId);
        if (appointment == null) {
            throw new InvalidInputException("Appointment ID",
                    "No appointment found with ID '" + appointmentId + "'.");
        }
        if (appointment.getStatus().name().equals("CANCELLED")) {
            throw new InvalidInputException("Appointment ID",
                    "A cancelled appointment cannot have a prescription.");
        }

        String cleanDiagnosis = InputValidator.requireText("Diagnosis", diagnosis);
        List<Prescription.Medicine> medicines = parseMedicines(medicineText);
        if (medicines.isEmpty()) {
            throw new InvalidInputException("Medicines",
                    "Add at least one medicine as name|dosage|days");
        }

        LocalDate followUp = null;
        if (followUpText != null && !followUpText.trim().isEmpty()
                && !followUpText.trim().equalsIgnoreCase("none")) {
            followUp = InputValidator.parseDate(followUpText);
        }

        Prescription prescription = new Prescription(
                store.nextPrescriptionId(), appointment.getId(),
                appointment.getPatientId(), appointment.getDoctorId(),
                LocalDate.now(), cleanDiagnosis, medicines,
                advice == null ? "" : advice.trim(), followUp);

        store.addPrescription(prescription);

        // a prescription means the patient was actually seen
        appointmentService.markCompleted(appointment.getId());
        return prescription;
    }

    /** Turns "Crocin|1-0-1|5;Zincovit|0-0-1|10" into Medicine objects. */
    public List<Prescription.Medicine> parseMedicines(String text) {
        List<Prescription.Medicine> medicines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return medicines;
        }
        String[] chunks = text.split(";");
        for (String chunk : chunks) {
            if (chunk.trim().isEmpty()) {
                continue;
            }
            Prescription.Medicine m = Prescription.Medicine.unpack(chunk.trim());
            if (m != null) {
                medicines.add(m);
            }
        }
        return medicines;
    }

    /** Full history, newest first - what the patient sees on "My Records". */
    public List<Prescription> historyOf(String patientId)
            throws InvalidInputException, PatientNotFoundException {

        patientService.requirePatient(patientId);   // fails fast on a bad ID

        List<Prescription> history = new ArrayList<>();
        for (Prescription r : store.allPrescriptions()) {
            if (r.getPatientId().equalsIgnoreCase(patientId)) {
                history.add(r);
            }
        }
        // anonymous Comparator: newest prescription first
        Collections.sort(history, new Comparator<Prescription>() {
            @Override
            public int compare(Prescription a, Prescription b) {
                return b.getIssuedOn().compareTo(a.getIssuedOn());
            }
        });
        return history;
    }

    /** Distinct past diagnoses - a quick summary line for the doctor. */
    public Set<String> pastDiagnoses(String patientId)
            throws InvalidInputException, PatientNotFoundException {
        Set<String> found = new LinkedHashSet<>();   // no duplicates, keeps order
        for (Prescription r : historyOf(patientId)) {
            found.add(r.getDiagnosis());
        }
        return found;
    }

    /** Follow-up visits that are due today or later. */
    public List<Prescription> pendingFollowUps() {
        List<Prescription> due = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Prescription r : store.allPrescriptions()) {
            if (r.getFollowUpOn() != null && !r.getFollowUpOn().isBefore(today)) {
                due.add(r);
            }
        }
        return due;
    }

    public Prescription findByAppointment(String appointmentId) {
        for (Prescription r : store.allPrescriptions()) {
            if (r.getAppointmentId().equalsIgnoreCase(appointmentId)) {
                return r;
            }
        }
        return null;
    }
}
