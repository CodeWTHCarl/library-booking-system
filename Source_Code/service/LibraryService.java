package service;

import data.ClientRepository;
import data.ResourceRepository;
import data.VisitRepository;
import model.BookedReservation;
import model.Client;
import model.Resource;
import model.ResourceType;
import model.TimeSlot;
import model.Visit;
import model.VisitStatus;
import model.VisitType;
import model.WalkInTicket;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The brain of the system. The UI only talks to this class; this class applies the rules
 * and uses the repositories to save to MySQL.
 *
 * The three ArrayLists below are the records the app works with while it runs.
 * Every change is saved to MySQL first, then the lists are reloaded so they always match the database.
 */
public class LibraryService {
    /** How many days ahead a booking may be made (the Booking tab offers exactly this many days). */
    public static final int MAX_DAYS_AHEAD = 30;

    private final ClientRepository clientRepository;
    private final ResourceRepository resourceRepository;
    private final VisitRepository visitRepository;

    private List<Client> clients = new ArrayList<>();
    private List<Resource> resources = new ArrayList<>();
    private List<Visit> visits = new ArrayList<>();

    public LibraryService() {
        this(new ClientRepository(), new ResourceRepository(), new VisitRepository());
    }

    /** Lets the repositories be swapped (for example by a test). */
    public LibraryService(ClientRepository clientRepository, ResourceRepository resourceRepository,
                          VisitRepository visitRepository) {
        this.clientRepository = clientRepository;
        this.resourceRepository = resourceRepository;
        this.visitRepository = visitRepository;
    }

    // Overridden only by tests, so they can pretend it is a different day or hour.
    protected LocalDate today() {
        return LocalDate.now();
    }

    protected LocalTime now() {
        return LocalTime.now();
    }

    // =====================================================================================
    //  LOADING AND READING  (Read)
    // =====================================================================================

    /** Reloads all three lists from MySQL. Call once at startup. */
    public void loadAll() throws LibraryException {
        try {
            clients = clientRepository.getAll();
            resources = resourceRepository.getAll();
            visits = visitRepository.getAll();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    // The lists are replaced after every change, so the UI should call these getters again to refresh its table.
    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    /** Overloaded: only the resources of one type (rooms or computers). */
    public List<Resource> getResources(ResourceType type) {
        List<Resource> result = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource.getResourceType() == type) {
                result.add(resource);
            }
        }
        return result;
    }

    public List<Visit> getVisits() {
        return Collections.unmodifiableList(visits);
    }

    /** Looks up a saved client by student ID. Returns null if there is none. */
    public Client findClient(String studentId) {
        if (studentId == null) {
            return null;
        }
        String wanted = studentId.trim();
        for (Client client : clients) {
            if (client.getStudentId().equalsIgnoreCase(wanted)) {
                return client;
            }
        }
        return null;
    }

    // =====================================================================================
    //  BOOKED CLIENTS  (Create)
    // =====================================================================================

    /** Reserves a resource for a fixed time slot. Registers the client too if the student ID is new. */
    public BookedReservation bookSlot(String fullName, String studentId, String contactNo,
                                      Resource resource, LocalDate date, TimeSlot slot,
                                      String purpose) throws LibraryException {
        Client client = prepareClient(fullName, studentId, contactNo);
        validateBooking(client.getStudentId(), resource, date, slot, purpose, null);
        saveClientIfNew(client);

        BookedReservation booking;
        try {
            booking = new BookedReservation(0, client, resource, date, slot, purpose, VisitStatus.BOOKED);
        } catch (IllegalArgumentException e) {
            throw new LibraryException(e.getMessage());
        }
        addVisit(booking);
        return booking;
    }

    /** The slots of that resource on that date that can still be booked (for the slot combo box). */
    public List<TimeSlot> getOpenSlots(Resource resource, LocalDate date) {
        List<TimeSlot> open = new ArrayList<>();
        if (resource == null || date == null) {
            return open;
        }
        for (TimeSlot slot : TimeSlot.values()) {
            boolean alreadyOver = date.equals(today()) && slot.hasEnded(now());
            if (!alreadyOver && !isSlotTaken(resource, date, slot, null)) {
                open.add(slot);
            }
        }
        return open;
    }

    // =====================================================================================
    //  WALK-INS  (Create)
    // =====================================================================================

