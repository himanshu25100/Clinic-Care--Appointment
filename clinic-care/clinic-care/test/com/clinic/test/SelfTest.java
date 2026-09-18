package com.clinic.test;

import com.clinic.exception.ClinicException;
import com.clinic.exception.InvalidInputException;
import com.clinic.exception.PatientNotFoundException;
import com.clinic.exception.SlotUnavailableException;
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
import com.clinic.util.InputValidator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A hand written test harness - no JUnit, no downloads, nothing but the JDK.
 *
 * Run it with:
 *   java -cp out com.clinic.test.SelfTest
 *
 * It exercises validation, the booking rules, the concurrency guarantee,
 * the record module and CSV round-tripping, then prints a PASS/FAIL summary
 * and exits with code 1 if anything failed (useful for CI).
 */
public class SelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("  ClinicCare self tests");
        System.out.println("======================================================");

        testValidation();
        testRegistration();
        testBookingRules();
        testConcurrentBooking();
        testCancelFreesSlot();
        testRecordsModule();
        testReportsAndRecursion();
        testCsvRoundTrip();

        System.out.println("------------------------------------------------------");
        System.out.println("  PASSED: " + passed + "    FAILED: " + failed);
        System.out.println("======================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Tiny assertion helpers
    // ------------------------------------------------------------------

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("-- " + title);
    }

    /** A weekday at least one day ahead, since the clinic shuts on Sunday. */
    private static LocalDate nextWorkingDay(int daysAhead) {
        LocalDate d = LocalDate.now().plusDays(daysAhead);
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    private static void freshClinic() {
        DataStore store = DataStore.getInstance();
        store.clearAll();
        store.seedDoctorsIfEmpty();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    private static void testValidation() {
        section("Input validation");

        try {
            InputValidator.validatePhone("12345");
            check("short phone is rejected", false);
        } catch (InvalidInputException ex) {
            check("short phone is rejected", true);
        }

        try {
            InputValidator.validatePhone("1234567890");
            check("phone starting with 1 is rejected", false);
        } catch (InvalidInputException ex) {
            check("phone starting with 1 is rejected", true);
        }

        try {
            check("valid phone passes", "9876543210"
                    .equals(InputValidator.validatePhone("98765 43210")));
        } catch (InvalidInputException ex) {
            check("valid phone passes", false);
        }

        try {
            InputValidator.validateAge("200");
            check("age 200 is rejected", false);
        } catch (InvalidInputException ex) {
            check("age 200 is rejected", true);
        }

        try {
            InputValidator.validateBloodGroup("XY+");
            check("nonsense blood group is rejected", false);
        } catch (InvalidInputException ex) {
            check("nonsense blood group is rejected", true);
        }

        try {
            InputValidator.validateFutureDate("01-01-2020");
            check("past date is rejected", false);
        } catch (InvalidInputException ex) {
            check("past date is rejected", true);
        }
    }

    private static void testRegistration() {
        section("Patient registration");
        freshClinic();
        PatientService service = new PatientService(DataStore.getInstance());

        try {
            Patient p = service.register("Ramesh Kumar", "9876543210", "34", "M", "B+");
            check("patient gets an ID", p.getId().startsWith("P-"));
            check("patient is retrievable", service.requirePatient(p.getId()) != null);

            try {
                service.register("Someone Else", "9876543210", "22", "F", "O+");
                check("duplicate phone is rejected", false);
            } catch (InvalidInputException ex) {
                check("duplicate phone is rejected", true);
            }

            try {
                service.requirePatient("P-9999");
                check("unknown ID raises PatientNotFoundException", false);
            } catch (PatientNotFoundException ex) {
                check("unknown ID raises PatientNotFoundException", true);
            }

            service.register("Gita Devi", "9812345678", "67", "F", "A-");
            check("senior citizen counted by recursion",
                    service.countSeniorCitizens() == 1);

        } catch (ClinicException ex) {
            check("registration ran without unexpected errors: " + ex.getMessage(), false);
        }
    }

    private static void testBookingRules() {
        section("Booking rules");
        freshClinic();
        DataStore store = DataStore.getInstance();
        PatientService patients = new PatientService(store);
        AppointmentService appointments = new AppointmentService(store, patients);

        try {
            Patient p = patients.register("Asha Verma", "9900112233", "29", "F", "O+");
            LocalDate day = nextWorkingDay(1);

            Appointment a = appointments.book(p.getId(), "D-01", day, 2, "Fever");
            check("appointment is created", a != null && a.getId().startsWith("A-"));
            check("appointment starts as BOOKED",
                    a.getStatus() == AppointmentStatus.BOOKED);
            check("fee is the specialization base fee", a.getFeeCharged() == 300.0);

            boolean[] free = appointments.freeSlots("D-01", day);
            check("booked slot is no longer free", !free[2]);
            check("other slots stay free", free[3]);

            try {
                Patient q = patients.register("Nikhil Jain", "9900112244", "31", "M", "A+");
                appointments.book(q.getId(), "D-01", day, 2, "Cough");
                check("double booking is blocked", false);
            } catch (SlotUnavailableException ex) {
                check("double booking is blocked", true);
            }

            try {
                appointments.book(p.getId(), "D-01", LocalDate.now().minusDays(3), 1, "x");
                check("booking in the past is blocked", false);
            } catch (ClinicException ex) {
                check("booking in the past is blocked", true);
            }

            try {
                appointments.book(p.getId(), "D-99", day, 4, "x");
                check("unknown doctor is blocked", false);
            } catch (ClinicException ex) {
                check("unknown doctor is blocked", true);
            }

            // Sunday closure
            LocalDate sunday = LocalDate.now().plusDays(1);
            while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
                sunday = sunday.plusDays(1);
            }
            try {
                appointments.book(p.getId(), "D-01", sunday, 1, "x");
                check("Sunday booking is blocked", false);
            } catch (SlotUnavailableException ex) {
                check("Sunday booking is blocked", true);
            }

            boolean[][] grid = appointments.weeklyGrid("D-01", LocalDate.now());
            check("weekly grid is 7 x " + Doctor.SLOTS_PER_DAY,
                    grid.length == 7 && grid[0].length == Doctor.SLOTS_PER_DAY);

            // senior discount
            Patient senior = patients.register("Hari Prasad", "9811122233", "70", "M", "B+");
            Appointment sa = appointments.book(senior.getId(), "D-05", day, 0, "Check-up");
            check("senior citizen pays 20% less", sa.getFeeCharged() == 640.0);

        } catch (ClinicException ex) {
            check("booking ran without unexpected errors: " + ex.getMessage(), false);
        }
    }

    /**
     * The important one: 25 threads race for the same slot.
     * Exactly one must win, or the synchronized block is not doing its job.
     */
    private static void testConcurrentBooking() {
        section("Concurrency - 25 threads fight over one slot");
        freshClinic();
        DataStore store = DataStore.getInstance();
        PatientService patients = new PatientService(store);
        final AppointmentService appointments = new AppointmentService(store, patients);

        try {
            final LocalDate day = nextWorkingDay(2);
            final AtomicInteger wins = new AtomicInteger(0);
            final AtomicInteger rejections = new AtomicInteger(0);

            List<Thread> threads = new ArrayList<>();
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < 25; i++) {
                Patient p = patients.register("Racer " + (char) ('A' + i),
                        "98000000" + String.format("%02d", i), "30", "M", "O+");
                ids.add(p.getId());
            }

            for (int i = 0; i < 25; i++) {
                final String patientId = ids.get(i);
                Thread t = new Thread(() -> {
                    try {
                        appointments.book(patientId, "D-02", day, 5, "Race test");
                        wins.incrementAndGet();
                    } catch (SlotUnavailableException ex) {
                        rejections.incrementAndGet();
                    } catch (ClinicException ex) {
                        // any other failure is a real bug
                    }
                }, "racer-" + i);
                threads.add(t);
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();       // wait for all of them to finish
            }

            check("exactly one thread booked the slot (got " + wins.get() + ")",
                    wins.get() == 1);
            check("the other 24 were rejected cleanly (got " + rejections.get() + ")",
                    rejections.get() == 24);

            boolean[] free = appointments.freeSlots("D-02", day);
            check("slot 6 is marked taken afterwards", !free[5]);

        } catch (Exception ex) {
            check("concurrency test ran: " + ex.getMessage(), false);
        }
    }

    private static void testCancelFreesSlot() {
        section("Cancellation");
        freshClinic();
        DataStore store = DataStore.getInstance();
        PatientService patients = new PatientService(store);
        AppointmentService appointments = new AppointmentService(store, patients);

        try {
            Patient p = patients.register("Divya Nair", "9700112233", "40", "F", "AB+");
            LocalDate day = nextWorkingDay(3);
            Appointment a = appointments.book(p.getId(), "D-03", day, 1, "Rash");

            appointments.cancel(a.getId(), p.getId());
            check("status becomes CANCELLED",
                    a.getStatus() == AppointmentStatus.CANCELLED);
            check("cancelled visit is not billed", a.getFeeCharged() == 0.0);
            check("slot is free again", appointments.freeSlots("D-03", day)[1]);

            try {
                appointments.cancel(a.getId(), p.getId());
                check("cancelling twice is refused", false);
            } catch (ClinicException ex) {
                check("cancelling twice is refused", true);
            }

            Patient other = patients.register("Imposter Lal", "9700112244", "40", "M", "AB+");
            Appointment b = appointments.book(p.getId(), "D-03", day, 4, "Rash");
            try {
                appointments.cancel(b.getId(), other.getId());
                check("cannot cancel someone else's appointment", false);
            } catch (InvalidInputException ex) {
                check("cannot cancel someone else's appointment", true);
            }

            Appointment moved = appointments.reschedule(b.getId(), p.getId(),
                    nextWorkingDay(4), 6);
            check("reschedule produces a new booking",
                    moved.getStatus() == AppointmentStatus.BOOKED);
            check("old booking is cancelled after reschedule",
                    b.getStatus() == AppointmentStatus.CANCELLED);

        } catch (ClinicException ex) {
            check("cancellation ran without unexpected errors: " + ex.getMessage(), false);
        }
    }

    private static void testRecordsModule() {
        section("Medical records");
        freshClinic();
        DataStore store = DataStore.getInstance();
        PatientService patients = new PatientService(store);
        AppointmentService appointments = new AppointmentService(store, patients);
        RecordService records = new RecordService(store, patients, appointments);

        try {
            Patient p = patients.register("Sameer Bose", "9600112233", "45", "M", "O-");
            Appointment a = appointments.book(p.getId(), "D-01", nextWorkingDay(1), 3,
                    "Persistent cough");

            Prescription r = records.writePrescription(a.getId(), "Bronchitis",
                    "Azithromycin 500|1-0-0|3;Cough Syrup|0-0-1|5",
                    "Drink warm water", "none");

            check("prescription is created", r.getId().startsWith("R-"));
            check("two medicines were parsed", r.getMedicines().size() == 2);
            check("dosage survived parsing",
                    "1-0-0".equals(r.getMedicines().get(0).getDosage()));
            check("appointment is auto-completed",
                    a.getStatus() == AppointmentStatus.COMPLETED);
            check("history has one entry", records.historyOf(p.getId()).size() == 1);
            check("past diagnoses listed",
                    records.pastDiagnoses(p.getId()).contains("Bronchitis"));
            check("lookup by appointment works",
                    records.findByAppointment(a.getId()) != null);

            try {
                records.writePrescription(a.getId(), "Anything", "", "", "none");
                check("prescription with no medicine is refused", false);
            } catch (InvalidInputException ex) {
                check("prescription with no medicine is refused", true);
            }

        } catch (ClinicException ex) {
            check("records ran without unexpected errors: " + ex.getMessage(), false);
        }
    }

    private static void testReportsAndRecursion() {
        section("Reports");
        freshClinic();
        DataStore store = DataStore.getInstance();
        PatientService patients = new PatientService(store);
        AppointmentService appointments = new AppointmentService(store, patients);
        ReportService reports = new ReportService(store, appointments);

        try {
            Patient p = patients.register("Latha Menon", "9500112233", "38", "F", "B-");
            appointments.book(p.getId(), "D-01", nextWorkingDay(1), 0, "a");
            appointments.book(p.getId(), "D-01", nextWorkingDay(2), 1, "b");
            appointments.book(p.getId(), "D-04", nextWorkingDay(1), 2, "c");

            check("counts per doctor are right",
                    reports.appointmentsPerDoctor().get("D-01") == 2);
            check("busiest doctor is D-01", "D-01".equals(reports.busiestDoctorId()));

            // recursion must agree with a plain loop
            double loopTotal = 0.0;
            for (Appointment a : store.allAppointments()) {
                loopTotal += a.getFeeCharged();
            }
            check("recursive revenue equals loop revenue",
                    Math.abs(reports.totalRevenue() - loopTotal) < 0.001);

            int[][] load = reports.weeklyLoadGrid(LocalDate.now());
            check("load grid is 7 rows", load.length == 7);
            check("age distribution has 5 bands",
                    reports.ageDistribution().size() == 5);

        } catch (ClinicException ex) {
            check("reports ran without unexpected errors: " + ex.getMessage(), false);
        }
    }

    private static void testCsvRoundTrip() {
        section("CSV serialisation");

        Patient original = new Patient("P-1234", "Test User", "9876543210", 33,
                "M", "B+", LocalDate.of(2024, 5, 20));
        Patient copy = Patient.fromCsv(original.toCsv());
        check("patient survives a CSV round trip",
                copy != null && copy.getId().equals("P-1234")
                        && copy.getAge() == 33
                        && copy.getRegisteredOn().equals(LocalDate.of(2024, 5, 20)));

        check("corrupt patient row returns null instead of crashing",
                Patient.fromCsv("P-1,broken,row") == null);

        Doctor d = new Doctor("D-09", "Dr. Test", "9811100099",
                com.clinic.model.Specialization.CARDIOLOGY, "301", true);
        Doctor dCopy = Doctor.fromCsv(d.toCsv());
        check("doctor survives a CSV round trip",
                dCopy != null && dCopy.isOnLeave()
                        && dCopy.getConsultationFee() == 800.0);

        Prescription.Medicine m = Prescription.Medicine.unpack("Crocin|1-0-1|5");
        check("medicine unpacks correctly",
                m != null && m.getDays() == 5 && "Crocin".equals(m.getName()));
    }
}
