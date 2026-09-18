package com.clinic.ui;

import com.clinic.concurrent.AutoSaveService;
import com.clinic.concurrent.ReminderService;
import com.clinic.exception.ClinicException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Prescription;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.RecordService;
import com.clinic.service.ReportService;
import com.clinic.storage.DataStore;
import com.clinic.storage.Repository;
import com.clinic.util.ConsoleUtil;
import com.clinic.util.InputValidator;
import com.clinic.util.ReflectionInspector;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The console front-end. It collects text, calls a service, prints the result
 * or the error - and contains no business rules of its own.
 *
 * Syllabus concepts: switch statements, loops, break / continue,
 * try-catch-finally, polymorphic calls, formatted output.
 */
public class MenuApp {

    private static final String STAFF_PIN = "1234";

    private final DataStore store;
    private final Repository repository;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final RecordService recordService;
    private final ReportService reportService;
    private final ReminderService reminderService;
    private final AutoSaveService autoSaveService;

    public MenuApp(DataStore store, Repository repository,
                   PatientService patientService, AppointmentService appointmentService,
                   RecordService recordService, ReportService reportService,
                   ReminderService reminderService, AutoSaveService autoSaveService) {
        this.store = store;
        this.repository = repository;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.recordService = recordService;
        this.reportService = reportService;
        this.reminderService = reminderService;
        this.autoSaveService = autoSaveService;
    }

    // ==================================================================
    // Main loop
    // ==================================================================

    public void start() {
        boolean keepRunning = true;
        while (keepRunning) {
            showReminders();
            ConsoleUtil.header("ClinicCare - Appointment & Records System");
            ConsoleUtil.info("1. Patient Desk");
            ConsoleUtil.info("2. Doctor / Staff Desk");
            ConsoleUtil.info("3. Reports & Analytics");
            ConsoleUtil.info("4. System Info");
            ConsoleUtil.info("5. Save & Exit");
            ConsoleUtil.line();

            int choice = readChoice(1, 5);
            switch (choice) {
                case 1:
                    patientDesk();
                    break;
                case 2:
                    staffDesk();
                    break;
                case 3:
                    reportsMenu();
                    break;
                case 4:
                    systemInfo();
                    break;
                case 5:
                    keepRunning = false;
                    break;
                default:
                    break;   // readChoice already refused anything else
            }
        }
    }

    private void showReminders() {
        List<String> reminders = reminderService.drainReminders();
        if (reminders.isEmpty()) {
            return;
        }
        ConsoleUtil.subHeader("Reminders from the background service");
        for (String r : reminders) {
            ConsoleUtil.warn(r);
        }
    }

    // ==================================================================
    // MODULE 1 - Patient Desk
    // ==================================================================

    private void patientDesk() {
        ConsoleUtil.header("Patient Desk");
        ConsoleUtil.info("1. I am a NEW patient (register)");
        ConsoleUtil.info("2. I already have a Patient ID");
        ConsoleUtil.info("3. I forgot my ID (search by name)");
        ConsoleUtil.info("4. Back");
        ConsoleUtil.line();

        int choice = readChoice(1, 4);
        Patient patient = null;

        try {
            switch (choice) {
                case 1:
                    patient = registerNewPatient();
                    break;
                case 2:
                    patient = patientService.requirePatient(
                            ConsoleUtil.ask("Patient ID (e.g. P-1001)"));
                    break;
                case 3:
                    searchPatients();
                    return;
                default:
                    return;
            }
        } catch (ClinicException ex) {
            ConsoleUtil.error(ex.displayMessage());
            ConsoleUtil.pause();
            return;
        }

        if (patient != null) {
            patientMenu(patient);
        }
    }

