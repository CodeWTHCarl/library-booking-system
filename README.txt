LIBRARY STUDY ROOM & COMPUTER STATION BOOKING SYSTEM
Java OOP + Swing + MySQL  |  OOP Finals Performance-Based Task

REQUIREMENTS
- JDK 17 or newer
- MySQL Server (command line client "mysql")
- MySQL Connector/J jar placed in the lib/ folder

SETUP (run from the project root folder)
1. Create the database:
     mysql -u root -p -e "source Database/schema.sql"

2. Compile
   macOS / Linux / Git Bash:
     mkdir -p bin
     javac -d bin -cp "lib/*" $(find Source_Code -name "*.java")
   Windows PowerShell:
     mkdir bin -Force
     javac -d bin -cp "lib/*" (Get-ChildItem Source_Code -Recurse -Filter *.java).FullName

3. Run
   macOS / Linux / Git Bash:   java -cp "bin:lib/*" Main
   Windows PowerShell:         java -cp "bin;lib/*" Main

GROUP MEMBERS
(add names here)
