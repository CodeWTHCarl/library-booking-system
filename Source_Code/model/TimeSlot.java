package model;

/** Fixed 2-hour slots. To change library hours or slot length, only this file needs editing. */
public enum TimeSlot {
    SLOT_1("8:00 AM - 10:00 AM"),
    SLOT_2("10:00 AM - 12:00 PM"),
    SLOT_3("12:00 PM - 2:00 PM"),
    SLOT_4("2:00 PM - 4:00 PM"),
    SLOT_5("4:00 PM - 6:00 PM");

    private final String label;

    TimeSlot(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // what a JComboBox will show
    }
}
