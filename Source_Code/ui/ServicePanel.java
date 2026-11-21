package ui;

import service.LibraryService;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * ABSTRACTION + INHERITANCE in the UI: every tab (Booking, Walk-in, Records) is a ServicePanel.
 * It holds the service and the dialog helpers, and forces each tab to say how it refreshes itself.
 */
public abstract class ServicePanel extends JPanel {
    protected final LibraryService service;

    protected ServicePanel(LibraryService service) {
        this.service = service;
    }

    /** Reloads what this tab shows. MainFrame calls it every time the tab is opened (POLYMORPHISM). */
    public abstract void refresh();

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
}