    /** Puts a walk-in in line. The queue number (R-001, C-001, ...) restarts every day. */
    public WalkInTicket registerWalkIn(String fullName, String studentId, String contactNo,
                                       ResourceType requestedType, String purpose) throws LibraryException {
        Client client = prepareClient(fullName, studentId, contactNo);
        if (requestedType == null) {
            throw new LibraryException("Please choose a study room or a computer station.");
        }
        requireText(purpose, "Purpose");

        Visit active = findActiveVisit(client.getStudentId(), requestedType, null);
        if (active != null) {
            throw alreadyActive(active);
        }
        saveClientIfNew(client);

        int queueNumber;
        try {
            queueNumber = visitRepository.nextQueueNumber(today(), requestedType);
        } catch (SQLException e) {
            throw databaseError(e);
        }

        WalkInTicket ticket;
        try {
            ticket = new WalkInTicket(0, client, null, today(), requestedType, queueNumber,
                    purpose, VisitStatus.WAITING);
        } catch (IllegalArgumentException e) {
            throw new LibraryException(e.getMessage());
        }
        addVisit(ticket);
        return ticket;
    }

    /** Today's waiting walk-ins of one type, first in line first. */
    public List<WalkInTicket> getWaitingQueue(ResourceType type) {
        List<WalkInTicket> queue = new ArrayList<>();
        for (Visit visit : visits) {
            if (visit instanceof WalkInTicket) {
                WalkInTicket ticket = (WalkInTicket) visit;
                if (ticket.getRequestedType() == type
                        && ticket.getStatus() == VisitStatus.WAITING
                        && ticket.getVisitDate().equals(today())) {
                    queue.add(ticket);
                }
            }
        }
        queue.sort(Comparator.comparingInt(WalkInTicket::getQueueNumber));
        return queue;
    }

    /** Resources of that type that nobody is using or has reserved for the slot running right now. */
    public List<Resource> getFreeResources(ResourceType type) {
        List<Resource> free = new ArrayList<>();
        for (Resource resource : getResources(type)) {
            if (isFreeNow(resource)) {
                free.add(resource);
            }
        }
        return free;
    }

    /** Staff action: the first walk-in in line gets the first free resource of the type. */
    public WalkInTicket assignNext(ResourceType type) throws LibraryException {
        List<WalkInTicket> queue = getWaitingQueue(type);
        if (queue.isEmpty()) {
            throw new LibraryException("Nobody is waiting for a " + type + ".");
        }
        List<Resource> free = getFreeResources(type);
        if (free.isEmpty()) {
            throw new LibraryException("No " + type + " is free right now.");
        }
        WalkInTicket ticket = queue.get(0);
        Visit active = findActiveVisit(ticket.getClient().getStudentId(), type, ticket);
        if (active != null) {
            throw new LibraryException("Can't serve " + ticket.getReference() + ": "
                    + alreadyActive(active).getMessage());
        }
        ticket.assignResource(free.get(0)); // the model enforces "right type of resource"
        saveChanges(ticket);
        return ticket;
    }

    // =====================================================================================
    //  UPDATE AND DELETE
    // =====================================================================================

    /** Moves a status forward (check-in, complete, cancel, no-show). Illegal jumps are rejected. */
    public void changeStatus(Visit visit, VisitStatus newStatus) throws LibraryException {
        requireSelected(visit);
        if (visit instanceof WalkInTicket && newStatus == VisitStatus.IN_USE) {
            throw new LibraryException("Use \"Assign next\" to start a walk-in.");
        }
        if (!canChangeStatus(visit.getStatus(), newStatus)) {
            throw new LibraryException("A visit marked " + visit.getStatus()
                    + " can't be changed to " + newStatus + ".");
        }
        if (newStatus == VisitStatus.IN_USE) {
            if (!visit.getVisitDate().equals(today())) {
                throw new LibraryException("A booking can only be checked in on its own date.");
            }
            Visit active = findActiveVisit(visit.getClient().getStudentId(), visit.getResourceType(), visit);
            if (active != null) {
                throw alreadyActive(active);
            }
            if (isInUseToday(visit.getResource(), visit)) {
                throw new LibraryException(visit.getResource().getName()
                        + " is still being used by another client.");
            }
        }
        visit.setStatus(newStatus);
        saveChanges(visit);
    }

    /** Changes the resource, date, slot or purpose of a reservation that has not started yet. */
    public void reschedule(BookedReservation booking, Resource newResource, LocalDate newDate,
                           TimeSlot newSlot, String newPurpose) throws LibraryException {
        requireSelected(booking);
        if (booking.getStatus() != VisitStatus.BOOKED) {
            throw new LibraryException("Only a BOOKED reservation can be edited.");
        }
        validateBooking(booking.getClient().getStudentId(), newResource, newDate, newSlot,
                newPurpose, booking);
        booking.setResource(newResource);
        booking.setVisitDate(newDate);
        booking.setTimeSlot(newSlot);
        booking.setPurpose(newPurpose);
        saveChanges(booking);
    }

