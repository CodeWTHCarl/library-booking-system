-- ============================================================
-- Library Study Room & Computer Station Booking System
-- Run this whole file once in MySQL Workbench (File > Open SQL Script > Run).
-- ============================================================
CREATE DATABASE IF NOT EXISTS library_booking;
USE library_booking;

-- Clients (library patrons)
CREATE TABLE IF NOT EXISTS clients (
    client_id   INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    student_id  VARCHAR(30)  NOT NULL UNIQUE,
    contact_no  VARCHAR(20)  NOT NULL
);

-- Resources: study rooms and computer stations (one table, "resource_type" tells them apart)
CREATE TABLE IF NOT EXISTS resources (
    resource_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(50) NOT NULL UNIQUE,
    resource_type  ENUM('ROOM', 'COMPUTER') NOT NULL,
    capacity       INT NOT NULL DEFAULT 1,          -- rooms: max users, computers: 1
    is_available   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Visits: reserved bookings AND walk-in tickets (one table, "visit_type" tells them apart)
--   BOOKED : resource_id + time_slot are filled in, queue_number is NULL
--   WALKIN : requested_type + queue_number are filled in, resource_id stays NULL until staff assigns one
CREATE TABLE IF NOT EXISTS visits (
    visit_id        INT AUTO_INCREMENT PRIMARY KEY,
    client_id       INT NOT NULL,
    resource_id     INT NULL,
    visit_type      ENUM('BOOKED', 'WALKIN') NOT NULL,
    requested_type  ENUM('ROOM', 'COMPUTER') NULL,
    visit_date      DATE NOT NULL,
    time_slot       VARCHAR(10) NULL,
    queue_number    INT NULL,
    purpose         VARCHAR(150) NOT NULL,
    status          ENUM('BOOKED', 'WAITING', 'IN_USE', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL,
    FOREIGN KEY (client_id)   REFERENCES clients(client_id)     ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE RESTRICT,
    -- a queue number can only be used once per day per resource type (NULLs are ignored for bookings)
    UNIQUE (visit_date, requested_type, queue_number)
);

-- Sample resources so the app has something to show on first run
INSERT IGNORE INTO resources (name, resource_type, capacity) VALUES
    ('Room A', 'ROOM', 6),
    ('Room B', 'ROOM', 4),
    ('Room C', 'ROOM', 8),
    ('PC-01', 'COMPUTER', 1),
    ('PC-02', 'COMPUTER', 1),
    ('PC-03', 'COMPUTER', 1),
    ('PC-04', 'COMPUTER', 1),
    ('PC-05', 'COMPUTER', 1);