    private Patient registerNewPatient() throws ClinicException {
        ConsoleUtil.subHeader("New Patient Registration");
        Patient patient = patientService.register(
                ConsoleUtil.ask("Full name"),
                ConsoleUtil.ask("Phone (10 digits)"),
                ConsoleUtil.ask("Age"),
                ConsoleUtil.ask("Gender (M/F/O)"),
                ConsoleUtil.ask("Blood group (e.g. B+)"));

        ConsoleUtil.blank();
        ConsoleUtil.success("Registered successfully.");
        ConsoleUtil.field("Your Patient ID", patient.getId());
        ConsoleUtil.info("Please keep this ID - you will need it every visit.");
        ConsoleUtil.pause();
        return patient;
    }

    private void searchPatients() {
        String fragment = ConsoleUtil.ask("Type part of the name");
        List<Patient> matches = patientService.searchByName(fragment);
        ConsoleUtil.subHeader("Matches: " + matches.size());
        for (Patient p : matches) {
            ConsoleUtil.info(p.describe());     // polymorphic call
        }
        if (matches.isEmpty()) {
            ConsoleUtil.warn("Nobody found. Register as a new patient instead.");
        }
        ConsoleUtil.pause();
    }

    private void patientMenu(Patient patient) {
        boolean stay = true;
        while (stay) {
            ConsoleUtil.header("Welcome, " + patient.getName()
                    + "  (" + patient.getId() + ")");
            ConsoleUtil.info("1. Book an appointment");
            ConsoleUtil.info("2. My upcoming appointments");
            ConsoleUtil.info("3. Cancel an appointment");
            ConsoleUtil.info("4. Reschedule an appointment");
            ConsoleUtil.info("5. My past prescriptions");
            ConsoleUtil.info("6. Update my phone number");
            ConsoleUtil.info("7. Log out");
            ConsoleUtil.line();

            int choice = readChoice(1, 7);
            try {
                switch (choice) {
                    case 1:
                        bookAppointment(patient);
                        break;
                    case 2:
                        showUpcoming(patient);
                        break;
                    case 3:
                        cancelAppointment(patient);
                        break;
                    case 4:
                        rescheduleAppointment(patient);
                        break;
                    case 5:
                        showHistory(patient);
                        break;
                    case 6:
                        patientService.updatePhone(patient.getId(),
                                ConsoleUtil.ask("New phone number"));
                        ConsoleUtil.success("Phone number updated.");
                        break;
                    default:
                        stay = false;
                        break;
                }
            } catch (ClinicException ex) {
                ConsoleUtil.error(ex.displayMessage());
            } finally {
                if (stay) {
                    ConsoleUtil.pause();
                }
            }
        }
    }

    // ==================================================================
    // MODULE 2 - Booking
    // ==================================================================

    private void bookAppointment(Patient patient) throws ClinicException {
        ConsoleUtil.subHeader("Available doctors");
        for (Doctor d : store.allDoctors()) {
            ConsoleUtil.info(d.describe());
        }

        String doctorId = InputValidator.validateDoctorId(
                ConsoleUtil.ask("Doctor ID (e.g. D-01)"));
        LocalDate date = InputValidator.validateFutureDate(
                ConsoleUtil.ask("Date (dd-MM-yyyy)"));

        boolean[] free = appointmentService.freeSlots(doctorId, date);
        ConsoleUtil.subHeader("Slots on " + date + " (" + date.getDayOfWeek() + ")");

        boolean anyFree = false;
        for (int i = 0; i < free.length; i++) {
            String state = free[i] ? "FREE" : "booked";
            System.out.printf("  %d. %-16s %s%n", i + 1, Doctor.slotLabel(i), state);
            if (free[i]) {
                anyFree = true;
            }
        }
        if (!anyFree) {
            ConsoleUtil.warn("No slots left that day. Try another date.");
            return;
        }

        int slot = InputValidator.validateMenuChoice(
                ConsoleUtil.ask("Choose a slot number"), 1, Doctor.SLOTS_PER_DAY) - 1;
        String reason = ConsoleUtil.ask("Reason for visit");

        Appointment appointment = appointmentService.book(
                patient.getId(), doctorId, date, slot, reason);

        Doctor doctor = store.findDoctor(doctorId);
        ConsoleUtil.blank();
        ConsoleUtil.success("Appointment confirmed.");
        ConsoleUtil.field("Appointment ID", appointment.getId());
        ConsoleUtil.field("Doctor", doctor.getName());
        ConsoleUtil.field("When", date + "  " + appointment.getSlotLabel());
        ConsoleUtil.field("Fee", "Rs. " + appointment.getFeeCharged()
                + (patient.isSeniorCitizen() ? "  (senior citizen discount applied)" : ""));
    }

