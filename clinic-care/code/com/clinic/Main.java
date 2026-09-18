package com.clinic;

import com.clinic.concurrent.AutoSaveService;
import com.clinic.concurrent.ReminderService;
import com.clinic.exception.StorageException;
import com.clinic.gui.GuiApp;
import com.clinic.gui.UiTheme;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.RecordService;
import com.clinic.service.ReportService;
import com.clinic.storage.CsvRepository;
import com.clinic.storage.DataStore;
import com.clinic.storage.Repository;
import com.clinic.ui.MenuApp;
import com.clinic.util.ConsoleUtil;

import javax.swing.SwingUtilities;

/**
 * Entry point of ClinicCare.
 *
 * Responsibilities, in order:
 *   1. choose the storage back-end
 *   2. load saved data
 *   3. build the service objects and hand them their dependencies
 *   4. start the two background threads
 *   5. run the menu
 *   6. save and shut the threads down cleanly
 *
 * Syllabus concepts: main method, object composition, thread start/join,
 * shutdown hook, try-catch-finally.
 */
public class Main {

    private static final long AUTO_SAVE_INTERVAL_MS = 20_000;   // 20 seconds
    private static final long REMINDER_INTERVAL_MS = 30_000;    // 30 seconds

    public static void main(String[] args) {

        // "--console" forces the text menu; anything else opens the Swing GUI
        boolean useConsole = args.length > 0 && "--console".equalsIgnoreCase(args[0]);
        String dataDir = "data";
        for (String a : args) {
            if (!a.startsWith("--")) {
                dataDir = a;
            }
        }

        if (useConsole) {
            runConsole(dataDir);
        } else {
            runGui(dataDir);
        }
    }

    // ------------------------------------------------------------------
    // Swing GUI entry point
    // ------------------------------------------------------------------

    private static void runGui(String dataDir) {
        Repository repository = new CsvRepository(dataDir);
        DataStore store = DataStore.getInstance();

        try {
            repository.loadAll(store);
        } catch (StorageException ex) {
            System.err.println(ex.displayMessage());
            store.seedDoctorsIfEmpty();
        }

        PatientService patientService = new PatientService(store);
        AppointmentService appointmentService = new AppointmentService(store, patientService);
        RecordService recordService = new RecordService(store, patientService, appointmentService);
        ReportService reportService = new ReportService(store, appointmentService);

        ReminderService reminderService = new ReminderService(store, REMINDER_INTERVAL_MS);
        Thread reminderThread = new Thread(reminderService, "reminder-scan");
        reminderThread.setDaemon(true);
        reminderThread.start();

        AutoSaveService autoSaveService =
                new AutoSaveService(store, repository, AUTO_SAVE_INTERVAL_MS);
        autoSaveService.start();

        Runtime.getRuntime().addShutdownHook(new Thread(
                autoSaveService::saveIfDirty, "shutdown-save"));

        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            GuiApp frame = new GuiApp(store, repository, patientService, appointmentService,
                    recordService, reportService, reminderService, autoSaveService);
            frame.setVisible(true);
        });
    }

    // ------------------------------------------------------------------
    // Console entry point (unchanged behaviour, run with --console)
    // ------------------------------------------------------------------

    private static void runConsole(String dataDir) {

        // 1. storage - the only line that would change for a JDBC back-end
        Repository repository = new CsvRepository(dataDir);
        DataStore store = DataStore.getInstance();

        // 2. load
        try {
            repository.loadAll(store);
        } catch (StorageException ex) {
            ConsoleUtil.error(ex.displayMessage());
            ConsoleUtil.warn("Starting with an empty clinic.");
            store.seedDoctorsIfEmpty();
        }

        // 3. services
        PatientService patientService = new PatientService(store);
        AppointmentService appointmentService = new AppointmentService(store, patientService);
        RecordService recordService = new RecordService(store, patientService, appointmentService);
        ReportService reportService = new ReportService(store, appointmentService);

        // 4. background threads
        ReminderService reminderService = new ReminderService(store, REMINDER_INTERVAL_MS);
        Thread reminderThread = new Thread(reminderService, "reminder-scan");
        reminderThread.setDaemon(true);
        reminderThread.start();

        AutoSaveService autoSaveService =
                new AutoSaveService(store, repository, AUTO_SAVE_INTERVAL_MS);
        autoSaveService.start();

        // last line of defence: save even if the user kills the program
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            autoSaveService.saveIfDirty();
        }, "shutdown-save"));

        ConsoleUtil.header("ClinicCare v1.0");
        ConsoleUtil.info("Loaded " + store.patientCount() + " patient(s), "
                + store.allDoctors().size() + " doctor(s), "
                + store.allAppointments().size() + " appointment(s).");
        ConsoleUtil.info("Storage: " + repository.backendName());

        MenuApp app = new MenuApp(store, repository, patientService, appointmentService,
                recordService, reportService, reminderService, autoSaveService);

        try {
            app.start();
        } finally {
            // 6. clean shutdown, whatever happened above
            ConsoleUtil.blank();
            ConsoleUtil.info("Saving data...");
            boolean saved = autoSaveService.saveIfDirty();
            ConsoleUtil.success(saved ? "All changes written to disk."
                    : "Nothing new to save.");

            reminderService.stop();
            reminderThread.interrupt();
            autoSaveService.shutdown();

            try {
                // join: wait (briefly) for the thread to actually finish
                autoSaveService.join(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            ConsoleUtil.info("Goodbye.");
        }
    }
}
