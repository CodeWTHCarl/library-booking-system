package ui;

import model.BookedReservation;
import model.Client;
import model.Resource;
import model.ResourceType;
import model.TimeSlot;
import model.Visit;
import model.VisitStatus;
import model.VisitType;
import service.LibraryException;
import service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The "Book a Slot" tab: reserve a study room or computer station for a fixed time slot. */
public class BookingPanel extends ServicePanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    private final JTextField txtStudentId = new JTextField(18);
    private final JTextField txtFullName = new JTextField(18);
    private final JTextField txtContact = new JTextField(18);
    private final JComboBox<ResourceType> cmbType = new JComboBox<>(ResourceType.values());
    private final JComboBox<Resource> cmbResource = new JComboBox<>();
    private final JComboBox<LocalDate> cmbDate = new JComboBox<>();
    private final JComboBox<TimeSlot> cmbSlot = new JComboBox<>();
    private final JTextField txtPurpose = new JTextField(18);
    private final JLabel lblResourceInfo = new JLabel(" ");
    private final JLabel lblSlotNote = new JLabel(" ");
    private final JButton btnBook = new JButton("Book Slot");
    private final JButton btnClear = new JButton("Clear");
    private final VisitTableModel tableModel = new VisitTableModel();

    // true while the code itself is refilling a combo box, so that it does not trigger the listeners
    private boolean updating = false;

    public BookingPanel(LibraryService service) {
        super(service);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        wireEvents();

        loadDates();
        updateResources();
        refreshTable();
    }

    // =====================================================================================
    //  BUILDING THE SCREEN
    // =====================================================================================

    private JPanel buildForm() {
        JPanel clientBox = new JPanel(new GridBagLayout());
        clientBox.setBorder(new TitledBorder("Client"));
        addRow(clientBox, 0, "Student ID:", txtStudentId);
        addRow(clientBox, 1, "Full name:", txtFullName);
        addRow(clientBox, 2, "Contact no.:", txtContact);
        addFiller(clientBox, 3);

        JPanel reservationBox = new JPanel(new GridBagLayout());
        reservationBox.setBorder(new TitledBorder("Reservation"));
        addRow(reservationBox, 0, "Type:", cmbType);
        addRow(reservationBox, 1, "Resource:", cmbResource);
        addRow(reservationBox, 2, "", lblResourceInfo);
        addRow(reservationBox, 3, "Date:", cmbDate);
        addRow(reservationBox, 4, "Time slot:", cmbSlot);
        addRow(reservationBox, 5, "", lblSlotNote);
        addRow(reservationBox, 6, "Purpose:", txtPurpose);
        addFiller(reservationBox, 7);

        lblResourceInfo.setFont(lblResourceInfo.getFont().deriveFont(Font.ITALIC));
        lblSlotNote.setForeground(new java.awt.Color(180, 40, 40));

        cmbDate.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LocalDate) {
                    LocalDate date = (LocalDate) value;
                    setText(DATE_FORMAT.format(date) + (date.equals(LocalDate.now()) ? "  (today)" : ""));
                }
                return this;
            }
        });

        JPanel boxes = new JPanel(new GridLayout(1, 2, 10, 0));
        boxes.add(clientBox);
        boxes.add(reservationBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnClear);
        buttons.add(btnBook);

        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.add(boxes, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }

    private JScrollPane buildTable() {
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        VisitTableModel.applyColumnWidths(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Upcoming reservations"));
        return scroll;
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
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
    private void addFiller(JPanel panel, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.weighty = 1;
        panel.add(new JLabel(), gbc);
    }

    // =====================================================================================
    //  EVENT HANDLING
    // =====================================================================================

    private void wireEvents() {
        cmbType.addActionListener(e -> {
            if (!updating) {
                updateResources();
            }
        });
        cmbResource.addActionListener(e -> {
            if (!updating) {
                updateResourceInfo();
                updateSlots();
            }
        });
        cmbDate.addActionListener(e -> {
            if (!updating) {
                updateSlots();
            }
        });
        btnBook.addActionListener(e -> onBook());
        btnClear.addActionListener(e -> clearForm());

        // typing a known student ID fills in the saved name and contact number
        txtStudentId.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                autofillClient();
            }
        });
    }

    private void onBook() {
        try {
            BookedReservation booking = service.bookSlot(
                    txtFullName.getText(),
                    txtStudentId.getText(),
                    txtContact.getText(),
                    (Resource) cmbResource.getSelectedItem(),
                    (LocalDate) cmbDate.getSelectedItem(),
                    (TimeSlot) cmbSlot.getSelectedItem(),
                    txtPurpose.getText());

            showInfo("Booking confirmed!\n\n"
                    + "Client:  " + booking.getClient().getFullName() + "\n"
                    + "Resource:  " + booking.getResource().getName() + "\n"
                    + "Date:  " + DATE_FORMAT.format(booking.getVisitDate()) + "\n"
                    + "Time:  " + booking.getReference());

            clearClientFields();
            updateSlots();
            refreshTable();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void autofillClient() {
        Client client = service.findClient(txtStudentId.getText());
        if (client != null) {
            txtFullName.setText(client.getFullName());
            txtContact.setText(client.getContactNo());
        }
    }

    private void clearClientFields() {
        txtStudentId.setText("");
        txtFullName.setText("");
        txtContact.setText("");
        txtPurpose.setText("");
    }

    private void clearForm() {
        clearClientFields();
        lblSlotNote.setText(" ");
        updateSlots();
    }

    // =====================================================================================
    //  KEEPING THE COMBO BOXES AND TABLE UP TO DATE
    // =====================================================================================

    @Override
    public void refresh() {
        if (!LocalDate.now().equals(cmbDate.getItemAt(0))) {
            loadDates(); // the app was left open past midnight
        }
        updateResources();
        refreshTable();
    }

    private void loadDates() {
        LocalDate previous = (LocalDate) cmbDate.getSelectedItem();
        LocalDate today = LocalDate.now();
        updating = true;
        cmbDate.removeAllItems();
        for (int i = 0; i <= LibraryService.MAX_DAYS_AHEAD; i++) {
            cmbDate.addItem(today.plusDays(i));
        }
        if (previous != null && !previous.isBefore(today)) {
            cmbDate.setSelectedItem(previous);
        }
        updating = false;
    }

    private void updateResources() {
        Resource previous = (Resource) cmbResource.getSelectedItem();
        ResourceType type = (ResourceType) cmbType.getSelectedItem();
        updating = true;
        cmbResource.removeAllItems();
        for (Resource resource : service.getResources(type)) {
            if (resource.isAvailable()) {
                cmbResource.addItem(resource);
                if (previous != null && resource.getResourceId() == previous.getResourceId()) {
                    cmbResource.setSelectedItem(resource);
                }
            }
        }
        updating = false;
        updateResourceInfo();
        updateSlots();
    }

    private void updateResourceInfo() {
        Resource resource = (Resource) cmbResource.getSelectedItem();
        if (resource == null) {
            lblResourceInfo.setText(" ");
        } else {
            // getUsageRules() gives a different text for a room and for a computer (polymorphism)
            lblResourceInfo.setText("<html><body style='width:230px'>" + resource.getUsageRules()
                    + "</body></html>");
        }
    }

    private void updateSlots() {
        Resource resource = (Resource) cmbResource.getSelectedItem();
        LocalDate date = (LocalDate) cmbDate.getSelectedItem();
        TimeSlot previous = (TimeSlot) cmbSlot.getSelectedItem();

        cmbSlot.removeAllItems();
        for (TimeSlot slot : service.getOpenSlots(resource, date)) {
            cmbSlot.addItem(slot);
            if (slot == previous) {
                cmbSlot.setSelectedItem(slot);
            }
        }
        boolean hasSlots = cmbSlot.getItemCount() > 0;
        lblSlotNote.setText(hasSlots ? " " : "No open slots on this date. Try another date.");
        btnBook.setEnabled(hasSlots);
    }

    private void refreshTable() {
        List<Visit> upcoming = new ArrayList<>();
        for (Visit visit : service.filterVisits(VisitType.BOOKED, VisitStatus.BOOKED, null)) {
            if (!visit.getVisitDate().isBefore(LocalDate.now())) {
                upcoming.add(visit);
            }
        }
        upcoming.sort(Comparator.comparing(Visit::getVisitDate)
                .thenComparing(visit -> ((BookedReservation) visit).getTimeSlot()));
        tableModel.setVisits(upcoming);
    }
}