    /** Edits the purpose of any visit (this is the only editable field of a walk-in). */
    public void updatePurpose(Visit visit, String newPurpose) throws LibraryException {
        requireSelected(visit);
        requireText(newPurpose, "Purpose");
        visit.setPurpose(newPurpose);
        saveChanges(visit);
    }

    /** Removes a record for good. (To keep a record but end it, use changeStatus with CANCELLED.) */
    public void deleteVisit(Visit visit) throws LibraryException {
        requireSelected(visit);
        if (visit.getStatus() == VisitStatus.IN_USE) {
            throw new LibraryException("This visit is in use. Complete it first.");
        }
        try {
            visitRepository.delete(visit.getVisitId());
        } catch (SQLException e) {
            throw databaseError(e);
        }
        loadAll();
    }

    // =====================================================================================
    //  SEARCH AND FILTER  (method overloading: same name, different parameter)
    // =====================================================================================

    /** Matches the keyword against name, student ID, reference, resource and purpose. */
    public List<Visit> findVisits(String keyword) {
        String needle = (keyword == null) ? "" : keyword.trim().toLowerCase();
        List<Visit> result = new ArrayList<>();
        for (Visit visit : visits) {
            String resourceName = (visit.getResource() == null) ? "" : visit.getResource().getName();
            String text = (visit.getClient().getFullName() + " " + visit.getClient().getStudentId()
                    + " " + visit.getReference() + " " + resourceName + " " + visit.getPurpose())
                    .toLowerCase();
            if (text.contains(needle)) {
                result.add(visit);
            }
        }
        return result;
    }

    public List<Visit> findVisits(LocalDate date) {
        List<Visit> result = new ArrayList<>();
        for (Visit visit : visits) {
            if (visit.getVisitDate().equals(date)) {
                result.add(visit);
            }
        }
        return result;
    }

    /** For the filter boxes of the Records tab. A null argument means "any". */
    public List<Visit> filterVisits(VisitType type, VisitStatus status, LocalDate date) {
        List<Visit> result = new ArrayList<>();
        for (Visit visit : visits) {
            boolean typeOk = (type == null) || visit.getVisitType() == type;
            boolean statusOk = (status == null) || visit.getStatus() == status;
            boolean dateOk = (date == null) || visit.getVisitDate().equals(date);
            if (typeOk && statusOk && dateOk) {
                result.add(visit);
            }
        }
        return result;
    }

    // =====================================================================================
    //  PRIVATE HELPERS
    // =====================================================================================

    /**
     * Checks the typed client details and returns the matching saved client (same student ID),
     * or a new unsaved one. Nothing is written to the database yet.
     */
    private Client prepareClient(String fullName, String studentId, String contactNo)
            throws LibraryException {
        try {
            Client typed = new Client(fullName, studentId, contactNo); // validates the three fields
            Client existing = findClient(typed.getStudentId());
            return (existing != null) ? existing : typed;
        } catch (IllegalArgumentException e) {
            throw new LibraryException(e.getMessage());
        }
    }

