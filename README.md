# Campus Event Management Application

## Overview
Campus Event Management Application is a Java desktop application for managing university events. 
It includes user accounts, role-based access, event organisation, and event registration functionality.

This project was developed as part of my university coursework to practise Java application development, 
GUI design, database integration, and object-oriented programming.

## Features
- User login system
- Role-based access for students, organisers, and admins
- Event creation and management
- Event registration
- PostgreSQL database connection
- Seed data for testing

## Technologies Used
- Java
- JavaFX / GUI development
- PostgreSQL
- SQL
- IntelliJ IDEA
- Object-Oriented Programming

## How to Run

Open the project in IntelliJ IDEA.

Run:
src/main/java/uk/ac/aru/campusevents/main/Launcher

Recommended setup:
- SDK: OpenJDK 23
- Language level: SDK default

## Database Setup

Create a PostgreSQL database named:
campusevent

Use the following local database settings:
- Host: localhost
- Port: 5432
- Database: campusevent
- User: postgres
- Password: postgres

The schema and seed data are located in:
src/main/resources/sql

Run the SQL files at least once before launching the application.

## Test Accounts

All test account passwords are:
12345678

- Stark@example.com — STUDENT, ORGANIZER, ADMIN
- Banner@example.com — STUDENT, ORGANIZER
- Rogers@example.com — ADMIN
- Romanoff@example.com — ORGANIZER, ADMIN
- Lang@example.com — STUDENT
- Parker@example.com — ORGANIZER
- Downer@example.com — STUDENT, ORGANIZER, ADMIN
- Tully@example.com — STUDENT, ORGANIZER, ADMIN
- Saint@example.com — STUDENT, ORGANIZER, ADMIN

## Notes
This is a coursework project and is intended for learning purposes. 
The application uses local database credentials for demonstration and testing only.
