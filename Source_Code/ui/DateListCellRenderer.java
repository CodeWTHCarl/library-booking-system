package ui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Shows a LocalDate in a combo box as "Mon, Nov 23, 2026  (today)". Other values are shown as they are. */
public class DateListCellRenderer extends DefaultListCellRenderer {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof LocalDate) {
            LocalDate date = (LocalDate) value;
            setText(FORMAT.format(date) + (date.equals(LocalDate.now()) ? "  (today)" : ""));
        }
        return this;
    }
}
