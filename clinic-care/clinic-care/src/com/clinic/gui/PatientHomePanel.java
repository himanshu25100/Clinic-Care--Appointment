package com.clinic.gui;

import com.clinic.exception.ClinicException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Prescription;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.RecordService;
import com.clinic.storage.DataStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@SuppressWarnings({ "this-escape", "serial" })
public class PatientHomePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GuiApp app;
    private final Patient patient;
    private final DataStore store;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final RecordService recordService;

    private DefaultTableModel upcomingModel;
    private JTextArea historyArea;

    public PatientHomePanel(GuiApp app, Patient patient, DataStore store,
            PatientService patientService,
            AppointmentService appointmentService,
            RecordService recordService) {
        this.app = app;
        this.patient = patient;
        this.store = store;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.recordService = recordService;

        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        add(NavBar.build("Welcome, " + patient.getName() + "  (" + patient.getId() + ")",
                () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiTheme.FONT_BODY);
        tabs.addTab("Book Appointment", buildBookingTab());
        tabs.addTab("My Appointments", buildAppointmentsTab());
        tabs.addTab("My Prescriptions", buildHistoryTab());
        tabs.addTab("My Profile", buildProfileTab());
        tabs.addChangeListener(e -> {
            refreshAppointments();
            refreshHistory();
        });

        add(tabs, BorderLayout.CENTER);
        refreshAppointments();
    }

    private JPanel buildBookingTab() {
        JPanel outer = new JPanel(new BorderLayout(12, 12));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JComboBox<Doctor> doctorBox = new JComboBox<>(
                store.allDoctors().toArray(new Doctor[0]));
        doctorBox.setRenderer(
                (list, value, index, isSelected, hasFocus) -> new JLabel("  " + value.getId() + " - " + value.getName()
                        + "  (" + value.getSpecialization().getLabel() + ")"));

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd-MM-yyyy"));
        dateSpinner.setValue(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));

        JButton checkButton = UiTheme.secondaryButton("Check availability");

        DefaultTableModel slotModel = new DefaultTableModel(
                new Object[] { "#", "Time", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable slotTable = new JTable(slotModel);
        slotTable.setFont(UiTheme.FONT_BODY);
        slotTable.setRowHeight(24);
        slotTable.getColumnModel().getColumn(0).setMaxWidth(40);

        JTextField reasonField = new JTextField();
        JButton bookButton = UiTheme.primaryButton("Book selected slot");
        bookButton.setEnabled(false);

        checkButton.addActionListener(e -> {
            try {
                Doctor doctor = (Doctor) doctorBox.getSelectedItem();
                LocalDate date = toLocalDate(dateSpinner);
                boolean[] free = appointmentService.freeSlots(doctor.getId(), date);
                slotModel.setRowCount(0);
                for (int i = 0; i < free.length; i++) {
                    slotModel.addRow(new Object[] { i + 1, Doctor.slotLabel(i),
                            free[i] ? "FREE" : "booked" });
                }
                bookButton.setEnabled(true);
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });

        bookButton.addActionListener(e -> {
            int row = slotTable.getSelectedRow();
            if (row < 0) {
                UiTheme.showError(this, "Select a slot from the table first.");
                return;
            }
            if (!"FREE".equals(slotModel.getValueAt(row, 2))) {
                UiTheme.showError(this, "That slot is already booked. Pick a FREE one.");
                return;
            }
            try {
                Doctor doctor = (Doctor) doctorBox.getSelectedItem();
                LocalDate date = toLocalDate(dateSpinner);
                int slotIndex = (Integer) slotModel.getValueAt(row, 0) - 1;

                Appointment appt = appointmentService.book(patient.getId(),
                        doctor.getId(), date, slotIndex, reasonField.getText());

                UiTheme.showSuccess(this, "Appointment confirmed.\n\n"
                        + "ID: " + appt.getId() + "\n"
                        + "Doctor: " + doctor.getName() + "\n"
                        + "When: " + date + "  " + appt.getSlotLabel() + "\n"
                        + "Fee: Rs. " + appt.getFeeCharged()
                        + (patient.isSeniorCitizen() ? "  (senior discount applied)" : ""));

                reasonField.setText("");
                checkButton.doClick();
                refreshAppointments();
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = FormUtil.constraints();
        FormUtil.row(form, c, 0, "Doctor", doctorBox);
        FormUtil.row(form, c, 1, "Date", dateSpinner);
        c.gridx = 1;
        c.gridy = 2;
        form.add(checkButton, c);
        FormUtil.row(form, c, 3, "Reason for visit", reasonField);
        c.gridx = 1;
        c.gridy = 4;
        form.add(bookButton, c);

        outer.add(form, BorderLayout.NORTH);
        outer.add(new JScrollPane(slotTable), BorderLayout.CENTER);
        return outer;
    }

    private LocalDate toLocalDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JPanel buildAppointmentsTab() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        upcomingModel = new DefaultTableModel(
                new Object[] { "Appt ID", "Date", "Slot", "Doctor", "Reason", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(upcomingModel);
        table.setFont(UiTheme.FONT_BODY);
        table.setRowHeight(24);

        JButton cancelBtn = UiTheme.secondaryButton("Cancel selected");
        JButton reschedBtn = UiTheme.secondaryButton("Reschedule selected");
        JButton refreshBtn = UiTheme.secondaryButton("Refresh");

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                UiTheme.showError(this, "Select an appointment first.");
                return;
            }
            String id = (String) upcomingModel.getValueAt(row, 0);
            if (!UiTheme.confirm(this, "Cancel appointment " + id + "?")) {
                return;
            }
            try {
                appointmentService.cancel(id, patient.getId());
                UiTheme.showSuccess(this, "Cancelled. The slot is free again.");
                refreshAppointments();
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });

        reschedBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                UiTheme.showError(this, "Select an appointment first.");
                return;
            }
            String id = (String) upcomingModel.getValueAt(row, 0);
            new RescheduleDialog(SwingUtilities.getWindowAncestor(this),
                    appointmentService, store, id, patient.getId(),
                    this::refreshAppointments).setVisible(true);
        });

        refreshBtn.addActionListener(e -> refreshAppointments());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(cancelBtn);
        buttons.add(reschedBtn);
        buttons.add(refreshBtn);

        outer.add(new JScrollPane(table), BorderLayout.CENTER);
        outer.add(buttons, BorderLayout.SOUTH);
        return outer;
    }

    private void refreshAppointments() {
        upcomingModel.setRowCount(0);
        for (Appointment a : appointmentService.forPatient(patient.getId())) {
            Doctor d = store.findDoctor(a.getDoctorId());
            upcomingModel.addRow(new Object[] {
                    a.getId(), a.getDate().toString(), a.getSlotLabel(),
                    d == null ? a.getDoctorId() : d.getName(),
                    a.getReason(), a.getStatus().toString() });
        }
    }

    private JPanel buildHistoryTab() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(UiTheme.FONT_MONO);
        historyArea.setBackground(Color.WHITE);
        historyArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        outer.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        refreshHistory();
        return outer;
    }

    private void refreshHistory() {
        if (historyArea == null) {
            return;
        }
        try {
            List<Prescription> history = recordService.historyOf(patient.getId());
            StringBuilder sb = new StringBuilder();

            if (history.isEmpty()) {
                sb.append("No prescriptions on record yet.\n");
            } else {
                Set<String> diagnoses = recordService.pastDiagnoses(patient.getId());
                sb.append("Past conditions: ").append(String.join(", ", diagnoses)).append("\n");
                sb.append("=".repeat(70)).append("\n\n");

                for (Prescription r : history) {
                    Doctor d = store.findDoctor(r.getDoctorId());
                    sb.append("Prescription ").append(r.getId())
                            .append("   dated ").append(r.getIssuedOn()).append("\n");
                    sb.append("Doctor: ").append(d == null ? r.getDoctorId() : d.getName()).append("\n");
                    sb.append("Diagnosis: ").append(r.getDiagnosis()).append("\n");
                    sb.append("Medicines:\n");
                    for (Prescription.Medicine m : r.getMedicines()) {
                        sb.append("   - ").append(m).append("\n");
                    }
                    if (!r.getAdvice().isEmpty()) {
                        sb.append("Advice: ").append(r.getAdvice()).append("\n");
                    }
                    if (r.getFollowUpOn() != null) {
                        sb.append("Follow-up on: ").append(r.getFollowUpOn()).append("\n");
                    }
                    sb.append("-".repeat(70)).append("\n\n");
                }
            }
            historyArea.setText(sb.toString());
            historyArea.setCaretPosition(0);
        } catch (ClinicException ex) {
            historyArea.setText("Could not load history: " + ex.getMessage());
        }
    }

    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = FormUtil.constraints();

        JPanel form = UiTheme.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints fc = FormUtil.constraints();

        JLabel idLbl = new JLabel(patient.getId());
        JLabel ageLbl = new JLabel(String.valueOf(patient.getAge()));
        JLabel genderLbl = new JLabel(patient.getGender());
        JLabel bloodLbl = new JLabel(patient.getBloodGroup());
        JLabel regLbl = new JLabel(patient.getRegisteredOn().toString());
        JTextField phoneField = new JTextField(patient.getPhone(), 16);
        JButton save = UiTheme.primaryButton("Update phone");

        FormUtil.row(form, fc, 0, "Patient ID", idLbl);
        FormUtil.row(form, fc, 1, "Age", ageLbl);
        FormUtil.row(form, fc, 2, "Gender", genderLbl);
        FormUtil.row(form, fc, 3, "Blood group", bloodLbl);
        FormUtil.row(form, fc, 4, "Registered on", regLbl);
        FormUtil.row(form, fc, 5, "Phone", phoneField);

        save.addActionListener(e -> {
            try {
                patientService.updatePhone(patient.getId(), phoneField.getText());
                UiTheme.showSuccess(this, "Phone number updated.");
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });
        fc.gridx = 1;
        fc.gridy = 6;
        form.add(save, fc);

        panel.add(form, c);
        return panel;
    }
}
