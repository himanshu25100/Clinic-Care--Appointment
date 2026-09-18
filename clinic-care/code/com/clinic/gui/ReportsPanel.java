package com.clinic.gui;

import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.service.PatientService;
import com.clinic.service.ReportService;
import com.clinic.storage.DataStore;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"this-escape","serial"})
public class ReportsPanel extends JPanel {

    private static final long serialVersionUID = 1L;


    private final DataStore store;
    private final ReportService reportService;
    private final PatientService patientService;
    private final JTextArea output = new JTextArea();

    public ReportsPanel(GuiApp app, DataStore store, ReportService reportService,
                        PatientService patientService) {
        this.store = store;
        this.reportService = reportService;
        this.patientService = patientService;

        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        add(NavBar.build("Reports & Analytics", () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(UiTheme.FONT_MONO);
        output.setBackground(Color.WHITE);
        output.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.setBackground(UiTheme.BG);
        buttons.add(reportButton("Per doctor", this::reportPerDoctor));
        buttons.add(reportButton("Status breakdown", this::reportStatus));
        buttons.add(reportButton("Revenue", this::reportRevenue));
        buttons.add(reportButton("Age distribution", this::reportAges));
        buttons.add(reportButton("Top diagnoses", this::reportDiagnoses));
        buttons.add(reportButton("7-day load grid", this::reportLoad));

        // header = nav bar on top, button row directly beneath it
        JPanel header = new JPanel(new BorderLayout());
        header.add(NavBar.build("Reports & Analytics", () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);
        header.add(buttons, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);

        reportPerDoctor();
    }

    private JButton reportButton(String label, Runnable action) {
        JButton b = UiTheme.secondaryButton(label);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void reportPerDoctor() {
        StringBuilder sb = new StringBuilder("APPOINTMENTS PER DOCTOR\n").append("=".repeat(60)).append("\n\n");
        Map<String, Integer> counts = reportService.appointmentsPerDoctor();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Doctor d = store.findDoctor(e.getKey());
            String name = d == null ? e.getKey() : d.getName();
            sb.append(String.format("%-24s %3d  %s%n", name, e.getValue(), "#".repeat(e.getValue())));
        }
        String busiest = reportService.busiestDoctorId();
        if (busiest != null) {
            Doctor d = store.findDoctor(busiest);
            sb.append("\nBusiest: ").append(d == null ? busiest : d.getName()).append("\n");
        }
        output.setText(sb.toString());
    }

    private void reportStatus() {
        StringBuilder sb = new StringBuilder("APPOINTMENT STATUS BREAKDOWN\n").append("=".repeat(60)).append("\n\n");
        Map<AppointmentStatus, Integer> counts = reportService.statusBreakdown();
        for (Map.Entry<AppointmentStatus, Integer> e : counts.entrySet()) {
            sb.append(String.format("%-14s %3d%n", e.getKey(), e.getValue()));
        }
        output.setText(sb.toString());
    }

    private void reportRevenue() {
        StringBuilder sb = new StringBuilder("REVENUE\n").append("=".repeat(60)).append("\n\n");
        sb.append("Total billed:        Rs. ").append(reportService.totalRevenue()).append("\n");
        sb.append("Registered patients: ").append(store.patientCount()).append("\n");
        sb.append("Senior citizens:     ").append(patientService.countSeniorCitizens()).append("\n");
        output.setText(sb.toString());
    }

    private void reportAges() {
        StringBuilder sb = new StringBuilder("PATIENT AGE DISTRIBUTION\n").append("=".repeat(60)).append("\n\n");
        for (Map.Entry<String, Integer> e : reportService.ageDistribution().entrySet()) {
            sb.append(String.format("%-8s %3d  %s%n", e.getKey(), e.getValue(), "*".repeat(e.getValue())));
        }
        output.setText(sb.toString());
    }

    private void reportDiagnoses() {
        StringBuilder sb = new StringBuilder("MOST COMMON DIAGNOSES\n").append("=".repeat(60)).append("\n\n");
        List<String> top = reportService.topDiagnoses(10);
        if (top.isEmpty()) {
            sb.append("No prescriptions recorded yet.\n");
        }
        for (String s : top) {
            sb.append("- ").append(s).append("\n");
        }
        output.setText(sb.toString());
    }

    private void reportLoad() {
        StringBuilder sb = new StringBuilder("CLINIC LOAD - NEXT 7 DAYS (patients booked per slot)\n")
                .append("=".repeat(60)).append("\n\n");
        int[][] grid = reportService.weeklyLoadGrid(LocalDate.now());
        sb.append(String.format("%-12s", "Date"));
        for (int slot = 0; slot < Doctor.SLOTS_PER_DAY; slot++) {
            sb.append(String.format("S%-2d", slot + 1));
        }
        sb.append("\n");
        for (int day = 0; day < grid.length; day++) {
            sb.append(String.format("%-12s", LocalDate.now().plusDays(day).toString()));
            for (int slot = 0; slot < grid[day].length; slot++) {
                sb.append(String.format(" %d ", grid[day][slot]));
            }
            sb.append("\n");
        }
        output.setText(sb.toString());
    }
}
