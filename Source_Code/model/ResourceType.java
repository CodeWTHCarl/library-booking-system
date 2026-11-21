package model;

/** The two kinds of bookable resources. The prefix is used to build queue numbers (R-001, C-001). */
public enum ResourceType {
    ROOM("R", "Study Room"),
    COMPUTER("C", "Computer Station");

    private final String prefix;
    private final String displayName;

    ResourceType(String prefix, String displayName) {
        this.prefix = prefix;
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return displayName; // what a JComboBox will show
    }
}
