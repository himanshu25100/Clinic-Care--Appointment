package com.clinic.gui;

import com.clinic.exception.ClinicException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
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


@SuppressWarnings({"this-escape","serial"})
public class StaffHomePanel extends JPanel {

    private static final long serialVersionUID = 1L;


    private final GuiApp app;
    private final DataStore store;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final RecordService recordService;

    public StaffHomePanel(GuiApp app, DataStore store, PatientService patientService,
                          AppointmentService appointmentService, RecordService recordService) {
        this.app = app;
        this.store = store;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.recordService = recordService;

        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        add(NavBar.build("Staff Desk", () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiTheme.FONT_BODY);
        tabs.addTab("Day Schedule", buildScheduleTab());
        tabs.addTab("Write Prescription", buildPrescriptionTab());
        tabs.addTab("Patient File", buildPatientFileTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ==================================================================
    // Day schedule
    // ==================================================================

    private JPanel buildScheduleTab() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JComboBox<Doctor> doctorBox = new JComboBox<>(store.allDoctors().toArray(new Doctor[0]));
        doctorBox.setRenderer((list, value, index, isSelected, hasFocus) ->
                new JLabel("  " + value.getId() + " - " + value.getName()));

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd-MM-yyyy"));
        dateSpinner.setValue(java.sql.Date.valueOf(LocalDate.now()));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Appt ID", "Slot", "Patient ID", "Patient", "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFont(UiTheme.FONT_BODY);
        table.setRowHeight(24);

        JButton load = UiTheme.secondaryButton("Load schedule");
        JButton noShow = UiTheme.secondaryButton("Mark selected as no-show");

        load.addActionListener(e -> {
            Doctor doctor = (Doctor) doctorBox.getSelectedItem();
            LocalDate date = toLocalDate(dateSpinner);
            model.setRowCount(0);
            List<Appointment> list = appointmentService.forDoctorOnDate(doctor.getId(), date);
            for (Appointment a : list) {
                Patient p = store.findPatient(a.getPatientId());
                model.addRow(new Object[]{a.getId(), a.getSlotLabel(), a.getPatientId(),
                        p == null ? "?" : p.getName(), a.getReason(), a.getStatus().toString()});
            }
            if (list.isEmpty()) {
                UiTheme.showError(this, "No patients booked that day.");
            }
        });

        noShow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                UiTheme.showError(this, "Select a row first.");
                return;
            }
            String id = (String) model.getValueAt(row, 0);
            Appointment a = store.findAppointment(id);
            if (a == null || !a.getStatus().canMoveTo(AppointmentStatus.NO_SHOW)) {
                UiTheme.showError(this, "That appointment cannot be marked no-show.");
                return;
            }
            a.setStatus(AppointmentStatus.NO_SHOW);
            model.setValueAt("NO_SHOW", row, 5);
            UiTheme.showSuccess(this, "Marked as NO_SHOW.");
        });

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints c = FormUtil.constraints();
        FormUtil.row(top, c, 0, "Doctor", doctorBox);
        FormUtil.row(top, c, 1, "Date", dateSpinner);
        c.gridx = 1;
        c.gridy = 2;
        top.add(load, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(noShow);

        outer.add(top, BorderLayout.NORTH);
        outer.add(new JScrollPane(table), BorderLayout.CENTER);
        outer.add(buttons, BorderLayout.SOUTH);
        return outer;
    }

    // ==================================================================
    // Write prescription
    // ==================================================================

    private JPanel buildPrescriptionTab() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = FormUtil.constraints();

        JTextField apptField = new JTextField(16);
        JTextField diagnosisField = new JTextField(24);
        JTextArea medsArea = new JTextArea(3, 24);
        medsArea.setLineWrap(true);
        JTextField adviceField = new JTextField(24);
        JTextField followUpField = new JTextField(12);
        followUpField.setText("none");

        FormUtil.row(form, c, 0, "Appointment ID", apptField);
        FormUtil.row(form, c, 1, "Diagnosis", diagnosisField);
        c.gridx = 0;
        c.gridy = 2;
        form.add(new JLabel("Medicines"), c);
        c.gridx = 1;
        form.add(new JScrollPane(medsArea), c);
        FormUtil.row(form, c, 3, "Advice", adviceField);
        FormUtil.row(form, c, 4, "Follow-up (dd-MM-yyyy or none)", followUpField);

        JLabel hint = UiTheme.muted(
                "<html>Medicine format: name|dosage|days &nbsp; e.g. "
                        + "Crocin|1-0-1|5;Zincovit|0-0-1|10</html>");

        JButton save = UiTheme.primaryButton("Save prescription");
        save.addActionListener(e -> {
            try {
                Prescription r = recordService.writePrescription(
                        apptField.getText(), diagnosisField.getText(), medsArea.getText(),
                        adviceField.getText(), followUpField.getText());
                UiTheme.showSuccess(this, "Saved as " + r.getId()
                        + ".\nThe appointment is now marked COMPLETED.");
                apptField.setText("");
                diagnosisField.setText("");
                medsArea.setText("");
                adviceField.setText("");
                followUpField.setText("none");
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });
        c.gridx = 1;
        c.gridy = 5;
        form.add(save, c);

        outer.add(hint, BorderLayout.NORTH);
        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ==================================================================
    // Patient file
    // ==================================================================

    private JPanel buildPatientFileTab() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JTextField idField = new JTextField(14);
        JButton open = UiTheme.secondaryButton("Open");
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(UiTheme.FONT_MONO);
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Runnable doOpen = () -> {
            try {
                Patient p = patientService.requirePatient(idField.getText());
                StringBuilder sb = new StringBuilder();
                sb.append("PATIENT FILE\n").append("=".repeat(60)).append("\n");
                sb.append("ID: ").append(p.getId()).append("\n");
                sb.append("Name: ").append(p.getName()).append("\n");
                sb.append("Age / Gender: ").append(p.getAge()).append(" / ").append(p.getGender()).append("\n");
                sb.append("Blood group: ").append(p.getBloodGroup()).append("\n");
                sb.append("Phone: ").append(p.getPhone()).append("\n");
                sb.append("Registered on: ").append(p.getRegisteredOn()).append("\n\n");

                sb.append("VISITS\n").append("-".repeat(60)).append("\n");
                List<Appointment> visits = appointmentService.forPatient(p.getId());
                if (visits.isEmpty()) {
                    sb.append("(none)\n");
                }
                for (Appointment a : visits) {
                    sb.append(a.toString()).append("\n");
                }

                sb.append("\nPRESCRIPTIONS\n").append("-".repeat(60)).append("\n");
                List<Prescription> history = recordService.historyOf(p.getId());
                if (history.isEmpty()) {
                    sb.append("(none)\n");
                }
                for (Prescription r : history) {
                    sb.append(r.getId()).append("  ").append(r.getIssuedOn())
                            .append("  ").append(r.getDiagnosis()).append("\n");
                    for (Prescription.Medicine m : r.getMedicines()) {
                        sb.append("    - ").append(m).append("\n");
                    }
                }
                area.setText(sb.toString());
                area.setCaretPosition(0);
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        };
        open.addActionListener(e -> doOpen.run());
        idField.addActionListener(e -> doOpen.run());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(new JLabel("Patient ID: "), BorderLayout.WEST);
        top.add(idField, BorderLayout.CENTER);
        top.add(open, BorderLayout.EAST);

        outer.add(top, BorderLayout.NORTH);
        outer.add(new JScrollPane(area), BorderLayout.CENTER);
        return outer;
    }

    private LocalDate toLocalDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
}