    private void showUpcoming(Patient patient) {
        List<Appointment> upcoming = appointmentService.upcomingForPatient(patient.getId());
        ConsoleUtil.subHeader("Upcoming appointments: " + upcoming.size());
        if (upcoming.isEmpty()) {
            ConsoleUtil.info("Nothing booked. Use option 1 to book a visit.");
            return;
        }
        for (Appointment a : upcoming) {
            Doctor d = store.findDoctor(a.getDoctorId());
            System.out.printf("  %-8s %s  %-16s %-20s %s%n",
                    a.getId(), a.getDate(), a.getSlotLabel(),
                    d == null ? a.getDoctorId() : d.getName(), a.getReason());
        }
    }

    private void cancelAppointment(Patient patient) throws ClinicException {
        showUpcoming(patient);
        String id = ConsoleUtil.ask("Appointment ID to cancel");
        appointmentService.cancel(id, patient.getId());
        ConsoleUtil.success("Appointment " + id.toUpperCase()
                + " cancelled and the slot is free again.");
    }

    private void rescheduleAppointment(Patient patient) throws ClinicException {
        showUpcoming(patient);
        String id = ConsoleUtil.ask("Appointment ID to move");
        LocalDate date = InputValidator.validateFutureDate(
                ConsoleUtil.ask("New date (dd-MM-yyyy)"));

        Appointment old = store.findAppointment(id);
        if (old == null) {
            ConsoleUtil.error("No appointment with that ID.");
            return;
        }
        boolean[] free = appointmentService.freeSlots(old.getDoctorId(), date);
        for (int i = 0; i < free.length; i++) {
            System.out.printf("  %d. %-16s %s%n",
                    i + 1, Doctor.slotLabel(i), free[i] ? "FREE" : "booked");
        }
        int slot = InputValidator.validateMenuChoice(
                ConsoleUtil.ask("Choose a slot number"), 1, Doctor.SLOTS_PER_DAY) - 1;

        Appointment fresh = appointmentService.reschedule(id, patient.getId(), date, slot);
        ConsoleUtil.success("Moved to " + fresh.getDate() + " "
                + fresh.getSlotLabel() + ". New ID: " + fresh.getId());
    }

    // ==================================================================
    // MODULE 3 - Records
    // ==================================================================

    private void showHistory(Patient patient) throws ClinicException {
        List<Prescription> history = recordService.historyOf(patient.getId());
        ConsoleUtil.subHeader("Medical history: " + history.size() + " prescription(s)");

        if (history.isEmpty()) {
            ConsoleUtil.info("No prescriptions on record yet.");
            return;
        }

        Set<String> diagnoses = recordService.pastDiagnoses(patient.getId());
        ConsoleUtil.field("Past conditions", String.join(", ", diagnoses));
        ConsoleUtil.blank();

        for (Prescription r : history) {
            Doctor d = store.findDoctor(r.getDoctorId());
            ConsoleUtil.line();
            ConsoleUtil.field("Prescription", r.getId() + "   dated " + r.getIssuedOn());
            ConsoleUtil.field("Doctor", d == null ? r.getDoctorId() : d.getName());
            ConsoleUtil.field("Diagnosis", r.getDiagnosis());
            ConsoleUtil.info("  Medicines:");
            for (Prescription.Medicine m : r.getMedicines()) {
                ConsoleUtil.info("    - " + m);
            }
            if (!r.getAdvice().isEmpty()) {
                ConsoleUtil.field("Advice", r.getAdvice());
            }
            if (r.getFollowUpOn() != null) {
                ConsoleUtil.field("Follow-up on", r.getFollowUpOn());
            }
        }
    }

