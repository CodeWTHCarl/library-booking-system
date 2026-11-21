package ui;

import model.Client;
import service.LibraryService;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * ABSTRACTION + INHERITANCE in the UI: every tab (Booking, Walk-in, Records) is a ServicePanel.
 * It holds the service, the dialog helpers and the form-building helpers that all tabs share,
 * and forces each tab to say how it refreshes itself.
 */
public abstract class ServicePanel extends JPanel {
    protected final LibraryService service;

    protected ServicePanel(LibraryService service) {
        this.service = service;
    }

    /** Reloads what this tab shows. MainFrame calls it every time the tab is opened (POLYMORPHISM). */
    public abstract void refresh();

    // ---- dialogs ----

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot complete", JOptionPane.ERROR_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // ---- form helpers ----

    /** If the typed student ID belongs to a saved client, fill in that client's name and contact number. */
    protected void autofillClient(JTextField studentId, JTextField fullName, JTextField contactNo) {
        Client client = service.findClient(studentId.getText());
        if (client != null) {
            fullName.setText(client.getFullName());
            contactNo.setText(client.getContactNo());
        }
    }

    /** Adds "label + field" as one row of a GridBagLayout panel. */
    protected void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    /** An empty last row that soaks up spare height, so the fields stay at the top of the box. */
    protected void addFiller(JPanel panel, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.weighty = 1;
        panel.add(new JLabel(), gbc);
    }
}
