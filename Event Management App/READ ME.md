Opening the software :

src > main > java > uk > ac > aru > campusevents > main > run 'Launcher



Open with IntelliJ

SDK: openjdk-23 Oracle OpenJDK 23.0.1

Language Level: SDK default



Database connection

Name: campusevent@localhost

Host: localhost

Port: 5432

User: postgres

Password: postgres



The schema and seed data are in:

src > main > resources > sql



Pre-created accounts (all passwords are: 12345678)

(a password must be at least 8 characters long)



Stark@example.com      STUDENT, ORGANIZER, ADMIN > registered for org\_id 9

Banner@example.com     STUDENT, ORGANIZER > registerd for org\_id 9 and 12

Rogers@example.com     ADMIN

Romanoff@example.com   ORGANIZER, ADMIN

Lang@example.com       STUDENT

Parker@example.com     ORGANIZER

Downer@example.com     STUDENT, ORGANIZER, ADMIN

Tully@example.com      STUDENT, ORGANIZER, ADMIN

Saint@example.com      STUDENT, ORGANIZER, ADMIN







Make sure:

\- The datasource points at database "campusevent"

\- All schemas / SQL files are attached and run at least once

