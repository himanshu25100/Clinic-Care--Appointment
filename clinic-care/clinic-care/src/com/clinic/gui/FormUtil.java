package com.clinic.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Tiny helper for laying out label+field rows in a GridBagLayout without
 * repeating the same six lines of constraint code on every form.
 */
final class FormUtil {

    private FormUtil() {
    }

    static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    /** Places a label in column 0 and the given field in column 1 of row y. */
    static void row(JPanel panel, GridBagConstraints c, int y, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(UiTheme.FONT_BODY);
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 0;
        panel.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        field.setFont(UiTheme.FONT_BODY);
        panel.add(field, c);
    }
}
