import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Scanner;

/**
 * IDE Assignment #3 — Bookstore Inventory Console (Extension Project)
 *
 * Starter driver for students. Options 1–3 are implemented. Your job is to complete options 4–6.
 * - Reads a CSV of inventory and course adoptions into an ArrayList<Books> (NO maps or streams).
 * - Writes text reports to src/data/reports/ using try-with-resources.
 *
 * CSV columns (5): CourseCode,Title,Author,Price,Required
 *   Example row:  CSC 222,Refactoring,Martin Fowler,42.00,Required
 *
 * IMPORTANT:
 *   • Keep your work for this assignment in this file (cases 4–6) — do not modify options 1–3.
 *   • Do not move the data path constants; Canvas tests rely on them.
 */
public class MenuDriver {

    // ====== Constants (do not change) ======
    private static final String DEFAULT_DATA_PATH = "src/data/inventory_textbooks.csv";
    private static final String REPORTS_DIR = "src/data/reports";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Ensure the reports directory exists (ok if already there)
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            System.out.println("Warning: Could not create reports directory: " + e.getMessage());
        }

        // ====== Prompt for the CSV path (press Enter for default) ======
        String dataPath = promptForDataPath(sc);

        // ====== Load data from CSV into ArrayList<Books> ======
        ArrayList<Books> books = loadBooks(dataPath);

        // ====== Print the banner header ======
        printBanner(dataPath, books.size());

        // ====== Menu loop ======
        int choice;
        do {
            printMenu();
            choice = readInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> listAllBooks(books);             // Implemented
                case 2 -> searchByCourse(books, sc);        // Implemented
                case 3 -> saveAllBooksReport(books);        // Implemented
                case 4 -> saveCourseReport(books, sc);      // TODO: YOU implement this method
                case 5 -> exportTopN(books, sc);            // TODO: YOU implement this method
                case 6 -> exportPriceStats(books);          // TODO: YOU implement this method
                case 0 -> System.out.println("Goodbye!");
                default -> System.out.println("Please choose a valid option (0–6).");
            }
            System.out.println();
        } while (choice != 0);
    }

    // ===== UI helpers =====

    /** Ask user for CSV path; Enter accepts the default data path. */
    private static String promptForDataPath(Scanner sc) {
        System.out.println("Enter path to CSV (press Enter for default):");
        System.out.println("Default -> " + DEFAULT_DATA_PATH);
        System.out.print("> ");
        String input = sc.nextLine().trim();
        return input.isEmpty() ? DEFAULT_DATA_PATH : input;
    }

    /** Prints a nicely formatted banner/header in the console. */
    private static void printBanner(String dataPath, int count) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String line = "=".repeat(54);
        System.out.println(line);
        System.out.println(" NRCC Bookstore Console — Inventory & Adoptions");
        System.out.println(" Data file: " + dataPath);
        System.out.println(" Loaded: " + count + " rows    Run: " + ts);
        System.out.println(line);
        System.out.println();
    }

    /** Prints the main menu. NOTE: only 1–3 are implemented in the starter. */
    private static void printMenu() {
        System.out.println("NRCC Bookstore Console");
        System.out.println("1) List all books");
        System.out.println("2) Search by course");
        System.out.println("3) Save ALL BOOKS report");
        System.out.println("4) Save COURSE report (list + totals)");
        System.out.println("5) Export TOP-N most expensive");
        System.out.println("6) Export PRICE STATS (min/max/avg/median)");
        System.out.println("0) Exit");
    }

    /** Read an integer with validation, reprompting until valid. */
    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    // ===== Data loading =====

    /**
     * Loads Books from a CSV file with columns:
     * CourseCode,Title,Author,Price,Required
     * Skips malformed rows. Keeps it simple for students (no error logs here).
     */
    private static ArrayList<Books> loadBooks(String path) {
        ArrayList<Books> list = new ArrayList<>();
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Could not find file: " + path);
            return list;
        }
        try (Scanner file = new Scanner(f, "UTF-8")) {
            boolean firstLine = true;
            while (file.hasNextLine()) {
                String line = file.nextLine();
                if (line.trim().isEmpty()) continue;

                // Skip header if it looks like one
                if (firstLine && looksLikeHeader(line)) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                // We expect exactly 5 columns; we don't attempt to heal extra commas
                String[] p = line.split(",", -1);
                if (p.length != 5) continue;

                String course = p[0].trim();
                String title = p[1].trim();
                String author = p[2].trim();
                String priceStr = p[3].trim();
                String requiredStr = p[4].trim();

                if (title.isEmpty() || author.isEmpty()) continue;

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                    if (price < 0) continue;
                } catch (NumberFormatException e) {
                    continue;
                }

                boolean required = requiredStr.equalsIgnoreCase("Required");
                list.add(new Books(title, author, price, course, required));
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return list;
    }

    /** A quick check for a header row. */
    private static boolean looksLikeHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("course") && lower.contains("title") && lower.contains("author");
    }

    // ===== Option 1 (implemented): list all =====

    private static void listAllBooks(ArrayList<Books> books) {
        if (books.isEmpty()) {
            System.out.println("No data loaded.");
            return;
        }
        System.out.printf("%-30.30s %-22.22s %10s %-10.10s %-10.10s%n",
                "Title","Author","Price","Course","Tag");
        System.out.println("-".repeat(90));
        for (Books b : books) {
            String tag = b.isRequired() ? "[Required]" :
                    (b.getCourseCode().isBlank() ? "—" : "[Optional]");
            String course = b.getCourseCode().isBlank() ? "—" : b.getCourseCode();
            System.out.printf("%-30.30s %-22.22s $%9.2f %-10.10s %-10.10s%n",
                    b.getTitle(), b.getAuthor(), b.getPrice(), course, tag);
        }
        System.out.println("\nTotal: " + books.size() + " books");
    }

    // ===== Option 2 (implemented): search by course =====

    private static void searchByCourse(ArrayList<Books> books, Scanner sc) {
        if (books.isEmpty()) {
            System.out.println("No data loaded.");
            return;
        }
        System.out.print("Enter course code (e.g., CSC 222): ");
        String code = sc.nextLine().trim();
        if (code.isEmpty()) {
            System.out.println("Course code cannot be blank.");
            return;
        }
        String codeNorm = code.toLowerCase(Locale.ROOT);

        int count = 0;
        System.out.printf("%-30.30s %-22.22s %10s %-10.10s %-10.10s%n",
                "Title","Author","Price","Course","Tag");
        System.out.println("-".repeat(90));
        for (Books b : books) {
            if (!b.getCourseCode().isBlank() &&
                    b.getCourseCode().toLowerCase(Locale.ROOT).equals(codeNorm)) {
                String tag = b.isRequired() ? "[Required]" : "[Optional]";
                System.out.printf("%-30.30s %-22.22s $%9.2f %-10.10s %-10.10s%n",
                        b.getTitle(), b.getAuthor(), b.getPrice(), b.getCourseCode(), tag);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("No textbook adoptions found for " + code + ".");
        } else {
            System.out.println("Results: " + count);
        }
    }

    // ===== Option 3 (implemented): save ALL BOOKS report =====

    private static void saveAllBooksReport(ArrayList<Books> books) {
        if (books.isEmpty()) {
            System.out.println("No data loaded.");
            return;
        }
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Path outPath = Paths.get(REPORTS_DIR, "all_books_report.txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath))) {
            out.printf("All Books Report  (%s)%n", ts);
            out.println("---------------------------------------------------------------------------");
            out.printf("%-30.30s %-22.22s %10s %-10.10s %-10.10s%n",
                    "Title","Author","Price","Course","Tag");
            out.println("---------------------------------------------------------------------------");
            double total = 0.0;
            for (Books b : books) {
                String course = b.getCourseCode().isBlank() ? "—" : b.getCourseCode();
                String tag = b.isRequired() ? "[Required]" :
                        (course.equals("—") ? "—" : "[Optional]");
                out.printf("%-30.30s %-22.22s $%9.2f %-10.10s %-10.10s%n",
                        b.getTitle(), b.getAuthor(), b.getPrice(), course, tag);
                total += b.getPrice();
            }
            out.println("---------------------------------------------------------------------------");
            out.printf("Total books: %d%n", books.size());
            out.printf("Grand total value: $%.2f%n", total);
            System.out.println("Saved report: " + outPath.toString());
        } catch (IOException e) {
            System.out.println("Error writing " + outPath + ": " + e.getMessage());
        }
    }

    // ===== Option 4 (YOU implement): save COURSE report (list + totals) =====
    /**
     * TODO: Implement this method.
     * Requirements recap:
     *  - Prompt for a course code (case-insensitive).
     *  - Filter matching rows, but DEDUPLICATE by Title+Author before totaling.
     *  - Compute Required subtotal, Optional subtotal, and Grand total.
     *  - Write to src/data/reports/course_<COURSE>_report.txt with:
     *      • Header (timestamp + course code)
     *      • Aligned table: Title, Author, Price, Tag ([Required]/[Optional])
     *      • Totals section
     *      • Footer "(X unique books; Y duplicates ignored)"
     *  - Print a friendly console message when done.
     */
    private static void saveCourseReport(ArrayList<Books> books, Scanner sc) {
        if (books.isEmpty()) {
            System.out.println("No data loaded.");
            return;
        }

        // the loop prompt is the same pattern as the readInt() method from starter code
        String code;
        while (true) {
            System.out.print("Enter course code (e.g., CSC 222): ");
            code = sc.nextLine().trim();
            if (!code.isEmpty()) {
                break;
            }
            System.out.println("Course code cant be blank.");

        }

        //used the same format from the loadBooks() method from the starter code
        ArrayList<Books> unique = new ArrayList<>();
        int dupCount = 0;
        double requiredTotal = 0.0;
        double optionalTotal = 0.0;

        String codeNorm = code.toLowerCase(Locale.ROOT);
// used format from searchByCourse, and the seenByTitleAuthor method
        for (Books b : books) {
            if (!b.getCourseCode().toLowerCase(Locale.ROOT).equals(codeNorm)) {
                continue;
            }
            if (seenByTitleAuthor(unique, b)) {
                dupCount++;
                continue;
            }
            unique.add(b);
            if (b.isRequired()){
                requiredTotal += b.getPrice();
            }else {
                optionalTotal += b.getPrice();
            }

        }
        // if there is no matches it will show this message, used the same as from the searchByCourse()
        if (unique.isEmpty()) {
            System.out.println("No textbook adoptions found.");
            return;
        }

        // used the output format from saveAllBooksReport option 3
        String fileSafe = code.toUpperCase();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Path outPath = Paths.get(REPORTS_DIR, "course_" + fileSafe.replace(" ", "") + "_report.txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath))) {
            out.printf("Course Report : %s (%s)%n", code.toUpperCase(), ts);
            out.println("---------------------------------------------------------------------------");
            out.printf("%-30.30s %-22.22s %10s %-10.10s%n",
                    "Title","Author","Price","Tag");
            out.println("---------------------------------------------------------------------------");
            for (Books b : unique) {
                String course = b.getCourseCode().isBlank() ? "—" : b.getCourseCode();
                String tag = b.isRequired() ? "[Required]" :
                        (course.equals("—") ? "—" : "[Optional]");
                out.printf("%-30.30s %-22.22s $%9.2f %-10.10s%n",
                        b.getTitle(), b.getAuthor(), b.getPrice(), tag);
            }
            out.println("---------------------------------------------------------------------------");
            out.printf("Required subtotal: $%.2f%n", requiredTotal);
            out.printf("Optional subtotal: $%.2f%n", optionalTotal);
            out.printf("Grand total: $%.2f%n", requiredTotal + optionalTotal);
            out.printf("(%d unique books; %d duplicates ignored)%n", unique.size(), dupCount);
            System.out.println("Saved course report: " + outPath + " (" + unique.size() + " unique: " + dupCount + " duplicates ignored)");
        } catch (IOException e) {
            System.out.println("Error writing " + outPath + ": " + e.getMessage());
        }
    }

    // ===== Option 5 (YOU implement): export TOP-N most expensive =====

    /**
     * TODO: Implement this method.
     * Requirements recap:
     *  - Prompt for N; validate 1 <= N <= books.size(); reprompt on bad input.
     *  - Make a COPY of the list, sort by price DESC, take first N.
     *  - Write to src/data/reports/topN.txt (e.g., top5.txt) with:
     *      • Numbered list: Title, Author, Price (aligned)
     *      • "Top-N total value: $XX.XX"
     *      • "Average of top N: $YY.YY"
     *  - Print a friendly console message when done.
     */
    private static void exportTopN(ArrayList<Books> books, Scanner sc) {
        // TODO: Your code here.
        // HINTS:
        //   • Read/validate N with a loop (similar to readInt but with range checks).
        //   • new ArrayList<>(books) gives you a shallow copy to sort.
        //   • Comparator: (a, b) -> Double.compare(b.getPrice(), a.getPrice())
        //   • Path out = Paths.get(REPORTS_DIR, "top" + N + ".txt")

        // used the same method from saveAllBooksReport to check if there is no data
        if (books.isEmpty()) {
            System.out.println("No data loaded");
            return;
        }

        // using the readInt() method from the starter code to prompt for N and range check
        // similar to the readInt but with range checks
        int n;
        while (true) {
            n = readInt(sc, "Enter N (1-" + books.size() + "): ");
            if (n >= 1 && n <= books.size()) {
                break;
            }
            System.out.println("Enter a number from 1 to " + books.size() + ".");
        }
        // copying and sorting price descending using the ArrayList<>(books) and comparator from the hints
        // sorts its highest price first with the Collections.sort from canvas constraints section

        ArrayList<Books> sorted = new ArrayList<>(books);
        Collections.sort(sorted, (a,b) -> Double.compare(b.getPrice(), a.getPrice()));

        // adds the prices of the first N books but sorted

        double total = 0.0;
        for (int i = 0; i < n; i++) {
            total += sorted.get(i).getPrice();
        }

        // writes the report, using path from hint with the try PrintWriter
        // using same format from the saveAllBooksReport, used file name from hints
        // using same loops for int, sorted.get that used for total

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Path outPath = Paths.get(REPORTS_DIR, "top" + n + ".txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath))) {
            // header from saveAllBooksReport
            out.printf("Top %d Most Expensive  (%s)%n", n, ts);
            out.println("---------------------------------------------------------------------------");
            out.printf("    %-30.30s %-22.22s %10s%n","Title","Author","Price");
            out.println("---------------------------------------------------------------------------");
            for (int i = 0; i < n; i++) {
                Books b = sorted.get(i);
                out.printf("%2d. %-30.30s %-22.22s $%9.2f%n",
                        i +1, b.getTitle(), b.getAuthor(), b.getPrice());
            }
            out.println("---------------------------------------------------------------------------");
            out.printf("Top-N total value: $%.2f%n", total);
            out.printf("Average of top N: $%.2f%n", total / n);
            System.out.println("Saved: " + outPath);
        } catch (IOException e) {
            System.out.println("Error writing " + outPath + ": " + e.getMessage());
        }
    }

    // ===== Option 6 (YOU implement): export PRICE STATS =====

    /**
     * TODO: Implement this method.
     * Requirements recap:
     *  - Compute Count, Min, Max, Average, Median across ALL books.
     *    • Median: sort the prices; for even count, average the middle two.
     *  - Write to src/data/reports/price_stats.txt with two-decimal formatting.
     *  - Print a friendly console message when done.
     */
    private static void exportPriceStats(ArrayList<Books> books) {
        // TODO: Your code here.
        // HINTS:
        //   • Collect prices into ArrayList<Double> and Collections.sort ascending.
        //   • Use printf or String.format("%.2f", value) for two decimals.
        //   • Path out = Paths.get(REPORTS_DIR, "price_stats.txt")

        // used the same method from saveAllBooksReport to check if there is no data
        if (books.isEmpty()) {
            System.out.println("No data loaded");
            return;
        }
        // Collects prices into ArrayList<Double> and Collections.sort ascending.
        ArrayList<Double> prices = new ArrayList<>();
        double total = 0.0;
        for (Books b : books) {
            prices.add(b.getPrice());
            total += b.getPrice();
        }
        Collections.sort(prices);

        //counts min,max,average sorted from min = first and max = last
        int count = prices.size();
        double min = prices.get(0);
        double max = prices.get(count - 1);
        double avg = total / count;

        // median rule sorts the prices; for even count, average the middle two
        // followed canvas order odd in middle, even is the average of the middle two
        double median;
        if (count % 2 == 1) {
            median = prices.get(count / 2);
        }
        else {
            median = (prices.get(count / 2 - 1) + prices.get(count / 2)) / 2;
        }


        // path from hints and the try/printwriter is the same from saveAllBooksReport
        // used labels from the canvas output
        Path outPath = Paths.get(REPORTS_DIR, "price_stats.txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath))) {
            out.printf("Count: %d%n", count);
            out.printf("Min: $%.2f%n", min);
            out.printf("Max: $%.2f%n", max);
            out.printf("Average: $%.2f%n", avg);
            out.printf("Median: $%.2f%n", median);
            System.out.println("Saved: " + outPath);
        } catch (IOException e) {
            System.out.println("Error writing " + outPath + ": " + e.getMessage());
        }
    }

    // ===== Helper you may reuse in case 4 (dedupe by Title + Author) =====

    /** Returns true if list already contains a book with the same Title+Author (case-insensitive). */
    private static boolean seenByTitleAuthor(ArrayList<Books> list, Books b) {
        for (Books x : list) {
            if (x.getTitle().equalsIgnoreCase(b.getTitle())
                    && x.getAuthor().equalsIgnoreCase(b.getAuthor())) {
                return true;
            }
        }
        return false;
    }
}
