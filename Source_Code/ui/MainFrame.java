package ui;

import service.LibraryService;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** The main window: a title and one tab per screen of the system. */
public class MainFrame extends JFrame {

    public MainFrame(LibraryService service) {
        super("Library Study Room & Computer Station Booking System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // center on screen

        JLabel header = new JLabel("Library Study Room & Computer Station Booking", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 20f));
        header.setBorder(new EmptyBorder(12, 0, 8, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Book a Slot", new BookingPanel(service));
        // Phase 4 adds the Walk-in tab and Phase 5 adds the Records tab: one addTab line each.

        // Every tab is a ServicePanel, so MainFrame can refresh whichever one is opened.
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected instanceof ServicePanel) {
                ((ServicePanel) selected).refresh();
            }
        });

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
