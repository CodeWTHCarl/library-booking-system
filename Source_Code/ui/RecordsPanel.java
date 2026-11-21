package ui;

import model.BookedReservation;
import model.Visit;
import model.VisitStatus;
import model.VisitType;
import service.LibraryException;
import service.LibraryService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The "Records" tab: every visit in one table, with search and filters, and the staff actions
 * that complete the CRUD cycle (check in, complete, cancel, no-show, reschedule, edit, delete).
 */
public class RecordsPanel extends ServicePanel {
    private static final String ALL_TYPES = "All types";
    private static final String ALL_STATUSES = "All statuses";
    private static final String ANY_DATE = "Any date";

    // search and filters (each combo box holds either the "All ..." text or a real value)
    private final JTextField txtSearch = new JTextField(16);
    private final JComboBox<Object> cmbType = new JComboBox<>();
    private final JComboBox<Object> cmbStatus = new JComboBox<>();
    private final JComboBox<Object> cmbDate = new JComboBox<>();
    private final JButton btnClearFilters = new JButton("Clear Filters");

    // the table
    private final VisitTableModel tableModel = new VisitTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel lblCount = new JLabel(" ");
    private final JLabel lblMessage = new JLabel(" ");

    // staff actions
    private final JButton btnCheckIn = new JButton("Check In");
    private final JButton btnComplete = new JButton("Complete");
    private final JButton btnCancel = new JButton("Cancel Visit");
    private final JButton btnNoShow = new JButton("No-Show");
    private final JButton btnReschedule = new JButton("Reschedule...");
    private final JButton btnEditPurpose = new JButton("Edit Purpose...");
    private final JButton btnDelete = new JButton("Delete");

    private boolean updating = false; // true while the code itself refills the date combo box

    public RecordsPanel(LibraryService service) {
        super(service);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        fillFixedChoices();
        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
        wireEvents();
        refresh();
    }

    // =====================================================================================
    //  BUILDING THE SCREEN
    // =====================================================================================

    private void fillFixedChoices() {
        cmbType.addItem(ALL_TYPES);
        for (VisitType type : VisitType.values()) {
            cmbType.addItem(type);
        }
        cmbStatus.addItem(ALL_STATUSES);
        for (VisitStatus status : VisitStatus.values()) {
            cmbStatus.addItem(status);
        }
        cmbDate.addItem(ANY_DATE);
        cmbDate.setRenderer(new DateListCellRenderer());
        cmbDate.setPrototypeDisplayValue("Mon, Nov 30, 2026  (today)");
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBorder(new TitledBorder("Search and filter"));
        bar.add(new JLabel("Search:"));
        bar.add(txtSearch);
        bar.add(new JLabel("Type:"));
        bar.add(cmbType);
        bar.add(new JLabel("Status:"));
        bar.add(cmbStatus);
        bar.add(new JLabel("Date:"));
        bar.add(cmbDate);
        bar.add(btnClearFilters);
        return bar;
    }

    private JScrollPane buildTable() {
        table.setAutoCreateRowSorter(true); // click a column header to sort
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        VisitTableModel.applyColumnWidths(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new TitledBorder("Records"));
        return scroll;
    }

    private JPanel buildActionBar() {
        lblMessage.setForeground(new Color(20, 110, 50));

        JPanel info = new JPanel(new BorderLayout(12, 0));
        info.add(lblCount, BorderLayout.WEST);
        info.add(lblMessage, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttons.add(btnCheckIn);
        buttons.add(btnComplete);
        buttons.add(btnCancel);
        buttons.add(btnNoShow);
        buttons.add(btnReschedule);
        buttons.add(btnEditPurpose);
        buttons.add(btnDelete);

        JPanel bar = new JPanel(new BorderLayout(0, 4));
        bar.add(info, BorderLayout.NORTH);
        bar.add(buttons, BorderLayout.CENTER);
        return bar;
    }

    // =====================================================================================
    //  EVENT HANDLING
    // =====================================================================================

    private void wireEvents() {
        // the table refilters as soon as anything in the filter bar changes
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });
        cmbType.addActionListener(e -> applyFilters());
        cmbStatus.addActionListener(e -> applyFilters());
        cmbDate.addActionListener(e -> {
            if (!updating) {
                applyFilters();
            }
        });
        btnClearFilters.addActionListener(e -> clearFilters());

