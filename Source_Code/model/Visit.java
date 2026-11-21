package model;

import java.time.LocalDate;

/**
 * ABSTRACTION: one client's visit to the library. It is either a BookedReservation
 * or a WalkInTicket (INHERITANCE). A Visit "has-a" Client and "has-a" Resource (composition).
 */
public abstract class Visit {
    private int visitId;
    private Client client;
    private Resource resource;      // null while a walk-in is still waiting for a free resource
    private LocalDate visitDate;
    private String purpose;
    private VisitStatus status;

    protected Visit(int visitId, Client client, Resource resource,
                    LocalDate visitDate, String purpose, VisitStatus status) {
        this.visitId = visitId;
        setClient(client);
        this.resource = resource;
        setVisitDate(visitDate);
        setPurpose(purpose);
        setStatus(status);
    }

    // Each child class answers these differently (POLYMORPHISM through overriding).
    public abstract VisitType getVisitType();

    /** What identifies this visit: the time slot for a booking, the queue number for a walk-in. */
    public abstract String getReference();

    /** Which kind of resource this visit is about (a study room or a computer station). */
    public abstract ResourceType getResourceType();

    /** One line summary. It calls getReference(), so the output changes depending on the real object type. */
    public String getDetails() {
        String resourceName = (resource == null) ? "Not yet assigned" : resource.getName();
        return getVisitType() + " | " + getReference() + " | " + client.getFullName()
                + " | " + resourceName + " | " + status;
    }

    public int getVisitId() {
        return visitId;
    }

    public void setVisitId(int visitId) {
        this.visitId = visitId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("A visit needs a client.");
        }
        this.client = client;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        if (visitDate == null) {
            throw new IllegalArgumentException("Visit date must not be empty.");
        }
        this.visitDate = visitDate;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose must not be empty.");
        }
        this.purpose = purpose.trim();
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be empty.");
        }
        this.status = status;
    }
}
