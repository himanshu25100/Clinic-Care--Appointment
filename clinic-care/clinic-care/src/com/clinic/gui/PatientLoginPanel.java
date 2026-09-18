package com.clinic.gui;

import com.clinic.exception.ClinicException;
import com.clinic.model.Patient;
import com.clinic.service.PatientService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@SuppressWarnings({ "this-escape", "serial" })
public class PatientLoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GuiApp app;
    private final PatientService patientService;

    private JTextField idField;
    private JTextField nameField, phoneField, ageField;
    private JComboBox<String> genderBox, bloodBox;
    private DefaultListModel<Patient> searchResults;

    public PatientLoginPanel(GuiApp app, PatientService patientService) {
        this.app = app;
        this.patientService = patientService;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        add(NavBar.build("Patient Desk", () -> app.show(GuiApp.HOME)), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiTheme.FONT_BODY);
        tabs.addTab("I have a Patient ID", buildLoginTab());
        tabs.addTab("New Patient", buildRegisterTab());
        tabs.addTab("Forgot my ID", buildSearchTab());

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(UiTheme.BG);
        wrap.add(tabs);
        add(wrap, BorderLayout.CENTER);
    }

    private JPanel buildLoginTab() {
        JPanel panel = UiTheme.card();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(460, 220));
        GridBagConstraints c = FormUtil.constraints();

        idField = new JTextField(16);
        FormUtil.row(panel, c, 0, "Patient ID (e.g. P-1001)", idField);

        JButton go = UiTheme.primaryButton("Continue");
        go.addActionListener(e -> doLogin());
        c.gridx = 1;
        c.gridy = 2;
        panel.add(go, c);

        idField.addActionListener(e -> doLogin());
        return panel;
    }

    private void doLogin() {
        try {
            Patient patient = patientService.requirePatient(idField.getText());
            idField.setText("");
            app.enterPatientHome(patient);
        } catch (ClinicException ex) {
            UiTheme.showError(this, ex.displayMessage());
        }
    }

    private JPanel buildRegisterTab() {
        JPanel panel = UiTheme.card();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(460, 320));
        GridBagConstraints c = FormUtil.constraints();

        nameField = new JTextField(18);
        phoneField = new JTextField(18);
        ageField = new JTextField(18);
        genderBox = new JComboBox<>(new String[] { "M", "F", "O" });
        bloodBox = new JComboBox<>(new String[] {
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-" });

        FormUtil.row(panel, c, 0, "Full name", nameField);
        FormUtil.row(panel, c, 1, "Phone (10 digits)", phoneField);
        FormUtil.row(panel, c, 2, "Age", ageField);
        FormUtil.row(panel, c, 3, "Gender", genderBox);
        FormUtil.row(panel, c, 4, "Blood group", bloodBox);

        JButton go = UiTheme.primaryButton("Register");
        go.addActionListener(e -> doRegister());
        c.gridx = 1;
        c.gridy = 5;
        panel.add(go, c);

        return panel;
    }

    private void doRegister() {
        try {
            Patient patient = patientService.register(
                    nameField.getText(), phoneField.getText(), ageField.getText(),
                    (String) genderBox.getSelectedItem(), (String) bloodBox.getSelectedItem());

            UiTheme.showSuccess(this, "Registered successfully.\n\n"
                    + "Your Patient ID is:  " + patient.getId()
                    + "\n\nPlease keep this ID - you will need it every visit.");

            nameField.setText("");
            phoneField.setText("");
            ageField.setText("");
            app.enterPatientHome(patient);
        } catch (ClinicException ex) {
            UiTheme.showError(this, ex.displayMessage());
        }
    }

    private JPanel buildSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(520, 340));

        JTextField search = new JTextField();
        JButton go = UiTheme.secondaryButton("Search");

        searchResults = new DefaultListModel<>();
        JList<Patient> list = new JList<>(searchResults);
        list.setFont(UiTheme.FONT_MONO);
        list.setCellRenderer(
                (jl, value, index, isSelected, hasFocus) -> new JLabel("  " + value.getId() + "   " + value.getName()
                        + "   " + value.getPhone()));

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    app.enterPatientHome(list.getSelectedValue());
                }
            }
        });

        Runnable doSearch = () -> {
            searchResults.clear();
            List<Patient> matches = patientService.searchByName(search.getText());
            for (Patient p : matches) {
                searchResults.addElement(p);
            }
            if (matches.isEmpty()) {
                UiTheme.showError(this, "Nobody found. Try the New Patient tab instead.");
            }
        };
        go.addActionListener(e -> doSearch.run());
        search.addActionListener(e -> doSearch.run());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(new JLabel("Name contains: "), BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        top.add(go, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(UiTheme.muted("Double-click a result to continue"), BorderLayout.SOUTH);
        return panel;
    }
}
