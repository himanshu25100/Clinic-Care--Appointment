package com.clinic.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * One place for colors, fonts and small builder methods, so every screen in
 * the GUI looks consistent instead of each dialog inventing its own style.
 *
 * Syllabus concepts: static utility class, Java Swing basics building on
 * the same OOP fundamentals (classes, access modifiers, static members).
 */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x2E6F6F);
    public static final Color PRIMARY_DARK = new Color(0x1E4E4E);
    public static final Color ACCENT = new Color(0xD98C4A);
    public static final Color BG = new Color(0xF4F6F5);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color ERROR = new Color(0xB3261E);
    public static final Color SUCCESS = new Color(0x1B7A43);
    public static final Color TEXT = new Color(0x222222);
    public static final Color MUTED = new Color(0x6B6B6B);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);

    private UiTheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex) {
            // fall back to the platform default silently - cosmetic only
        }
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", new Color(0xDDE3E2));
        UIManager.put("text", TEXT);
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BODY.deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(PRIMARY_DARK);
        b.setFont(FONT_BODY);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createLineBorder(PRIMARY, 1));
        return b;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(PRIMARY_DARK);
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADING);
        l.setForeground(PRIMARY_DARK);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(MUTED);
        return l;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0), 1),
                new EmptyBorder(20, 24, 20, 24)));
        return p;
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
