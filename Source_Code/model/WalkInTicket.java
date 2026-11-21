package model;

import java.time.LocalDate;

/** A client without a reservation. Gets a queue number and waits for staff to assign a free resource. Child of Visit. */
public class WalkInTicket extends Visit {
    private ResourceType requestedType;
    private int queueNumber;

    public WalkInTicket(int visitId, Client client, Resource resource, LocalDate visitDate,
                        ResourceType requestedType, int queueNumber, String purpose, VisitStatus status) {
        super(visitId, client, resource, visitDate, purpose, status);
        if (requestedType == null) {
            throw new IllegalArgumentException("Please choose a study room or a computer station.");
        }
        if (queueNumber < 1) {
            throw new IllegalArgumentException("Queue number must be at least 1.");
        }
        this.requestedType = requestedType;
        this.queueNumber = queueNumber;
    }

    public ResourceType getRequestedType() {
        return requestedType;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    /** Staff action: give this waiting ticket a free resource of the type the client asked for. */
    public void assignResource(Resource resource) {
        if (resource.getResourceType() != requestedType) {
            throw new IllegalArgumentException("This client requested a " + requestedType + ".");
        }
        setResource(resource);
        setStatus(VisitStatus.IN_USE);
    }

    @Override
    public VisitType getVisitType() {
        return VisitType.WALKIN;
    }

    @Override
    public String getReference() {
        return requestedType.getPrefix() + "-" + String.format("%03d", queueNumber); // e.g. R-001
    }
}
