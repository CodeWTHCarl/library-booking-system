package ui;

import model.Visit;
import model.VisitType;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells a JTable how to show a list of visits. The same model is reused by the Booking,
 * Walk-in and Records tabs. Cells are read-only.
 */
public class VisitTableModel extends AbstractTableModel {
    private static final String[] COLUMNS =
            {"Type", "Reference", "Client", "Student ID", "Resource", "Date", "Status", "Purpose"};

    private List<Visit> visits = new ArrayList<>();

    /** Gives each column a sensible width. Every tab that shows visits calls this once. */
    public static void applyColumnWidths(JTable table) {
        int[] widths = {70, 170, 150, 100, 110, 100, 90, 220};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public void setVisits(List<Visit> newVisits) {
        visits = new ArrayList<>(newVisits);
        fireTableDataChanged(); // tells the JTable to redraw
    }

    public Visit getVisitAt(int row) {
        return visits.get(row);
    }

    @Override
    public int getRowCount() {
        return visits.size();
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
        Visit visit = visits.get(row);
        switch (column) {
            case 0:
                return visit.getVisitType() == VisitType.BOOKED ? "Booked" : "Walk-in";
            case 1:
                return visit.getReference(); // time slot for a booking, queue number for a walk-in
            case 2:
                return visit.getClient().getFullName();
            case 3:
                return visit.getClient().getStudentId();
            case 4:
                return (visit.getResource() == null) ? "Not yet assigned" : visit.getResource().getName();
            case 5:
                return visit.getVisitDate();
            case 6:
                return visit.getStatus();
            default:
                return visit.getPurpose();
        }
    }
}
