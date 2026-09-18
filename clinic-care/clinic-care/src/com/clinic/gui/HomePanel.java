package com.clinic.gui;

import javax.swing.*;
import java.awt.*;


@SuppressWarnings({"this-escape","serial"})
public class HomePanel extends JPanel {

    private static final long serialVersionUID = 1L;


    public HomePanel(GuiApp app) {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BG);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel logo = UiTheme.title("ClinicCare");
        logo.setFont(logo.getFont().deriveFont(34f));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = UiTheme.muted("Appointment booking and medical records, in one place");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(logo);
        content.add(Box.createVerticalStrut(6));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(36));

        content.add(desk("Patient Desk", "Book a visit, check history, manage appointments",
                () -> app.show(GuiApp.PATIENT_LOGIN)));
        content.add(Box.createVerticalStrut(16));
        content.add(desk("Doctor / Staff Desk", "Schedules, prescriptions, patient files (PIN required)",
                () -> app.show(GuiApp.STAFF_LOGIN)));
        content.add(Box.createVerticalStrut(16));
        content.add(desk("Reports & Analytics", "Clinic-wide numbers for the manager",
                () -> app.show(GuiApp.REPORTS)));

        add(content);
    }

    private JPanel desk(String title, String subtitle, Runnable onClick) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(16, 0));
        card.setMaximumSize(new Dimension(520, 90));
        card.setPreferredSize(new Dimension(520, 90));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = UiTheme.heading(title);
        JLabel s = UiTheme.muted(subtitle);
        text.add(t);
        text.add(s);

        JButton go = UiTheme.primaryButton("Open  ->");
        go.addActionListener(e -> onClick.run());

        card.add(text, BorderLayout.CENTER);
        card.add(go, BorderLayout.EAST);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                onClick.run();
            }
        });
        return card;
    }
}
