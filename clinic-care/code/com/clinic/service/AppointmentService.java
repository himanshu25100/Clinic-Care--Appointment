package com.clinic.service;

import com.clinic.exception.DoctorNotFoundException;
import com.clinic.exception.InvalidInputException;
import com.clinic.exception.PatientNotFoundException;
import com.clinic.exception.SlotUnavailableException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.storage.DataStore;
import com.clinic.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MODULE 2 - Appointment Scheduling. The heart of the system.
 *
 * A consulting slot is a shared resource: two receptionists booking the same
 * 10:00 slot for Dr. Rao at the same instant must not both succeed. The
 * check-then-book sequence is therefore guarded by a single lock object, which
 * makes the whole operation atomic.
 *
 * Syllabus concepts: synchronized blocks, shared mutable state, arrays and
 * 2-D arrays, enums, collections, custom exceptions, Collections.sort.
 */
public class AppointmentService {

    /**
     * One dedicated lock for all booking operations. Using a private object
     * rather than 'this' means no outside code can accidentally hold our lock.
     */
    private final Object bookingLock = new Object();

    private final DataStore store;
    private final PatientService patientService;

    public AppointmentService(DataStore store, PatientService patientService) {
        this.store = store;
        this.patientService = patientService;
    }

    // ------------------------------------------------------------------
    // Availability
    // ------------------------------------------------------------------

    /**
     * Returns an array of length SLOTS_PER_DAY where true means "free".
     * Built by scanning live bookings, so it can never fall out of sync.
     */
    public boolean[] freeSlots(String doctorId, LocalDate date)
            throws DoctorNotFoundException {

        Doctor doctor = store.findDoctor(doctorId);
        if (doctor == null) {
            throw new DoctorNotFoundException(doctorId);
        }

        boolean[] free = new boolean[Doctor.SLOTS_PER_DAY];
        if (!doctor.worksOn(date)) {
            return free;   // every entry stays false - the doctor is unavailable
        }
        // start with everything open
        for (int i = 0; i < free.length; i++) {
            free[i] = true;
        }
        // close whatever is already booked
        for (Appointment a : store.allAppointments()) {
            if (a.isActive()
                    && a.getDoctorId().equals(doctor.getId())
                    && a.getDate().equals(date)) {
                int slot = a.getSlotIndex();
                if (slot >= 0 && slot < free.length) {
                    free[slot] = false;
                }
            }
        }
        return free;
    }

    /**
     * A 2-D array view of one doctor's next seven days:
     * grid[day][slot] == true means that slot is free.
     * Rows are days, columns are the eight consulting slots.
     */
    public boolean[][] weeklyGrid(String doctorId, LocalDate startDate)
            throws DoctorNotFoundException {

        boolean[][] grid = new boolean[7][Doctor.SLOTS_PER_DAY];
        for (int day = 0; day < 7; day++) {
            boolean[] daySlots = freeSlots(doctorId, startDate.plusDays(day));
            // copy the row into the 2-D array
            for (int slot = 0; slot < Doctor.SLOTS_PER_DAY; slot++) {
                grid[day][slot] = daySlots[slot];
            }
        }
        return grid;
    }

    // ------------------------------------------------------------------
    // Booking
    // ------------------------------------------------------------------

    /**
     * Books one slot. Either the appointment is created and the slot is taken,
     * or an exception explains exactly why it could not happen.
     */
    public Appointment book(String patientId, String doctorId, LocalDate date,
                            int slotIndex, String reason)
            throws InvalidInputException, PatientNotFoundException,
            DoctorNotFoundException, SlotUnavailableException {

        Patient patient = patientService.requirePatient(patientId);

        String cleanDoctorId = InputValidator.validateDoctorId(doctorId);
        Doctor doctor = store.findDoctor(cleanDoctorId);
        if (doctor == null) {
            throw new DoctorNotFoundException(cleanDoctorId);
        }

        if (slotIndex < 0 || slotIndex >= Doctor.SLOTS_PER_DAY) {
            throw new InvalidInputException("Slot",
                    "Slot must be between 1 and " + Doctor.SLOTS_PER_DAY + ".");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new SlotUnavailableException("That date has already passed.");
        }
        if (!doctor.worksOn(date)) {
            throw new SlotUnavailableException(doctor.getName()
                    + " does not consult on " + date.getDayOfWeek()
                    + (doctor.isOnLeave() ? " (currently on leave)." : "."));
        }

        String cleanReason = InputValidator.requireText("Reason", reason);

        // ---- critical section: check and book must not be interrupted ----
        synchronized (bookingLock) {

            for (Appointment a : store.allAppointments()) {
                if (!a.isActive() || !a.getDate().equals(date)) {
                    continue;
                }
                // is this doctor's slot already taken?
                if (a.getDoctorId().equals(doctor.getId())
                        && a.getSlotIndex() == slotIndex) {
                    throw new SlotUnavailableException("Slot "
                            + Doctor.slotLabel(slotIndex) + " with "
                            + doctor.getName() + " on " + date + " is already booked.");
                }
                // can the patient be in two rooms at once? no.
                if (a.getPatientId().equals(patient.getId())
                        && a.getSlotIndex() == slotIndex) {
                    throw new SlotUnavailableException(
                            "You already have another appointment at "
                                    + Doctor.slotLabel(slotIndex) + " on " + date + ".");
                }
            }

            double fee = calculateFee(doctor, patient);
            Appointment appointment = new Appointment(
                    store.nextAppointmentId(), patient.getId(), doctor.getId(),
                    date, slotIndex, cleanReason, AppointmentStatus.BOOKED,
                    LocalDateTime.now(), fee);
            store.addAppointment(appointment);
            return appointment;
        }
        // ---- end critical section ----
    }

