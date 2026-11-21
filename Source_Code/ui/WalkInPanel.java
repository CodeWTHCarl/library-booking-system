package ui;

import model.Resource;
import model.ResourceType;
import model.Visit;
import model.VisitStatus;
import model.WalkInTicket;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The "Walk-in Queue" tab. The front desk registers clients who have no reservation and gets them a
 * queue number; staff then press "Assign next" to give the first in line a free resource.
 */
public class WalkInPanel extends ServicePanel {
    // registration form
    private final JTextField txtStudentId = new JTextField(18);
    private final JTextField txtFullName = new JTextField(18);
    private final JTextField txtContact = new JTextField(18);
    private final JComboBox<ResourceType> cmbType = new JComboBox<>(ResourceType.values());
    private final JTextField txtPurpose = new JTextField(18);
    private final JButton btnRegister = new JButton("Get Queue Number");
    private final JButton btnClear = new JButton("Clear");

    // the two waiting lines
    private final QueueTableModel roomQueueModel = new QueueTableModel();
    private final QueueTableModel computerQueueModel = new QueueTableModel();
    private final TitledBorder roomQueueTitle = new TitledBorder("");
    private final TitledBorder computerQueueTitle = new TitledBorder("");
    private final JLabel lblRoomFree = new JLabel(" ");
    private final JLabel lblComputerFree = new JLabel(" ");
    private final JButton btnAssignRoom = new JButton("Assign next: Study Room");
    private final JButton btnAssignComputer = new JButton("Assign next: Computer Station");

    // who is using something right now
    private final VisitTableModel inUseModel = new VisitTableModel();
    private final JTable inUseTable = new JTable(inUseModel);
    private final JButton btnComplete = new JButton("Mark Selected as Completed");

    public WalkInPanel(LibraryService service) {
        super(service);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        add(buildForm(), BorderLayout.NORTH);
        add(buildQueues(), BorderLayout.CENTER);
        add(buildInUse(), BorderLayout.SOUTH);
        wireEvents();
        refresh();
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

        JPanel requestBox = new JPanel(new GridBagLayout());
        requestBox.setBorder(new TitledBorder("What does the client need?"));
        addRow(requestBox, 0, "Type:", cmbType);
        addRow(requestBox, 1, "Purpose:", txtPurpose);
        addFiller(requestBox, 2);

        JPanel boxes = new JPanel(new GridLayout(1, 2, 10, 0));
        boxes.add(clientBox);
        boxes.add(requestBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnClear);
        buttons.add(btnRegister);

        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.add(boxes, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }

    private JPanel buildQueues() {
        JPanel queues = new JPanel(new GridLayout(1, 2, 10, 0));
        queues.add(buildQueueBox(roomQueueTitle, roomQueueModel, lblRoomFree, btnAssignRoom));
        queues.add(buildQueueBox(computerQueueTitle, computerQueueModel, lblComputerFree, btnAssignComputer));
        return queues;
    }

    private JPanel buildQueueBox(TitledBorder title, QueueTableModel model, JLabel freeLabel,
                                 JButton assignButton) {
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {70, 130, 90, 150};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.add(freeLabel, BorderLayout.CENTER);
        bottom.add(assignButton, BorderLayout.EAST);

        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setBorder(title);
        box.add(new JScrollPane(table), BorderLayout.CENTER);
        box.add(bottom, BorderLayout.SOUTH);
        return box;
    }

    private JPanel buildInUse() {
        inUseTable.setFillsViewportHeight(true);
        inUseTable.setRowHeight(24);
        inUseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        VisitTableModel.applyColumnWidths(inUseTable);

        JScrollPane scroll = new JScrollPane(inUseTable);
        scroll.setPreferredSize(new Dimension(0, 130));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnComplete);

        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setBorder(new TitledBorder("In use right now (today)"));
        box.add(scroll, BorderLayout.CENTER);
        box.add(buttons, BorderLayout.SOUTH);
        return box;
    }

    // =====================================================================================
    //  EVENT HANDLING
    // =====================================================================================

    private void wireEvents() {
        btnRegister.addActionListener(e -> onRegister());
        btnClear.addActionListener(e -> clearForm());
        btnAssignRoom.addActionListener(e -> onAssign(ResourceType.ROOM));
        btnAssignComputer.addActionListener(e -> onAssign(ResourceType.COMPUTER));
        btnComplete.addActionListener(e -> onComplete());

        txtStudentId.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                autofillClient(txtStudentId, txtFullName, txtContact);
            }
        });
    }

    private void onRegister() {
        try {
            ResourceType type = (ResourceType) cmbType.getSelectedItem();
            WalkInTicket ticket = service.registerWalkIn(
                    txtFullName.getText(),
                    txtStudentId.getText(),
                    txtContact.getText(),
                    type,
                    txtPurpose.getText());

            int peopleAhead = service.getWaitingQueue(type).size() - 1; // everyone waiting except this ticket
            showInfo("Queue number:  " + ticket.getReference() + "\n\n"
                    + "Client:  " + ticket.getClient().getFullName() + "\n"
                    + "Waiting for:  " + type + "\n"
                    + "People ahead:  " + peopleAhead);

            clearForm();
            refresh();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onAssign(ResourceType type) {
        try {
            WalkInTicket ticket = service.assignNext(type);
            showInfo("Now serving  " + ticket.getReference() + "  (" + ticket.getClient().getFullName() + ")\n\n"
                    + "Assigned to:  " + ticket.getResource().getName());
            refresh();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onComplete() {
        int row = inUseTable.getSelectedRow();
        Visit selected = (row == -1) ? null : inUseModel.getVisitAt(row);
        try {
            service.changeStatus(selected, VisitStatus.COMPLETED); // null gives "Please select a record first."
            refresh();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void clearForm() {
        txtStudentId.setText("");
        txtFullName.setText("");
        txtContact.setText("");
        txtPurpose.setText("");
    }

    // =====================================================================================
    //  KEEPING THE TABLES UP TO DATE
    // =====================================================================================

    @Override
    public void refresh() {
        refreshQueue(ResourceType.ROOM, roomQueueModel, roomQueueTitle, lblRoomFree);
        refreshQueue(ResourceType.COMPUTER, computerQueueModel, computerQueueTitle, lblComputerFree);
        inUseModel.setVisits(service.filterVisits(null, VisitStatus.IN_USE, LocalDate.now()));
    }

    private void refreshQueue(ResourceType type, QueueTableModel model, TitledBorder title, JLabel freeLabel) {
        List<WalkInTicket> queue = service.getWaitingQueue(type);
        model.setTickets(queue);
        title.setTitle(type + " queue (" + queue.size() + " waiting)");

        List<String> freeNames = new ArrayList<>();
        for (Resource resource : service.getFreeResources(type)) {
            freeNames.add(resource.getName());
        }
        freeLabel.setText(freeNames.isEmpty()
                ? "Free now: none"
                : "Free now (" + freeNames.size() + "): " + String.join(", ", freeNames));
        repaint(); // so the new border title is drawn
    }
}