    // ==================================================================
    // Doctor / Staff desk
    // ==================================================================

    private void staffDesk() {
        String pin = ConsoleUtil.ask("Staff PIN");
        if (!STAFF_PIN.equals(pin.trim())) {
            ConsoleUtil.error("Wrong PIN. Access denied.");
            ConsoleUtil.pause();
            return;
        }

        boolean stay = true;
        while (stay) {
            ConsoleUtil.header("Doctor / Staff Desk");
            ConsoleUtil.info("1. Day schedule for a doctor");
            ConsoleUtil.info("2. Write a prescription");
            ConsoleUtil.info("3. Mark a patient as no-show");
            ConsoleUtil.info("4. Open a patient's file");
            ConsoleUtil.info("5. 7-day availability grid");
            ConsoleUtil.info("6. Back");
            ConsoleUtil.line();

            int choice = readChoice(1, 6);
            try {
                switch (choice) {
                    case 1:
                        daySchedule();
                        break;
                    case 2:
                        writePrescription();
                        break;
                    case 3:
                        markNoShow();
                        break;
                    case 4:
                        openPatientFile();
                        break;
                    case 5:
                        availabilityGrid();
                        break;
                    default:
                        stay = false;
                        break;
                }
            } catch (ClinicException ex) {
                ConsoleUtil.error(ex.displayMessage());
            } finally {
                if (stay) {
                    ConsoleUtil.pause();
                }
            }
        }
    }

    private void daySchedule() throws ClinicException {
        String doctorId = InputValidator.validateDoctorId(ConsoleUtil.ask("Doctor ID"));
        LocalDate date = InputValidator.parseDate(
                ConsoleUtil.ask("Date (dd-MM-yyyy)"));

        List<Appointment> list = appointmentService.forDoctorOnDate(doctorId, date);
        ConsoleUtil.subHeader("Schedule for " + doctorId + " on " + date
                + " - " + list.size() + " patient(s)");
        for (Appointment a : list) {
            Patient p = store.findPatient(a.getPatientId());
            System.out.printf("  %-16s %-8s %-22s %s%n",
                    a.getSlotLabel(), a.getPatientId(),
                    p == null ? "?" : p.getName(), a.getReason());
        }
        if (list.isEmpty()) {
            ConsoleUtil.info("No patients booked.");
        }
    }

    private void writePrescription() throws ClinicException {
        ConsoleUtil.subHeader("Write a prescription");
        ConsoleUtil.info("Medicine format:  name|dosage|days");
        ConsoleUtil.info("Several medicines:  Crocin|1-0-1|5;Zincovit|0-0-1|10");

        String appointmentId = ConsoleUtil.ask("Appointment ID");
        Prescription existing = recordService.findByAppointment(appointmentId);
        if (existing != null) {
            ConsoleUtil.warn("This visit already has prescription " + existing.getId() + ".");
            return;
        }

        Prescription r = recordService.writePrescription(
                appointmentId,
                ConsoleUtil.ask("Diagnosis"),
                ConsoleUtil.ask("Medicines"),
                ConsoleUtil.ask("Advice (optional)"),
                ConsoleUtil.ask("Follow-up date dd-MM-yyyy or 'none'"));

        ConsoleUtil.success("Saved as " + r.getId()
                + " and the appointment is marked COMPLETED.");
    }