    private void saveClientIfNew(Client client) throws LibraryException {
        if (client.getClientId() != 0) {
            return; // already in the database
        }
        try {
            clientRepository.add(client);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /** All the booking rules in one place, used both for new bookings and for rescheduling. */
    private void validateBooking(String studentId, Resource resource, LocalDate date, TimeSlot slot,
                                 String purpose, Visit ignore) throws LibraryException {
        if (resource == null) {
            throw new LibraryException("Please choose a resource.");
        }
        if (date == null) {
            throw new LibraryException("Please choose a date.");
        }
        if (slot == null) {
            throw new LibraryException("Please choose a time slot.");
        }
        requireText(purpose, "Purpose");

        if (!resource.isAvailable()) {
            throw new LibraryException(resource.getName() + " is out of service.");
        }
        if (date.isBefore(today())) {
            throw new LibraryException("You can't book a date that has already passed.");
        }
        if (date.isAfter(today().plusDays(MAX_DAYS_AHEAD))) {
            throw new LibraryException("Bookings can only be made up to " + MAX_DAYS_AHEAD + " days ahead.");
        }
        if (date.equals(today()) && slot.hasEnded(now())) {
            throw new LibraryException("The " + slot + " slot is already over today.");
        }
        if (isSlotTaken(resource, date, slot, ignore)) {
            throw new LibraryException(resource.getName() + " is already booked for " + slot
                    + " on " + date + ".");
        }
        for (Visit visit : visits) {
            if (visit instanceof BookedReservation && !isSame(visit, ignore)) {
                BookedReservation other = (BookedReservation) visit;
                if (other.getClient().getStudentId().equalsIgnoreCase(studentId)
                        && other.getVisitDate().equals(date)
                        && other.getTimeSlot() == slot
                        && occupiesSlot(other.getStatus())) {
                    throw new LibraryException(other.getClient().getFullName()
                            + " already has a booking for " + slot + " on " + date + ".");
                }
            }
        }
    }

    private boolean isSlotTaken(Resource resource, LocalDate date, TimeSlot slot, Visit ignore) {
        for (Visit visit : visits) {
            if (visit instanceof BookedReservation && !isSame(visit, ignore)) {
                BookedReservation booking = (BookedReservation) visit;
                if (booking.getResource().getResourceId() == resource.getResourceId()
                        && booking.getVisitDate().equals(date)
                        && booking.getTimeSlot() == slot
                        && occupiesSlot(booking.getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Cancelled and no-show bookings give their slot back; all others keep it. */
    private boolean occupiesSlot(VisitStatus status) {
        return status == VisitStatus.BOOKED || status == VisitStatus.IN_USE
                || status == VisitStatus.COMPLETED;
    }

    private boolean isFreeNow(Resource resource) {
        if (!resource.isAvailable() || isInUseToday(resource, null)) {
            return false;
        }
        TimeSlot current = TimeSlot.at(now());
        if (current == null) {
            return true; // outside slot hours, so no reservation can be waiting
        }
        for (Visit visit : visits) {
            if (visit instanceof BookedReservation && visit.getStatus() == VisitStatus.BOOKED
                    && visit.getResource().getResourceId() == resource.getResourceId()
                    && visit.getVisitDate().equals(today())
                    && ((BookedReservation) visit).getTimeSlot() == current) {
                return false; // reserved for the slot that is running now
            }
        }
        return true;
    }

    private boolean isInUseToday(Resource resource, Visit ignore) {
        for (Visit visit : visits) {
            if (!isSame(visit, ignore) && visit.getStatus() == VisitStatus.IN_USE
                    && visit.getResource() != null
                    && visit.getResource().getResourceId() == resource.getResourceId()
                    && visit.getVisitDate().equals(today())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The rule "one active visit per resource type": a client may be WAITING or IN_USE for only one
     * study room (and only one computer) at a time. Returns that active visit, or null if there is none.
     */
    private Visit findActiveVisit(String studentId, ResourceType type, Visit ignore) {
        for (Visit visit : visits) {
            boolean active = visit.getStatus() == VisitStatus.WAITING
                    || visit.getStatus() == VisitStatus.IN_USE;
            if (active && !isSame(visit, ignore)
                    && visit.getResourceType() == type // polymorphism: works for both kinds of visit
                    && visit.getVisitDate().equals(today())
                    && visit.getClient().getStudentId().equalsIgnoreCase(studentId)) {
                return visit;
            }
        }
        return null;
    }

    private LibraryException alreadyActive(Visit active) {
        String name = active.getClient().getFullName();
        if (active.getStatus() == VisitStatus.WAITING) {
            return new LibraryException(name + " is already in the queue as " + active.getReference() + ".");
        }
        return new LibraryException(name + " is already using " + active.getResource().getName()
                + ". Complete that visit first.");
    }

    /** The allowed moves: BOOKED -> IN_USE/CANCELLED/NO_SHOW, WAITING -> CANCELLED/NO_SHOW, IN_USE -> COMPLETED. */
    private boolean canChangeStatus(VisitStatus from, VisitStatus to) {
        switch (from) {
            case BOOKED:
                return to == VisitStatus.IN_USE || to == VisitStatus.CANCELLED || to == VisitStatus.NO_SHOW;
            case WAITING:
                return to == VisitStatus.CANCELLED || to == VisitStatus.NO_SHOW;
            case IN_USE:
                return to == VisitStatus.COMPLETED;
            default:
                return false; // COMPLETED, CANCELLED and NO_SHOW are final
        }
    }

    private boolean isSame(Visit a, Visit b) {
        return b != null && a.getVisitId() == b.getVisitId();
    }

    private void requireSelected(Visit visit) throws LibraryException {
        if (visit == null) {
            throw new LibraryException("Please select a record first.");
        }
    }

    private void requireText(String value, String fieldName) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException(fieldName + " must not be empty.");
        }
    }

    private void addVisit(Visit visit) throws LibraryException {
        try {
            visitRepository.add(visit);
        } catch (SQLException e) {
            throw databaseError(e);
        }
        loadAll();
    }

    /** Saves an edited visit, then reloads. If saving fails, reload anyway so memory matches the database. */
    private void saveChanges(Visit visit) throws LibraryException {
        try {
            visitRepository.update(visit);
        } catch (SQLException e) {
            try {
                loadAll();
            } catch (LibraryException ignored) {
                // the original error below is the useful one
            }
            throw databaseError(e);
        }
        loadAll();
    }

    private LibraryException databaseError(SQLException e) {
        return new LibraryException("Database error: " + e.getMessage(), e);
    }
}
