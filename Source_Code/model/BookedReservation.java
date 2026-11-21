package model;

import java.time.LocalDate;

/** A client who reserved a specific resource for a specific time slot. Child of Visit. */
public class BookedReservation extends Visit {
    private TimeSlot timeSlot;

    public BookedReservation(int visitId, Client client, Resource resource, LocalDate visitDate,
                             TimeSlot timeSlot, String purpose, VisitStatus status) {
        super(visitId, client, resource, visitDate, purpose, status);
        if (resource == null) {
            throw new IllegalArgumentException("A reservation needs a resource.");
        }
        setTimeSlot(timeSlot);
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        if (timeSlot == null) {
            throw new IllegalArgumentException("Time slot must not be empty.");
        }
        this.timeSlot = timeSlot;
    }

    @Override
    public VisitType getVisitType() {
        return VisitType.BOOKED;
    }

    @Override
    public String getReference() {
        return timeSlot.getLabel();
    }
}
