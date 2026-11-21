import data.ClientRepository;
import data.ResourceRepository;
import data.VisitRepository;
import model.*;

import java.time.LocalDate;
import java.util.List;

/** TEMPORARY Phase 1 checkpoint. Talks to MySQL, prints what it did, then cleans up after itself. */
public class DataLayerCheck {
    public static void main(String[] args) {
        try {
            ClientRepository clients = new ClientRepository();
            ResourceRepository resources = new ResourceRepository();
            VisitRepository visits = new VisitRepository();

            System.out.println("1) Loading resources...");
            List<Resource> all = resources.getAll();
            System.out.println("   Found " + all.size() + " resources");
            Resource room = null;
            for (Resource r : all) {
                if (r.getResourceType() == ResourceType.ROOM) {
                    room = r;
                    break;
                }
            }

            System.out.println("2) Adding a test client...");
            Client client = new Client("Check Client", "CHECK-" + System.currentTimeMillis(), "09170000000");
            clients.add(client);
            System.out.println("   Saved with id " + client.getClientId());

            System.out.println("3) Adding a booking and a walk-in...");
            LocalDate today = LocalDate.now();
            visits.add(new BookedReservation(0, client, room, today, TimeSlot.SLOT_1,
                    "Check booking", VisitStatus.BOOKED));
            int queue = visits.nextQueueNumber(today, ResourceType.COMPUTER);
            visits.add(new WalkInTicket(0, client, null, today, ResourceType.COMPUTER, queue,
                    "Check walk-in", VisitStatus.WAITING));

            System.out.println("4) Reading visits back from MySQL...");
            for (Visit v : visits.getAll()) {
                System.out.println("   " + v.getDetails());
            }

            System.out.println("5) Cleaning up the test data...");
            clients.delete(client.getClientId()); // visits are deleted too (ON DELETE CASCADE)
            System.out.println("DATA LAYER OK");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
