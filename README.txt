==============================================================================
 LIBRARY STUDY ROOM & COMPUTER STATION BOOKING SYSTEM
 Hybrid reservation + walk-in queue  |  Java OOP, Swing and MySQL
 OOP Finals Performance-Based Task
==============================================================================

DESCRIPTION
  A desktop application for a library that serves two kinds of clients in one
  system:
    - BOOKED clients reserve a study room or computer station for a fixed
      2-hour time slot (8 AM to 6 PM, up to 30 days ahead).
    - WALK-IN clients register on arrival and get a queue number
      (R-001 for study rooms, C-001 for computers). Staff press "Assign next"
      to give the first person in line a free resource.

FEATURES
  Book a Slot    Reserve a slot. Double bookings are refused.
  Walk-in Queue  Register walk-ins, see both queues, assign the next free
                 resource, complete visits.
  Records        All visits in one table with search and filters. Check in,
                 complete, cancel, no-show, reschedule, edit purpose, delete.

REQUIREMENTS
  - JDK 17 or newer (java and javac available in the terminal)
  - MySQL Server (the "mysql" command line client must work)
  - MySQL Connector/J .jar file in the lib folder (included)

SETUP AND RUN
  Run every command from this folder (the project root).

  1. Create the database (asks for your MySQL password):
       mysql -u root -p -e "source Database/schema.sql"

  2. Copy db.properties.example to db.properties and type your MySQL
     password on the db.password line.
       Windows PowerShell:   Copy-Item db.properties.example db.properties
       macOS / Linux:        cp db.properties.example db.properties

  3. Compile.
       Windows PowerShell:
         mkdir bin -Force
         javac --release 17 -d bin -cp "lib/*" (Get-ChildItem Source_Code -Recurse -Filter *.java).FullName
       macOS / Linux / Git Bash:
         mkdir -p bin
         javac --release 17 -d bin -cp "lib/*" $(find Source_Code -name "*.java")

  4. Run.
       Windows PowerShell:   java -cp "bin;lib/*" Main
       macOS / Linux:        java -cp "bin:lib/*" Main

TROUBLESHOOTING
  "Cannot start" dialog      MySQL is not running, or the password in
                             db.properties is wrong.
  "No suitable driver"       The Connector/J .jar must be directly inside lib/.
  UnsupportedClassVersion    Delete the bin folder and compile again with
                             --release 17.
  "No open slots" for today  Every slot of today is over after 6 PM. Choose
                             tomorrow. Walk-ins work at any hour.

NOTES
  - Rooms and computers are loaded from Database/schema.sql (3 rooms, 5 PCs).
  - To clear test data and keep the rooms and computers:
       mysql -u root -p -e "USE library_booking; DELETE FROM clients;"

FOLDER STRUCTURE
  Source_Code/   Main.java and the packages model, data, service, ui
  Database/      schema.sql
  lib/           MySQL Connector/J
  Screenshots/   Screenshots of the running system
  Documentation/ Project_Documentation (with UML diagrams)
  README.txt     This file

GROUP MEMBERS
  1. Ken Carlo A. Gelizon
  2. Vince Lendon L. Padar
  3. Clinther R. Baquiro
  4. Jedelyn L. Tanguihan
  5. Jonathan Matthew Abad
  6. Anghela S. Casas