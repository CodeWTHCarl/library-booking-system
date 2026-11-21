package model;

/** A group study room. Child of Resource (INHERITANCE). */
public class StudyRoom extends Resource {
    private int capacity;

    public StudyRoom(int resourceId, String name, int capacity, boolean available) {
        super(resourceId, name, available);
        setCapacity(capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Room capacity must be at least 1.");
        }
        this.capacity = capacity;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ROOM;
    }

    @Override
    public int getMaxUsers() {
        return capacity;
    }

    @Override
    public String getUsageRules() {
        return "Group study room. Maximum of " + capacity + " users. Keep noise to a minimum.";
    }
}
