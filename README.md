# ACLC Research Title Management System

A Java desktop application that prevents duplicate research titles using:
- **MySQL** (central database, optional)
- **SQLite** (local offline fallback)
- **TF‑IDF** similarity detection

## Features
- Add, edit, delete research titles
- Search by title, SY‑YR, or strand
- Automatic sync between MySQL and SQLite
- Offline support (SQLite only)

## Requirements
- Java 17 or later
- Maven (for building)
- MySQL (optional, for sync)

## Building and Running

### Build the executable JAR
```bash
mvn clean package

# Installer Exit Codes

The installer (created with jpackage) uses the following exit codes:

| Scenario | Exit Code |
|----------|-----------|
| Installation successful | 0 |
| User cancelled installation | 1602 |
| Another installation already in progress | 1618 |
| Disk space full | 112 |
| Reboot required | 3010 |
| Fatal error | 1603 |
| (Re‑installation of same version) | 0 |