    private void markNoShow() throws ClinicException {
        String id = ConsoleUtil.ask("Appointment ID");
        Appointment a = store.findAppointment(id);
        if (a == null) {
            ConsoleUtil.error("No appointment with that ID.");
            return;
        }
        if (!a.getStatus().canMoveTo(AppointmentStatus.NO_SHOW)) {
            ConsoleUtil.error("Appointment is " + a.getStatus() + ", cannot change it.");
            return;
        }
        a.setStatus(AppointmentStatus.NO_SHOW);
        ConsoleUtil.success("Marked as NO_SHOW.");
    }

    private void openPatientFile() throws ClinicException {
        Patient p = patientService.requirePatient(ConsoleUtil.ask("Patient ID"));
        ConsoleUtil.subHeader("File: " + p.getName());
        ConsoleUtil.field("ID", p.getId());
        ConsoleUtil.field("Age / Gender", p.getAge() + " / " + p.getGender());
        ConsoleUtil.field("Blood group", p.getBloodGroup());
        ConsoleUtil.field("Phone", p.getPhone());
        ConsoleUtil.field("Registered on", p.getRegisteredOn());

        List<Appointment> all = appointmentService.forPatient(p.getId());
        ConsoleUtil.subHeader("Visits: " + all.size());
        for (Appointment a : all) {
            ConsoleUtil.info(a.toString());
        }
        showHistory(p);
    }

    private void availabilityGrid() throws ClinicException {
        String doctorId = InputValidator.validateDoctorId(ConsoleUtil.ask("Doctor ID"));
        LocalDate start = LocalDate.now();
        boolean[][] grid = appointmentService.weeklyGrid(doctorId, start);

        ConsoleUtil.subHeader("Next 7 days for " + doctorId
                + "   ( . = free,  X = booked,  - = closed )");
        System.out.print("  Date        ");
        for (int slot = 0; slot < Doctor.SLOTS_PER_DAY; slot++) {
            System.out.printf("S%-2d", slot + 1);
        }
        System.out.println();

        Doctor doctor = store.findDoctor(doctorId);
        for (int day = 0; day < grid.length; day++) {
            LocalDate date = start.plusDays(day);
            boolean closed = doctor != null && !doctor.worksOn(date);
            System.out.printf("  %-12s", date.toString());
            for (int slot = 0; slot < grid[day].length; slot++) {
                char mark = closed ? '-' : (grid[day][slot] ? '.' : 'X');
                System.out.print(" " + mark + " ");
            }
            System.out.println();
        }
    }

    // ==================================================================
    // MODULE 4 - Reports
    // ==================================================================

    private void reportsMenu() {
        boolean stay = true;
        while (stay) {
            ConsoleUtil.header("Reports & Analytics");
            ConsoleUtil.info("1. Appointments per doctor");
            ConsoleUtil.info("2. Appointment status breakdown");
            ConsoleUtil.info("3. Revenue summary");
            ConsoleUtil.info("4. Patient age distribution");
            ConsoleUtil.info("5. Most common diagnoses");
            ConsoleUtil.info("6. Clinic load for the next 7 days");
            ConsoleUtil.info("7. Back");
            ConsoleUtil.line();

            int choice = readChoice(1, 7);
            switch (choice) {
                case 1:
                    reportPerDoctor();
                    break;
                case 2:
                    reportStatus();
                    break;
                case 3:
                    reportRevenue();
                    break;
                case 4:
                    reportAges();
                    break;
                case 5:
                    reportDiagnoses();
                    break;
                case 6:
                    reportLoad();
                    break;
                default:
                    stay = false;
                    break;
            }
            if (stay) {
                ConsoleUtil.pause();
            }
        }
    }

