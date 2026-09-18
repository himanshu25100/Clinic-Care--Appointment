package com.clinic.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Gatekeeper before the doctor/staff tools. Same PIN concept as
 * MenuApp.STAFF_PIN in the console build.
 */
@SuppressWarnings({"this-escape","serial"})
public class StaffLoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;


    private static final String STAFF_PIN = "1234";

    public StaffLoginPanel(GuiApp app) {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        add(NavBar.build("Doctor / Staff Desk", () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);

        JPanel card = UiTheme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 180));
        GridBagConstraints c = FormUtil.constraints();

        JLabel heading = UiTheme.heading("Staff PIN required");
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        card.add(heading, c);
        c.gridwidth = 1;

        JPasswordField pinField = new JPasswordField(10);
        FormUtil.row(card, c, 1, "PIN", pinField);

        JButton go = UiTheme.primaryButton("Unlock");
        Runnable tryLogin = () -> {
            String entered = new String(pinField.getPassword());
            if (STAFF_PIN.equals(entered)) {
                pinField.setText("");
                app.enterStaffHome();
            } else {
                UiTheme.showError(this, "Wrong PIN. Access denied.");
            }
        };
        go.addActionListener(e -> tryLogin.run());
        pinField.addActionListener(e -> tryLogin.run());

        c.gridx = 1;
        c.gridy = 2;
        card.add(go, c);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(UiTheme.BG);
        wrap.add(card);
        add(wrap, BorderLayout.CENTER);
    }
}
