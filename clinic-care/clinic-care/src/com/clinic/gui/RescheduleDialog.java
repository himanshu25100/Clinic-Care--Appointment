package com.clinic.gui;

import com.clinic.exception.ClinicException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.service.AppointmentService;
import com.clinic.storage.DataStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;


@SuppressWarnings({"this-escape","serial"})
public class RescheduleDialog extends JDialog {

    private static final long serialVersionUID = 1L;


    public RescheduleDialog(Window owner, AppointmentService appointmentService,
                            DataStore store, String appointmentId, String patientId,
                            Runnable onSuccess) {
        super(owner, "Reschedule " + appointmentId, ModalityType.APPLICATION_MODAL);
        setSize(480, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        Appointment old = store.findAppointment(appointmentId);
        if (old == null) {
            UiTheme.showError(owner, "Appointment not found.");
            dispose();
            return;
        }

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd-MM-yyyy"));
        dateSpinner.setValue(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));

        JButton checkButton = UiTheme.secondaryButton("Check availability");
        DefaultTableModel slotModel = new DefaultTableModel(
                new Object[]{"#", "Time", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable slotTable = new JTable(slotModel);
        slotTable.setRowHeight(22);

        checkButton.addActionListener(e -> {
            try {
                LocalDate date = toLocalDate(dateSpinner);
                boolean[] free = appointmentService.freeSlots(old.getDoctorId(), date);
                slotModel.setRowCount(0);
                for (int i = 0; i < free.length; i++) {
                    slotModel.addRow(new Object[]{i + 1, Doctor.slotLabel(i),
                            free[i] ? "FREE" : "booked"});
                }
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });

        JButton confirm = UiTheme.primaryButton("Confirm reschedule");
        confirm.addActionListener(e -> {
            int row = slotTable.getSelectedRow();
            if (row < 0 || !"FREE".equals(slotModel.getValueAt(row, 2))) {
                UiTheme.showError(this, "Pick a FREE slot from the table.");
                return;
            }
            try {
                LocalDate date = toLocalDate(dateSpinner);
                int slotIndex = (Integer) slotModel.getValueAt(row, 0) - 1;
                Appointment fresh = appointmentService.reschedule(
                        appointmentId, patientId, date, slotIndex);
                UiTheme.showSuccess(this, "Moved to " + fresh.getDate() + " "
                        + fresh.getSlotLabel() + ".\nNew ID: " + fresh.getId());
                onSuccess.run();
                dispose();
            } catch (ClinicException ex) {
                UiTheme.showError(this, ex.displayMessage());
            }
        });

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = FormUtil.constraints();
        FormUtil.row(top, c, 0, "New date", dateSpinner);
        c.gridx = 1;
        c.gridy = 1;
        top.add(checkButton, c);

        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(slotTable), BorderLayout.CENTER);
        content.add(confirm, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private LocalDate toLocalDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
}
