LIBRARY STUDY ROOM & COMPUTER STATION BOOKING SYSTEM
Java OOP + Swing + MySQL  |  OOP Finals Performance-Based Task

REQUIREMENTS
- JDK 17 or newer
- MySQL Server (command line client "mysql")
- MySQL Connector/J: the single .jar file placed directly in the lib/ folder

SETUP (run every command from the project root folder)
1. Create the database:
     mysql -u root -p -e "source Database/schema.sql"

2. Copy db.properties.example to db.properties and type your MySQL password in it.

3. Compile (--release 17 makes the classes runnable on Java 17 or newer)
   macOS / Linux / Git Bash:
     mkdir -p bin
     javac --release 17 -d bin -cp "lib/*" $(find Source_Code -name "*.java")
   Windows PowerShell:
     mkdir bin -Force
     javac --release 17 -d bin -cp "lib/*" (Get-ChildItem Source_Code -Recurse -Filter *.java).FullName

4. Run
   macOS / Linux / Git Bash:   java -cp "bin:lib/*" Main
   Windows PowerShell:         java -cp "bin;lib/*" Main

GROUP MEMBERS
(add names here)
