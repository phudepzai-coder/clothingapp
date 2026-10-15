package store.ui;

import java.text.DecimalFormat;
import java.util.Scanner;

public class ConsoleIO {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DecimalFormat df = new DecimalFormat("#,###");

    public static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String readOptionalLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    public static long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(",", "").replace(".", "");
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please try again.");
            }
        }
    }

    public static String formatMoney(long amount) {
        return df.format(amount);
    }

    public static void pressEnterToReturn() {
        System.out.println("\nPress ENTER to return...");
        scanner.nextLine();
    }
}
