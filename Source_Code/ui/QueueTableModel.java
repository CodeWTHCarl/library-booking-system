package ui;

import model.WalkInTicket;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** A compact table model for the waiting lines: queue number, client, student ID and purpose. */
public class QueueTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Queue No.", "Client", "Student ID", "Purpose"};

    private List<WalkInTicket> tickets = new ArrayList<>();

    public void setTickets(List<WalkInTicket> newTickets) {
        tickets = new ArrayList<>(newTickets);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return tickets.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        WalkInTicket ticket = tickets.get(row);
        switch (column) {
            case 0:
                return ticket.getReference(); // R-001, C-004, ...
            case 1:
                return ticket.getClient().getFullName();
            case 2:
                return ticket.getClient().getStudentId();
            default:
                return ticket.getPurpose();
        }
    }
}
