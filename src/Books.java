/**
 * Books.java — simple immutable data class for a bookstore row.
 * Fields:
 *   - title:       String (required, trimmed)
 *   - author:      String (required, trimmed)
 *   - price:       double (>= 0)
 *   - courseCode:  String ("" if inventory-only)
 *   - required:    boolean (true = "Required", false = "Optional"/blank)
 *
 * You DO NOT change this class for IDE #3.
 */
public class Books {
    private final String title;
    private final String author;
    private final double price;
    private final String courseCode; // "" if inventory-only (no course adoption)
    private final boolean required;  // true -> Required, false -> Optional/blank

    public Books(String title, String author, double price, String courseCode, boolean required) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("title required");
        if (author == null || author.trim().isEmpty()) throw new IllegalArgumentException("author required");
        if (price < 0) throw new IllegalArgumentException("price must be >= 0");
        this.title = title.trim();
        this.author = author.trim();
        this.price = price;
        this.courseCode = (courseCode == null) ? "" : courseCode.trim();
        this.required = required;
    }

    public String getTitle()      { return title; }
    public String getAuthor()     { return author; }
    public double getPrice()      { return price; }
    public String getCourseCode() { return courseCode; }
    public boolean isRequired()   { return required; }

    /** CSV helper (not required for the assignment, but handy for debugging). */
    public String toCsv() {
        return String.format("%s,%s,%.2f,%s,%s",
                title, author, price, courseCode, required ? "Required" : "Optional");
    }
}
