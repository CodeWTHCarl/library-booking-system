import data.ClientRepository;
import model.*;
import service.LibraryException;
import service.LibraryService;

import java.time.LocalDate;

/** TEMPORARY Phase 2 checkpoint. Tests the rules of LibraryService and removes its own test data. */
public class ServiceCheck {

    private interface Action {
        void run() throws Exception;
    }

    private static int failures = 0;

    public static void main(String[] args) {
        try {
            int failed = run(new LibraryService(), new ClientRepository());
            System.out.println(failed == 0 ? "SERVICE OK" : failed + " CHECK(S) FAILED");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static int run(LibraryService service, ClientRepository clientRepo) throws Exception {
        failures = 0;
        String stamp = String.valueOf(System.currentTimeMillis());
        final String idA = "SVC-A-" + stamp;
        final String idB = "SVC-B-" + stamp;
        final LocalDate tomorrow = LocalDate.now().plusDays(1);

        service.loadAll();
        final Resource room = service.getResources(ResourceType.ROOM).get(0);
        final Resource room2 = service.getResources(ResourceType.ROOM).get(1);

        try {
            System.out.println("--- Booking rules ---");
            BookedReservation booking = service.bookSlot("Svc Alice", idA, "0917", room, tomorrow,
                    TimeSlot.SLOT_2, "Group study");
            check("A valid booking is saved and gets an id", booking.getVisitId() > 0);
            check("The booked slot is no longer offered",
                    !service.getOpenSlots(room, tomorrow).contains(TimeSlot.SLOT_2));

            expectFailure("Same room + same slot is rejected",
                    () -> service.bookSlot("Svc Bob", idB, "0918", room, tomorrow, TimeSlot.SLOT_2, "Study"));
            expectFailure("A date in the past is rejected",
                    () -> service.bookSlot("Svc Bob", idB, "0918", room2, LocalDate.now().minusDays(1),
                            TimeSlot.SLOT_2, "Study"));
            expectFailure("Same client, same slot, other room is rejected",
                    () -> service.bookSlot("Svc Alice", idA, "0917", room2, tomorrow, TimeSlot.SLOT_2, "Study"));
            expectFailure("A blank name is rejected",
                    () -> service.bookSlot("   ", idB, "0918", room2, tomorrow, TimeSlot.SLOT_3, "Study"));
            expectFailure("A blank purpose is rejected",
                    () -> service.bookSlot("Svc Bob", idB, "0918", room2, tomorrow, TimeSlot.SLOT_3, " "));

            System.out.println("--- Walk-in queue ---");
            WalkInTicket first = service.registerWalkIn("Svc Alice", idA, "0917", ResourceType.COMPUTER, "Research");
            WalkInTicket second = service.registerWalkIn("Svc Bob", idB, "0918", ResourceType.COMPUTER, "Printing");
            System.out.println("   Queue numbers given: " + first.getReference() + " and " + second.getReference());
            check("The second walk-in gets the next queue number",
                    second.getQueueNumber() == first.getQueueNumber() + 1);
            expectFailure("The same client can't queue twice for the same type",
                    () -> service.registerWalkIn("Svc Alice", idA, "0917", ResourceType.COMPUTER, "Again"));

            WalkInTicket assigned = service.assignNext(ResourceType.COMPUTER);
            System.out.println("   Assigned " + assigned.getReference() + " to " + assigned.getResource());
            check("Assign next puts the first in line on a free computer",
                    assigned.getStatus() == VisitStatus.IN_USE
                            && assigned.getResource().getResourceType() == ResourceType.COMPUTER);

            System.out.println("--- Status changes, edit, delete ---");
            service.changeStatus(assigned, VisitStatus.COMPLETED);
            check("An IN_USE visit can be completed", assigned.getStatus() == VisitStatus.COMPLETED);
            expectFailure("A COMPLETED visit can't go back to IN_USE",
                    () -> service.changeStatus(assigned, VisitStatus.IN_USE));

            service.reschedule(booking, room, tomorrow, TimeSlot.SLOT_3, "Group study (moved)");
            check("Rescheduling frees the old slot and takes the new one",
                    service.getOpenSlots(room, tomorrow).contains(TimeSlot.SLOT_2)
                            && !service.getOpenSlots(room, tomorrow).contains(TimeSlot.SLOT_3));

            service.changeStatus(booking, VisitStatus.CANCELLED);
            check("A cancelled booking gives its slot back",
                    service.getOpenSlots(room, tomorrow).contains(TimeSlot.SLOT_3));

            System.out.println("--- Search and filter ---");
            check("Search by name finds Alice's 2 visits", service.findVisits("Svc Alice").size() == 2);
            check("Filter finds the waiting walk-in",
                    service.filterVisits(VisitType.WALKIN, VisitStatus.WAITING, LocalDate.now()).size() >= 1);

            Visit toDelete = null;
            for (Visit v : service.getVisits()) {
                if (v.getVisitId() == second.getVisitId()) {
                    toDelete = v;
                }
            }
            service.deleteVisit(toDelete);
            boolean gone = true;
            for (Visit v : service.getVisits()) {
                if (v.getVisitId() == second.getVisitId()) {
                    gone = false;
                }
            }
            check("Delete removes the record", gone);
        } finally {
            System.out.println("--- Cleaning up test data ---");
            service.loadAll();
            for (Client c : service.getClients()) {
                if (c.getStudentId().startsWith("SVC-") && c.getStudentId().endsWith(stamp)) {
                    clientRepo.delete(c.getClientId()); // their visits go too (ON DELETE CASCADE)
                }
            }
        }
        return failures;
    }

    private static void check(String label, boolean condition) {
        System.out.println((condition ? "PASS - " : "FAIL - ") + label);
        if (!condition) {
            failures++;
        }
    }

    /** The action must be refused with a LibraryException. The message printed is what the user would see. */
    private static void expectFailure(String label, Action action) {
        try {
            action.run();
            System.out.println("FAIL - " + label + " (nothing was refused)");
            failures++;
        } catch (LibraryException e) {
            System.out.println("PASS - " + label + "  -> \"" + e.getMessage() + "\"");
        } catch (Exception e) {
            System.out.println("FAIL - " + label + " (wrong error: " + e + ")");
            failures++;
        }
    }
}
