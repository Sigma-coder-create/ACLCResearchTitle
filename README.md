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