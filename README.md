# Book Bank Management System

This project is a Java-only, menu-driven console application for managing a small book bank. It uses only the Java Standard Library and has no external frameworks or database dependencies.

## Features

The system supports book creation, editing, deletion, searching, and listing. It supports member registration, editing, removal, searching, and listing. Staff can issue and return books, view active and overdue loans, calculate late-return fines, and review system statistics.

The default loan period is 14 days, and the fine is 2.00 per overdue day. A member may have up to five active loans, and a member cannot borrow the same book twice at the same time.

## Requirements

Java Development Kit 17 or newer is recommended because the source uses modern Java language features and the standard `Stream.toList()` method.

## Compile and run

Open a terminal in the project directory and run:

```bash
javac Main.java
java Main
```

The program stores its data in `book-bank-data.ser` in the current working directory. This file is created automatically and loaded when the application starts. Keep it in the same directory as the program if you want to preserve records between runs.

## Main workflow

First, use **Manage books** to add at least one book and use **Manage members** to register a member. Then choose **Issue a book**, provide the book ID and member ID, and keep the generated loan ID. Use that loan ID in **Return a book** when the book is returned.

## Source layout

The application is intentionally contained in a single source file:

```text
Main.java
```

The file contains the console interface, validation logic, book and member models, loan tracking, fine calculation, and serialized file persistence.
