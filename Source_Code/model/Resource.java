package model;

/**
 * ABSTRACTION: a generic bookable resource. You can never create a plain Resource,
 * only a StudyRoom or a ComputerStation (INHERITANCE).
 */
public abstract class Resource {
    private int resourceId;
    private String name;
    private boolean available;

    protected Resource(int resourceId, String name, boolean available) {
        this.resourceId = resourceId;
        setName(name);
        this.available = available;
    }

    // Each child class must decide these for itself (POLYMORPHISM through overriding).
    public abstract ResourceType getResourceType();

    public abstract int getMaxUsers();

    public abstract String getUsageRules();

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name must not be empty.");
        }
        this.name = name.trim();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return name; // what a JComboBox will show
    }
}