    /** Senior citizens pay 20% less; this is the clinic's standing policy. */
    public double calculateFee(Doctor doctor, Patient patient) {
        double fee = doctor.getConsultationFee();
        if (patient.isSeniorCitizen()) {
            fee = fee * 0.80;
        }
        return Math.round(fee * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------
    // Changing an existing appointment
    // ------------------------------------------------------------------

    public void cancel(String appointmentId, String patientId)
            throws InvalidInputException, SlotUnavailableException {

        Appointment appointment = store.findAppointment(appointmentId);
        if (appointment == null) {
            throw new InvalidInputException("Appointment ID",
                    "No appointment found with ID '" + appointmentId + "'.");
        }
        // a patient may only cancel their own booking
        if (!appointment.getPatientId().equalsIgnoreCase(patientId)) {
            throw new InvalidInputException("Appointment ID",
                    "That appointment does not belong to you.");
        }
        if (!appointment.getStatus().canMoveTo(AppointmentStatus.CANCELLED)) {
            throw new SlotUnavailableException("This appointment is already "
                    + appointment.getStatus() + " and cannot be cancelled.");
        }
        synchronized (bookingLock) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setFeeCharged(0.0);
        }
    }

    /** Cancels the old slot and books a new one in a single guarded step. */
    public Appointment reschedule(String appointmentId, String patientId,
                                  LocalDate newDate, int newSlot)
            throws InvalidInputException, PatientNotFoundException,
            DoctorNotFoundException, SlotUnavailableException {

        Appointment old = store.findAppointment(appointmentId);
        if (old == null) {
            throw new InvalidInputException("Appointment ID",
                    "No appointment found with ID '" + appointmentId + "'.");
        }
        if (!old.getPatientId().equalsIgnoreCase(patientId)) {
            throw new InvalidInputException("Appointment ID",
                    "That appointment does not belong to you.");
        }
        if (!old.isActive()) {
            throw new SlotUnavailableException("Only an active booking can be moved.");
        }

        // book the new one first; if it fails, the old booking is untouched
        Appointment fresh = book(old.getPatientId(), old.getDoctorId(),
                newDate, newSlot, old.getReason());
        old.setStatus(AppointmentStatus.CANCELLED);
        old.setFeeCharged(0.0);
        return fresh;
    }

    /** Marks a visit as finished. Called by the doctor before writing a record. */
    public void markCompleted(String appointmentId) throws InvalidInputException {
        Appointment appointment = store.findAppointment(appointmentId);
        if (appointment == null) {
            throw new InvalidInputException("Appointment ID",
                    "No appointment found with ID '" + appointmentId + "'.");
        }
        if (!appointment.getStatus().canMoveTo(AppointmentStatus.COMPLETED)) {
            throw new InvalidInputException("Appointment ID",
                    "Appointment is " + appointment.getStatus() + ", not BOOKED.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<Appointment> forPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : store.allAppointments()) {
            if (a.getPatientId().equalsIgnoreCase(patientId)) {
                result.add(a);
            }
        }
        Collections.sort(result);     // uses Appointment.compareTo
        return result;
    }

    public List<Appointment> upcomingForPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Appointment a : forPatient(patientId)) {
            if (a.isActive() && !a.getDate().isBefore(today)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Appointment> forDoctorOnDate(String doctorId, LocalDate date) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : store.allAppointments()) {
            if (a.getDoctorId().equalsIgnoreCase(doctorId)
                    && a.getDate().equals(date) && a.isActive()) {
                result.add(a);
            }
        }
        Collections.sort(result);
        return result;
    }

    /** Every active booking on a given calendar day, across all doctors. */
    public List<Appointment> onDate(LocalDate date) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : store.allAppointments()) {
            if (a.getDate().equals(date) && a.isActive()) {
                result.add(a);
            }
        }
        Collections.sort(result);
        return result;
    }
}
