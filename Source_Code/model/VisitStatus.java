package model;

/** Life cycle of a visit: BOOKED/WAITING -> IN_USE -> COMPLETED (or CANCELLED / NO_SHOW). */
public enum VisitStatus {
    BOOKED("Booked"),
    WAITING("Waiting"),
    IN_USE("In use"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No-show");

    private final String label;

    VisitStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label; // what tables, combo boxes and messages show; MySQL still stores name()
    }
}
