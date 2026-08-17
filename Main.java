import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Book Bank Management System
 *
 * A Java-only, menu-driven console application with:
 * - Book CRUD and search
 * - Member registration and listing
 * - Book issue and return tracking
 * - Due dates and automatic fine calculation
 * - File-based persistence using Java serialization
 *
 * Compile: javac Main.java
 * Run:     java Main
 */
public class Main {
    private static final String DATA_FILE = "book-bank-data.ser";
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double FINE_PER_DAY = 2.0;
    private static final int MAX_ACTIVE_LOANS_PER_MEMBER = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final Scanner scanner = new Scanner(System.in);
    private final DataStore store;

    public Main() {
        store = DataStore.load(DATA_FILE);
    }

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("===============================================");
        System.out.println("       BOOK BANK MANAGEMENT SYSTEM");
        System.out.println("===============================================");
        System.out.println("Data file: " + DATA_FILE);

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Choose an option: ");
            System.out.println();
            switch (choice) {
                case 1 -> bookMenu();
                case 2 -> memberMenu();
                case 3 -> issueBook();
                case 4 -> returnBook();
                case 5 -> listActiveLoans();
                case 6 -> showOverdueLoans();
                case 7 -> showStatistics();
                case 0 -> running = false;
                default -> System.out.println("Invalid option. Please choose again.");
            }
            if (running) pause();
        }

        store.save(DATA_FILE);
        scanner.close();
        System.out.println("Application closed. Data saved successfully.");
    }

    private void printMainMenu() {
        System.out.println("\n--------------- MAIN MENU ----------------");
        System.out.println("1. Manage books");
        System.out.println("2. Manage members");
        System.out.println("3. Issue a book");
        System.out.println("4. Return a book");
        System.out.println("5. View active loans");
        System.out.println("6. View overdue loans");
        System.out.println("7. View statistics");
        System.out.println("0. Save and exit");
        System.out.println("--------------------------------------------");
    }

    private void bookMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--------------- BOOK MENU -----------------");
            System.out.println("1. Add book");
            System.out.println("2. Update book");
            System.out.println("3. Delete book");
            System.out.println("4. Search books");
            System.out.println("5. List all books");
            System.out.println("0. Back to main menu");
            System.out.println("--------------------------------------------");

            int choice = readInt("Choose an option: ");
            System.out.println();
            switch (choice) {
                case 1 -> addBook();
                case 2 -> updateBook();
                case 3 -> deleteBook();
                case 4 -> searchBooks();
                case 5 -> listAllBooks();
                case 0 -> back = true;
                default -> System.out.println("Invalid option.");
            }
            if (!back) pause();
        }
    }

    private void memberMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-------------- MEMBER MENU ----------------");
            System.out.println("1. Register member");
            System.out.println("2. Update member");
            System.out.println("3. Remove member");
            System.out.println("4. Search members");
            System.out.println("5. List all members");
            System.out.println("0. Back to main menu");
            System.out.println("--------------------------------------------");

            int choice = readInt("Choose an option: ");
            System.out.println();
            switch (choice) {
                case 1 -> addMember();
                case 2 -> updateMember();
                case 3 -> deleteMember();
                case 4 -> searchMembers();
                case 5 -> listAllMembers();
                case 0 -> back = true;
                default -> System.out.println("Invalid option.");
            }
            if (!back) pause();
        }
    }

    private void addBook() {
        System.out.println("ADD BOOK");
        String id = readRequired("Book ID: ");
        if (store.books.containsKey(id)) {
            System.out.println("A book with this ID already exists.");
            return;
        }
        String title = readRequired("Title: ");
        String author = readRequired("Author: ");
        String category = readRequired("Category: ");
        int quantity = readPositiveInt("Quantity: ");

        store.books.put(id, new Book(id, title, author, category, quantity));
        store.save(DATA_FILE);
        System.out.println("Book added successfully.");
    }

    private void updateBook() {
        System.out.println("UPDATE BOOK");
        String id = readRequired("Book ID: ");
        Book book = store.books.get(id);
        if (book == null) {
            System.out.println("Book not found.");
            return;
        }

        System.out.println("Press Enter to keep the existing value.");
        String title = readOptional("Title [" + book.title + "]: ");
        String author = readOptional("Author [" + book.author + "]: ");
        String category = readOptional("Category [" + book.category + "]: ");
        String quantityText = readOptional("Quantity [" + book.totalQuantity + "]: ");

        if (!title.isBlank()) book.title = title;
        if (!author.isBlank()) book.author = author;
        if (!category.isBlank()) book.category = category;
        if (!quantityText.isBlank()) {
            try {
                int newQuantity = Integer.parseInt(quantityText);
                int issuedCopies = book.totalQuantity - book.availableQuantity;
                if (newQuantity < issuedCopies || newQuantity <= 0) {
                    System.out.println("Quantity cannot be less than issued copies (" + issuedCopies + ").");
                    return;
                }
                book.totalQuantity = newQuantity;
                book.availableQuantity = newQuantity - issuedCopies;
            } catch (NumberFormatException e) {
                System.out.println("Quantity must be a valid positive integer.");
                return;
            }
        }

        store.save(DATA_FILE);
        System.out.println("Book updated successfully.");
    }

    private void deleteBook() {
        System.out.println("DELETE BOOK");
        String id = readRequired("Book ID: ");
        Book book = store.books.get(id);
        if (book == null) {
            System.out.println("Book not found.");
            return;
        }
        boolean hasActiveLoan = store.loans.values().stream()
                .anyMatch(loan -> loan.bookId.equals(id) && loan.isActive());
        if (hasActiveLoan) {
            System.out.println("This book cannot be deleted while copies are issued.");
            return;
        }
        if (readYesNo("Delete '" + book.title + "'? (y/n): ")) {
            store.books.remove(id);
            store.save(DATA_FILE);
            System.out.println("Book deleted successfully.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void searchBooks() {
        String query = readRequired("Search by ID, title, author, or category: ").toLowerCase();
        List<Book> matches = store.books.values().stream()
                .filter(book -> book.id.toLowerCase().contains(query)
                        || book.title.toLowerCase().contains(query)
                        || book.author.toLowerCase().contains(query)
                        || book.category.toLowerCase().contains(query))
                .sorted(Comparator.comparing(book -> book.title.toLowerCase()))
                .toList();
        printBooks(matches);
    }

    private void listAllBooks() {
        List<Book> books = new ArrayList<>(store.books.values());
        books.sort(Comparator.comparing(book -> book.title.toLowerCase()));
        printBooks(books);
    }

    private void printBooks(Collection<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        System.out.printf("%-12s %-28s %-22s %-16s %8s %10s%n", "ID", "TITLE", "AUTHOR", "CATEGORY", "TOTAL", "AVAILABLE");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (Book book : books) {
            System.out.printf("%-12s %-28s %-22s %-16s %8d %10d%n",
                    truncate(book.id, 12), truncate(book.title, 28), truncate(book.author, 22),
                    truncate(book.category, 16), book.totalQuantity, book.availableQuantity);
        }
    }

    private void addMember() {
        System.out.println("REGISTER MEMBER");
        String id = readRequired("Member ID: ");
        if (store.members.containsKey(id)) {
            System.out.println("A member with this ID already exists.");
            return;
        }
        String name = readRequired("Name: ");
        String phone = readRequired("Phone: ");
        String email = readRequired("Email: ");
        store.members.put(id, new Member(id, name, phone, email));
        store.save(DATA_FILE);
        System.out.println("Member registered successfully.");
    }

    private void updateMember() {
        System.out.println("UPDATE MEMBER");
        String id = readRequired("Member ID: ");
        Member member = store.members.get(id);
        if (member == null) {
            System.out.println("Member not found.");
            return;
        }
        System.out.println("Press Enter to keep the existing value.");
        String name = readOptional("Name [" + member.name + "]: ");
        String phone = readOptional("Phone [" + member.phone + "]: ");
        String email = readOptional("Email [" + member.email + "]: ");
        if (!name.isBlank()) member.name = name;
        if (!phone.isBlank()) member.phone = phone;
        if (!email.isBlank()) member.email = email;
        store.save(DATA_FILE);
        System.out.println("Member updated successfully.");
    }

    private void deleteMember() {
        System.out.println("REMOVE MEMBER");
        String id = readRequired("Member ID: ");
        Member member = store.members.get(id);
        if (member == null) {
            System.out.println("Member not found.");
            return;
        }
        boolean hasActiveLoan = store.loans.values().stream()
                .anyMatch(loan -> loan.memberId.equals(id) && loan.isActive());
        if (hasActiveLoan) {
            System.out.println("This member cannot be removed while books are issued.");
            return;
        }
        if (readYesNo("Remove '" + member.name + "'? (y/n): ")) {
            store.members.remove(id);
            store.save(DATA_FILE);
            System.out.println("Member removed successfully.");
        } else {
            System.out.println("Removal cancelled.");
        }
    }

    private void searchMembers() {
        String query = readRequired("Search by ID, name, phone, or email: ").toLowerCase();
        List<Member> matches = store.members.values().stream()
                .filter(member -> member.id.toLowerCase().contains(query)
                        || member.name.toLowerCase().contains(query)
                        || member.phone.toLowerCase().contains(query)
                        || member.email.toLowerCase().contains(query))
                .sorted(Comparator.comparing(member -> member.name.toLowerCase()))
                .toList();
        printMembers(matches);
    }

    private void listAllMembers() {
        List<Member> members = new ArrayList<>(store.members.values());
        members.sort(Comparator.comparing(member -> member.name.toLowerCase()));
        printMembers(members);
    }

    private void printMembers(Collection<Member> members) {
        if (members.isEmpty()) {
            System.out.println("No members found.");
            return;
        }
        System.out.printf("%-12s %-24s %-18s %-30s %8s%n", "ID", "NAME", "PHONE", "EMAIL", "ACTIVE LOANS");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (Member member : members) {
            long activeLoans = store.loans.values().stream()
                    .filter(loan -> loan.memberId.equals(member.id) && loan.isActive()).count();
            System.out.printf("%-12s %-24s %-18s %-30s %8d%n",
                    truncate(member.id, 12), truncate(member.name, 24), truncate(member.phone, 18),
                    truncate(member.email, 30), activeLoans);
        }
    }

    private void issueBook() {
        System.out.println("ISSUE BOOK");
        String bookId = readRequired("Book ID: ");
        Book book = store.books.get(bookId);
        if (book == null) {
            System.out.println("Book not found.");
            return;
        }
        if (book.availableQuantity <= 0) {
            System.out.println("No available copy of this book.");
            return;
        }

        String memberId = readRequired("Member ID: ");
        Member member = store.members.get(memberId);
        if (member == null) {
            System.out.println("Member not found.");
            return;
        }

        long activeLoans = store.loans.values().stream()
                .filter(loan -> loan.memberId.equals(memberId) && loan.isActive()).count();
        if (activeLoans >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            System.out.println("This member has reached the maximum of " + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans.");
            return;
        }
        boolean alreadyBorrowed = store.loans.values().stream()
                .anyMatch(loan -> loan.memberId.equals(memberId) && loan.bookId.equals(bookId) && loan.isActive());
        if (alreadyBorrowed) {
            System.out.println("This member already has an active loan for this book.");
            return;
        }

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(LOAN_PERIOD_DAYS);
        String loanId = "L" + String.format("%05d", store.nextLoanNumber++);
        Loan loan = new Loan(loanId, bookId, memberId, issueDate, dueDate);
        store.loans.put(loanId, loan);
        book.availableQuantity--;
        store.save(DATA_FILE);

        System.out.println("Book issued successfully.");
        System.out.println("Loan ID: " + loanId);
        System.out.println("Due date: " + formatDate(dueDate));
    }

    private void returnBook() {
        System.out.println("RETURN BOOK");
        String loanId = readRequired("Loan ID: ");
        Loan loan = store.loans.get(loanId);
        if (loan == null) {
            System.out.println("Loan not found.");
            return;
        }
        if (!loan.isActive()) {
            System.out.println("This loan has already been returned on " + formatDate(loan.returnDate) + ".");
            return;
        }

        LocalDate returnDate = LocalDate.now();
        double fine = calculateFine(loan.dueDate, returnDate);
        loan.returnDate = returnDate;
        loan.fine = fine;
        Book book = store.books.get(loan.bookId);
        if (book != null) book.availableQuantity++;
        store.save(DATA_FILE);

        System.out.println("Book returned successfully on " + formatDate(returnDate) + ".");
        if (fine > 0) {
            System.out.printf("Late return fine: %.2f%n", fine);
        } else {
            System.out.println("No fine is due.");
        }
    }

    private void listActiveLoans() {
        System.out.println("ACTIVE LOANS");
        List<Loan> loans = store.loans.values().stream()
                .filter(Loan::isActive)
                .sorted(Comparator.comparing(loan -> loan.dueDate))
                .toList();
        printLoans(loans);
    }

    private void showOverdueLoans() {
        System.out.println("OVERDUE LOANS");
        LocalDate today = LocalDate.now();
        List<Loan> loans = store.loans.values().stream()
                .filter(loan -> loan.isActive() && loan.dueDate.isBefore(today))
                .sorted(Comparator.comparing(loan -> loan.dueDate))
                .toList();
        printLoans(loans);
        if (!loans.isEmpty()) {
            double total = loans.stream().mapToDouble(loan -> calculateFine(loan.dueDate, today)).sum();
            System.out.printf("Current total outstanding fine: %.2f%n", total);
        }
    }

    private void printLoans(Collection<Loan> loans) {
        if (loans.isEmpty()) {
            System.out.println("No loans found.");
            return;
        }
        System.out.printf("%-10s %-12s %-24s %-12s %-12s %-12s %10s%n",
                "LOAN ID", "BOOK ID", "BOOK TITLE", "MEMBER ID", "ISSUED", "DUE", "FINE");
        System.out.println("------------------------------------------------------------------------------------------------");
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            Book book = store.books.get(loan.bookId);
            double currentFine = loan.isActive() ? calculateFine(loan.dueDate, today) : loan.fine;
            System.out.printf("%-10s %-12s %-24s %-12s %-12s %-12s %10.2f%n",
                    loan.id, loan.bookId, truncate(book == null ? "Unknown" : book.title, 24), loan.memberId,
                    formatDate(loan.issueDate), formatDate(loan.dueDate), currentFine);
        }
    }

    private void showStatistics() {
        int totalCopies = store.books.values().stream().mapToInt(book -> book.totalQuantity).sum();
        int availableCopies = store.books.values().stream().mapToInt(book -> book.availableQuantity).sum();
        long activeLoans = store.loans.values().stream().filter(Loan::isActive).count();
        long overdueLoans = store.loans.values().stream()
                .filter(loan -> loan.isActive() && loan.dueDate.isBefore(LocalDate.now())).count();
        double collectedFines = store.loans.values().stream().mapToDouble(loan -> loan.fine).sum();

        System.out.println("SYSTEM STATISTICS");
        System.out.println("Total book titles       : " + store.books.size());
        System.out.println("Total physical copies   : " + totalCopies);
        System.out.println("Available copies        : " + availableCopies);
        System.out.println("Registered members      : " + store.members.size());
        System.out.println("Active loans            : " + activeLoans);
        System.out.println("Currently overdue loans : " + overdueLoans);
        System.out.printf("Recorded returned fines : %.2f%n", collectedFines);
    }

    private static double calculateFine(LocalDate dueDate, LocalDate date) {
        if (!date.isAfter(dueDate)) return 0.0;
        long overdueDays = ChronoUnit.DAYS.between(dueDate, date);
        return overdueDays * FINE_PER_DAY;
    }

    private int readInt(String prompt) {
        while (true) {
            String value = readOptional(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) return value;
            System.out.println("Please enter a number greater than zero.");
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readOptional(prompt);
            if (!value.isBlank()) return value;
            System.out.println("This field is required.");
        }
    }

    private String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String answer = readOptional(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println("Please answer y or n.");
        }
    }

    private void pause() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private static class DataStore implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, Book> books = new HashMap<>();
        private final Map<String, Member> members = new HashMap<>();
        private final Map<String, Loan> loans = new HashMap<>();
        private int nextLoanNumber = 1;

        static DataStore load(String fileName) {
            File file = new File(fileName);
            if (!file.exists()) return new DataStore();
            try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
                Object object = input.readObject();
                if (object instanceof DataStore loaded) return loaded;
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                System.out.println("Existing data could not be loaded. Starting with an empty database.");
            }
            return new DataStore();
        }

        void save(String fileName) {
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(fileName))) {
                output.writeObject(this);
            } catch (IOException e) {
                System.out.println("Warning: data could not be saved: " + e.getMessage());
            }
        }
    }

    private static class Book implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private String title;
        private String author;
        private String category;
        private int totalQuantity;
        private int availableQuantity;

        Book(String id, String title, String author, String category, int quantity) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.category = category;
            this.totalQuantity = quantity;
            this.availableQuantity = quantity;
        }
    }

    private static class Member implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private String name;
        private String phone;
        private String email;

        Member(String id, String name, String phone, String email) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
        }
    }

    private static class Loan implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final String bookId;
        private final String memberId;
        private final LocalDate issueDate;
        private final LocalDate dueDate;
        private LocalDate returnDate;
        private double fine;

        Loan(String id, String bookId, String memberId, LocalDate issueDate, LocalDate dueDate) {
            this.id = id;
            this.bookId = bookId;
            this.memberId = memberId;
            this.issueDate = issueDate;
            this.dueDate = dueDate;
        }

        boolean isActive() {
            return returnDate == null;
        }
    }
}
