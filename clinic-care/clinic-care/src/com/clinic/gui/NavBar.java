package com.clinic.gui;

import javax.swing.*;
import java.awt.*;

public final class NavBar {

    private NavBar() {
    }

    public static JPanel build(String title, Runnable onBack) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.PRIMARY);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JButton back = new JButton("<-  Home");
        back.setForeground(Color.WHITE);
        back.setBackground(UiTheme.PRIMARY);
        back.setBorderPainted(false);
        back.setContentAreaFilled(false);
        back.setFocusPainted(false);
        back.setFont(UiTheme.FONT_BODY);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());

        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(UiTheme.FONT_HEADING);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        bar.add(back, BorderLayout.WEST);
        bar.add(label, BorderLayout.CENTER);
        return bar;
    }
}
