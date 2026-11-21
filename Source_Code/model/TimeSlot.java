package model;

import java.time.LocalTime;

/** Fixed 2-hour slots. To change library hours or slot length, only this file needs editing. */
public enum TimeSlot {
    SLOT_1("8:00 AM - 10:00 AM", 8, 10),
    SLOT_2("10:00 AM - 12:00 PM", 10, 12),
    SLOT_3("12:00 PM - 2:00 PM", 12, 14),
    SLOT_4("2:00 PM - 4:00 PM", 14, 16),
    SLOT_5("4:00 PM - 6:00 PM", 16, 18);

    private final String label;
    private final int startHour;
    private final int endHour;

    TimeSlot(String label, int startHour, int endHour) {
        this.label = label;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public String getLabel() {
        return label;
    }

    /** True if this slot is already over at the given time of day. */
    public boolean hasEnded(LocalTime time) {
        return time.getHour() >= endHour;
    }

    /** The slot that is running at the given time, or null if the library is outside slot hours. */
    public static TimeSlot at(LocalTime time) {
        for (TimeSlot slot : values()) {
            if (time.getHour() >= slot.startHour && time.getHour() < slot.endHour) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label; // what a JComboBox will show
    }
}
