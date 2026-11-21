package model;

/** Life cycle of a visit: BOOKED/WAITING -> IN_USE -> COMPLETED (or CANCELLED / NO_SHOW). */
public enum VisitStatus {
    BOOKED,
    WAITING,
    IN_USE,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
