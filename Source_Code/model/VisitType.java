package model;

/** How the client is being served: with a reservation, or as a walk-in. */
public enum VisitType {
    BOOKED("Booked"),
    WALKIN("Walk-in");

    private final String label;

    VisitType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label; // what combo boxes and messages show; MySQL still stores name()
    }
}
