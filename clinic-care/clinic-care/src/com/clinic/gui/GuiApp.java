package com.clinic.gui;

import com.clinic.concurrent.AutoSaveService;
import com.clinic.concurrent.ReminderService;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.RecordService;
import com.clinic.service.ReportService;
import com.clinic.storage.DataStore;
import com.clinic.storage.Repository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@SuppressWarnings({ "this-escape", "serial" })
public class GuiApp extends JFrame {

    public static final String HOME = "HOME";
    public static final String PATIENT_LOGIN = "PATIENT_LOGIN";
    public static final String PATIENT_HOME = "PATIENT_HOME";
    public static final String STAFF_LOGIN = "STAFF_LOGIN";
    public static final String STAFF_HOME = "STAFF_HOME";
    public static final String REPORTS = "REPORTS";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHolder = new JPanel(cardLayout);

    private final DataStore store;
    private final Repository repository;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final RecordService recordService;
    private final ReportService reportService;
    private final ReminderService reminderService;
    private final AutoSaveService autoSaveService;

    private PatientHomePanel patientHomePanel;
    private StaffHomePanel staffHomePanel;
    private final JLabel statusBar = new JLabel(" Ready");
    private javax.swing.Timer reminderTimer;

    public GuiApp(DataStore store, Repository repository,
            PatientService patientService, AppointmentService appointmentService,
            RecordService recordService, ReportService reportService,
            ReminderService reminderService, AutoSaveService autoSaveService) {

        super("ClinicCare - Appointment & Records System");
        this.store = store;
        this.repository = repository;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.recordService = recordService;
        this.reportService = reportService;
        this.reminderService = reminderService;
        this.autoSaveService = autoSaveService;

        buildWindow();
        buildScreens();
        startReminderPolling();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
            }
        });
    }

    private void buildWindow() {
        setSize(1000, 680);
        setMinimumSize(new Dimension(860, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout());

        cardHolder.setBackground(UiTheme.BG);
        add(cardHolder, BorderLayout.CENTER);

        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        statusBar.setFont(UiTheme.FONT_BODY);
        statusBar.setForeground(UiTheme.MUTED);
        statusBar.setOpaque(true);
        statusBar.setBackground(Color.WHITE);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void buildScreens() {
        cardHolder.add(new HomePanel(this), HOME);
        cardHolder.add(new PatientLoginPanel(this, patientService), PATIENT_LOGIN);
        cardHolder.add(new StaffLoginPanel(this), STAFF_LOGIN);
        cardHolder.add(new ReportsPanel(this, store, reportService, patientService), REPORTS);
    }

    /** Called once a patient has registered or logged in successfully. */
    public void enterPatientHome(com.clinic.model.Patient patient) {
        patientHomePanel = new PatientHomePanel(this, patient, store,
                patientService, appointmentService, recordService);
        cardHolder.add(patientHomePanel, PATIENT_HOME);
        show(PATIENT_HOME);
        setStatus("Signed in as " + patient.getName() + "  (" + patient.getId() + ")");
    }

    /** Called once the staff PIN has been accepted. */
    public void enterStaffHome() {
        staffHomePanel = new StaffHomePanel(this, store,
                patientService, appointmentService, recordService);
        cardHolder.add(staffHomePanel, STAFF_HOME);
        show(STAFF_HOME);
        setStatus("Staff desk unlocked");
    }

    public void show(String cardName) {
        cardLayout.show(cardHolder, cardName);
    }

    public void setStatus(String text) {
        statusBar.setText(" " + text);
    }

    public DataStore getStore() {
        return store;
    }

    private void startReminderPolling() {
        reminderTimer = new javax.swing.Timer(4000, e -> {
            List<String> reminders = reminderService.drainReminders();
            if (!reminders.isEmpty()) {
                StringBuilder sb = new StringBuilder("Upcoming appointment reminders:\n\n");
                for (String r : reminders) {
                    sb.append(" - ").append(r).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString(),
                        "Reminder", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        reminderTimer.start();
    }

    private void shutdown() {
        if (!UiTheme.confirm(this, "Save changes and exit ClinicCare?")) {
            return;
        }
        if (reminderTimer != null) {
            reminderTimer.stop();
        }
        setStatus("Saving...");
        boolean saved = autoSaveService.saveIfDirty();
        reminderService.stop();
        autoSaveService.shutdown();
        try {
            autoSaveService.join(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println(saved ? "All changes written to disk."
                : "Nothing new to save.");
        dispose();
        System.exit(0);
    }
}
