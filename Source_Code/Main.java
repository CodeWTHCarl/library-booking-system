import model.*;
import java.time.LocalDate;

/** TEMPORARY setup check. This file is replaced when the Swing UI is built. */
public class Main {
    public static void main(String[] args) {
        Client client = new Client("Test Client", "2024-0001", "09170000000");
        Resource room = new StudyRoom(1, "Room A", 6, true);
        Visit visit = new BookedReservation(1, client, room, LocalDate.now(),
                TimeSlot.SLOT_1, "Setup check", VisitStatus.BOOKED);
        System.out.println(visit.getDetails());
        System.out.println("Setup OK!");
    }
}
