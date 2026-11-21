import service.LibraryException;
import service.LibraryService;
import ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the application: load the records from MySQL, then open the main window. */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // native look
        } catch (Exception ignored) {
            // the default look is fine too
        }

        SwingUtilities.invokeLater(() -> {
            LibraryService service = new LibraryService();
            try {
                service.loadAll();
            } catch (LibraryException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\n\nCheck that MySQL is running and that db.properties is correct.",
                        "Cannot start", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new MainFrame(service).setVisible(true);
        });
    }
}
