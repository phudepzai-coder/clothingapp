package store.ui;

import java.util.Scanner;

/** Console input helpers with validation and error handling. */
public final class ConsoleIO {

    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleIO() {
    }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        String line = SCANNER.nextLine();
        return line == null ? "" : line.trim();
    }

    /** Reads a non-empty string; re-prompts until valid. */
    public static String readRequired(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Value must not be empty. Please try again.");
        }
    }

    /** Reads an integer; re-prompts until valid. */
    public static int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    /** Reads a positive integer (> 0); re-prompts until valid. */
    public static int readPositiveInt(String prompt) {
        while (true) {
            int n = readInt(prompt);
            if (n > 0) {
                return n;
            }
            System.out.println("Number must be greater than zero.");
        }
    }

    /** Reads a non-negative integer (>= 0); re-prompts until valid. */
    public static int readNonNegativeInt(String prompt) {
        while (true) {
            int n = readInt(prompt);
            if (n >= 0) {
                return n;
            }
            System.out.println("Number cannot be negative.");
        }
    }

    /** Reads a price (VND) as a number with optional thousand separators. */
    public static long readPrice(String prompt) {
        while (true) {
            String s = readLine(prompt).replace(",", "").replace(".", "");
            try {
                long v = Long.parseLong(s);
                if (v > 0) return v;
                System.out.println("Price must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number, e.g. 250000 or 250,000.");
            }
        }
    }

    /** Reads a money amount that may be 0 or positive (for stock updates). */
    public static long readAmountOrZero(String prompt) {
        while (true) {
            String s = readLine(prompt).replace(",", "").replace(".", "");
            try {
                long v = Long.parseLong(s);
                if (v >= 0) return v;
                System.out.println("Number cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    public static void pause() {
        System.out.print("Press ENTER to return...");
        SCANNER.nextLine();
    }
}