    private void reportPerDoctor() {
        ConsoleUtil.subHeader("Appointments per doctor");
        Map<String, Integer> counts = reportService.appointmentsPerDoctor();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Doctor d = store.findDoctor(e.getKey());
            String name = d == null ? e.getKey() : d.getName();
            System.out.printf("  %-22s %3d  %s%n", name, e.getValue(),
                    ConsoleUtil.repeat('#', e.getValue()));
        }
        String busiest = reportService.busiestDoctorId();
        if (busiest != null) {
            Doctor d = store.findDoctor(busiest);
            ConsoleUtil.field("Busiest", d == null ? busiest : d.getName());
        }
    }

    private void reportStatus() {
        ConsoleUtil.subHeader("Status breakdown");
        Map<AppointmentStatus, Integer> counts = reportService.statusBreakdown();
        for (Map.Entry<AppointmentStatus, Integer> e : counts.entrySet()) {
            System.out.printf("  %-12s %3d%n", e.getKey(), e.getValue());
        }
    }

    private void reportRevenue() {
        ConsoleUtil.subHeader("Revenue");
        ConsoleUtil.field("Total billed", "Rs. " + reportService.totalRevenue());
        ConsoleUtil.field("Registered patients", store.patientCount());
        ConsoleUtil.field("Senior citizens", patientService.countSeniorCitizens());
    }

    private void reportAges() {
        ConsoleUtil.subHeader("Patient age distribution");
        for (Map.Entry<String, Integer> e : reportService.ageDistribution().entrySet()) {
            System.out.printf("  %-8s %3d  %s%n", e.getKey(), e.getValue(),
                    ConsoleUtil.repeat('*', e.getValue()));
        }
    }

    private void reportDiagnoses() {
        ConsoleUtil.subHeader("Most common diagnoses");
        List<String> top = reportService.topDiagnoses(5);
        if (top.isEmpty()) {
            ConsoleUtil.info("No prescriptions recorded yet.");
            return;
        }
        for (String s : top) {
            ConsoleUtil.info("- " + s);
        }
    }

    private void reportLoad() {
        ConsoleUtil.subHeader("Clinic load, next 7 days (patients booked per slot)");
        int[][] grid = reportService.weeklyLoadGrid(LocalDate.now());
        System.out.print("  Date        ");
        for (int slot = 0; slot < Doctor.SLOTS_PER_DAY; slot++) {
            System.out.printf("S%-2d", slot + 1);
        }
        System.out.println();
        for (int day = 0; day < grid.length; day++) {
            System.out.printf("  %-12s", LocalDate.now().plusDays(day).toString());
            for (int slot = 0; slot < grid[day].length; slot++) {
                System.out.printf(" %d ", grid[day][slot]);
            }
            System.out.println();
        }
    }

    // ==================================================================
    // System info
    // ==================================================================

    private void systemInfo() {
        ConsoleUtil.header("System Info");
        ConsoleUtil.field("Java version", System.getProperty("java.version"));
        ConsoleUtil.field("Storage back-end", repository.backendName());
        ConsoleUtil.field("Unsaved changes", store.isDirty() ? "yes" : "no");
        ConsoleUtil.field("Auto-saves done", autoSaveService.getSaveCount());
        ConsoleUtil.field("Auto-save thread", autoSaveService.getState()
                + (autoSaveService.isDaemon() ? " (daemon)" : ""));
        ConsoleUtil.field("Reminder scans", reminderService.getScanCount());
        if (autoSaveService.getLastError() != null) {
            ConsoleUtil.error("Last save error: " + autoSaveService.getLastError());
        }

        ConsoleUtil.subHeader("Data dictionary (built at runtime by reflection)");
        ReflectionInspector.describe("com.clinic.model.Patient");
        ReflectionInspector.describe("com.clinic.model.Appointment");
        ConsoleUtil.pause();
    }

    // ==================================================================
    // Helper
    // ==================================================================

    /** Keeps asking until the user types a number inside the allowed range. */
    private int readChoice(int min, int max) {
        while (true) {
            try {
                return InputValidator.validateMenuChoice(
                        ConsoleUtil.ask("Choice"), min, max);
            } catch (ClinicException ex) {
                ConsoleUtil.error(ex.getMessage());
            }
        }
    }
}