        // the buttons light up only for what the selected record allows
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });

        btnCheckIn.addActionListener(e -> changeStatus(VisitStatus.IN_USE, null, "Checked in"));
        btnComplete.addActionListener(e -> changeStatus(VisitStatus.COMPLETED, null, "Completed"));
        btnCancel.addActionListener(e -> changeStatus(VisitStatus.CANCELLED, "Cancel this visit?", "Cancelled"));
        btnNoShow.addActionListener(e -> changeStatus(VisitStatus.NO_SHOW, "Mark this client as a no-show?", "Marked no-show"));
        btnReschedule.addActionListener(e -> onReschedule());
        btnEditPurpose.addActionListener(e -> onEditPurpose());
        btnDelete.addActionListener(e -> onDelete());
    }

    /** One method serves four buttons: only the new status, the question and the message differ. */
    private void changeStatus(VisitStatus next, String question, String doneText) {
        Visit visit = getSelectedVisit();
        if (visit == null) {
            showError("Please select a record first.");
            return;
        }
        String who = describe(visit);
        if (question != null && !confirm(question + "\n\n" + who)) {
            return;
        }
        try {
            service.changeStatus(visit, next);
            say(doneText + ": " + who);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void onReschedule() {
        Visit visit = getSelectedVisit();
        if (!service.canReschedule(visit)) {
            showError("Only a booked reservation can be rescheduled.");
            return;
        }
        BookedReservation booking = (BookedReservation) visit;
        String who = describe(booking);
        ReschedulePanel form = new ReschedulePanel(service, booking);

        // keep showing the form until it is accepted or cancelled
        while (showForm(form, "Reschedule " + who)) {
            try {
                service.reschedule(booking, form.getSelectedResource(), form.getSelectedDate(),
                        form.getSelectedSlot(), form.getPurpose());
                say("Rescheduled: " + who);
                refresh();
                return;
            } catch (LibraryException ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onEditPurpose() {
        Visit visit = getSelectedVisit();
        if (visit == null) {
            showError("Please select a record first.");
            return;
        }
        String who = describe(visit);
        String newPurpose = askText("New purpose for " + who + ":", visit.getPurpose());
        if (newPurpose == null) {
            return; // cancelled
        }
        try {
            service.updatePurpose(visit, newPurpose);
            say("Purpose updated: " + who);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void onDelete() {
        Visit visit = getSelectedVisit();
        if (visit == null) {
            showError("Please select a record first.");
            return;
        }
        String who = describe(visit);
        if (!confirm("Permanently delete this record?\n\n" + who + "\n\nThis cannot be undone.")) {
            return;
        }
        try {
            service.deleteVisit(visit);
            say("Deleted: " + who);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void clearFilters() {
        updating = true;
        txtSearch.setText("");
        cmbType.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        cmbDate.setSelectedIndex(0);
        updating = false;
        applyFilters();
    }

    // =====================================================================================
    //  FILTERING AND SHOWING THE RECORDS
    // =====================================================================================

    @Override
    public void refresh() {
        refreshDateChoices();
        applyFilters();
    }

    /** The date box lists only the dates that actually have records. */
    private void refreshDateChoices() {
        Object previous = cmbDate.getSelectedItem();
        Set<LocalDate> dates = new TreeSet<>(Comparator.reverseOrder()); // newest first
        for (Visit visit : service.getVisits()) {
            dates.add(visit.getVisitDate());
        }
        updating = true;
        cmbDate.removeAllItems();
        cmbDate.addItem(ANY_DATE);
        for (LocalDate date : dates) {
            cmbDate.addItem(date);
        }
        if (previous != null) {
            cmbDate.setSelectedItem(previous); // ignored if that date no longer has records
        }
        updating = false;
    }

    private void applyFilters() {
        Object typeChoice = cmbType.getSelectedItem();
        Object statusChoice = cmbStatus.getSelectedItem();
        Object dateChoice = cmbDate.getSelectedItem();
        VisitType type = (typeChoice instanceof VisitType) ? (VisitType) typeChoice : null;
        VisitStatus status = (statusChoice instanceof VisitStatus) ? (VisitStatus) statusChoice : null;
        LocalDate date = (dateChoice instanceof LocalDate) ? (LocalDate) dateChoice : null;

        List<Visit> shown = service.filterVisits(type, status, date);

        String keyword = txtSearch.getText().trim();
        if (!keyword.isEmpty()) {
            Set<Integer> matchingIds = new HashSet<>();
            for (Visit visit : service.findVisits(keyword)) { // overloaded search by text
                matchingIds.add(visit.getVisitId());
            }
            shown.removeIf(visit -> !matchingIds.contains(visit.getVisitId()));
        }

        tableModel.setVisits(shown);
        lblCount.setText("Showing " + shown.size() + " of " + service.getVisits().size() + " records");
        updateButtons();
    }

    /** The selected record, or null. The table may be sorted, so the row must be converted first. */
    private Visit getSelectedVisit() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            return null;
        }
        return tableModel.getVisitAt(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        Visit visit = getSelectedVisit();
        btnCheckIn.setEnabled(service.canChangeTo(visit, VisitStatus.IN_USE));
        btnComplete.setEnabled(service.canChangeTo(visit, VisitStatus.COMPLETED));
        btnCancel.setEnabled(service.canChangeTo(visit, VisitStatus.CANCELLED));
        btnNoShow.setEnabled(service.canChangeTo(visit, VisitStatus.NO_SHOW));
        btnReschedule.setEnabled(service.canReschedule(visit));
        btnEditPurpose.setEnabled(visit != null);
        btnDelete.setEnabled(service.canDelete(visit));
    }

    private String describe(Visit visit) {
        return visit.getReference() + " - " + visit.getClient().getFullName();
    }

    private void say(String message) {
        lblMessage.setText(message);
    }
}
