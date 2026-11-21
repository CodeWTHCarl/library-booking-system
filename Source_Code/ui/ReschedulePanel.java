package ui;

import model.BookedReservation;
import model.Resource;
import model.TimeSlot;
import service.LibraryService;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.time.LocalDate;

/**
 * The form shown when staff reschedule a booking. It is a ServicePanel too (inheritance), and
 * refresh() re-reads the slots that are open for the chosen resource and date.
 */
public class ReschedulePanel extends ServicePanel {
    private final BookedReservation booking;
    private final JComboBox<Resource> cmbResource = new JComboBox<>();
    private final JComboBox<LocalDate> cmbDate = new JComboBox<>();
    private final JComboBox<TimeSlot> cmbSlot = new JComboBox<>();
    private final JTextField txtPurpose;
    private boolean updating = false;

    public ReschedulePanel(LibraryService service, BookedReservation booking) {
        super(service);
        this.booking = booking;
        this.txtPurpose = new JTextField(booking.getPurpose(), 22);

        setLayout(new GridBagLayout());
        addRow(this, 0, "Client:", new JLabel(booking.getClient().getFullName()));
        addRow(this, 1, "Type:", new JLabel(booking.getResourceType().toString()));
        addRow(this, 2, "Resource:", cmbResource);
        addRow(this, 3, "Date:", cmbDate);
        addRow(this, 4, "Time slot:", cmbSlot);
        addRow(this, 5, "Purpose:", txtPurpose);

        cmbDate.setRenderer(new DateListCellRenderer());
        loadResources();
        loadDates();
        refresh();

        cmbResource.addActionListener(e -> {
            if (!updating) {
                refresh();
            }
        });
        cmbDate.addActionListener(e -> {
            if (!updating) {
                refresh();
            }
        });
    }

    private void loadResources() {
        updating = true;
        for (Resource resource : service.getResources(booking.getResourceType())) {
            if (resource.isAvailable()) {
                cmbResource.addItem(resource);
                if (resource.getResourceId() == booking.getResource().getResourceId()) {
                    cmbResource.setSelectedItem(resource);
                }
            }
        }
        updating = false;
    }

    private void loadDates() {
        updating = true;
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= LibraryService.MAX_DAYS_AHEAD; i++) {
            cmbDate.addItem(today.plusDays(i));
        }
        if (!booking.getVisitDate().isBefore(today)) {
            cmbDate.setSelectedItem(booking.getVisitDate());
        }
        updating = false;
    }

    /** Offers the open slots, counting the booking's own current slot as open. */
    @Override
    public void refresh() {
        TimeSlot previous = (TimeSlot) cmbSlot.getSelectedItem();
        TimeSlot wanted = (previous != null) ? previous : booking.getTimeSlot();

        cmbSlot.removeAllItems();
        for (TimeSlot slot : service.getOpenSlots(getSelectedResource(), getSelectedDate(), booking)) {
            cmbSlot.addItem(slot);
            if (slot == wanted) {
                cmbSlot.setSelectedItem(slot);
            }
        }
    }

    public Resource getSelectedResource() {
        return (Resource) cmbResource.getSelectedItem();
    }

    public LocalDate getSelectedDate() {
        return (LocalDate) cmbDate.getSelectedItem();
    }

    public TimeSlot getSelectedSlot() {
        return (TimeSlot) cmbSlot.getSelectedItem();
    }

    public String getPurpose() {
        return txtPurpose.getText();
    }
}
