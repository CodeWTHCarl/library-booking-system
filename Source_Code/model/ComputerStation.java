package model;

/** A single computer station. Child of Resource (INHERITANCE). */
public class ComputerStation extends Resource {

    public ComputerStation(int resourceId, String name, boolean available) {
        super(resourceId, name, available);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.COMPUTER;
    }

    @Override
    public int getMaxUsers() {
        return 1;
    }

    @Override
    public String getUsageRules() {
        return "Single user only. For research and schoolwork.";
    }
}
