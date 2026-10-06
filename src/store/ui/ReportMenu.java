package store.ui;

import store.model.Product;
import store.service.ReportService;

import java.util.List;

/** REPORTS menu (Tasks B13 - B16 + total revenue). */
public class ReportMenu {

    private final ReportService reportService;

    public ReportMenu(ReportService reportService) {
        this.reportService = reportService;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("---------- REPORTS ----------");
            System.out.println("1. Monthly Sales Report");
            System.out.println("2. Best-Selling Products");
            System.out.println("3. Highest-Spending Customers");
            System.out.println("4. Low Stock Report");
            System.out.println("5. Total Revenue for a Month");
            System.out.println("6. Back");
            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1: monthlyReport(); break;
                case 2: bestSelling(); break;
                case 3: highestSpending(); break;
                case 4: lowStockReport(); break;
                case 5: totalRevenue(); break;
                case 6: return;
                default: System.out.println("Invalid option. Please choose 1-6.");
            }
        }
    }

    /** Task B13 - Monthly Sales Report. */
    private void monthlyReport() {
        System.out.println("---------- MONTHLY SALES REPORT ----------");
        int month = ConsoleIO.readInt("Month (1-12): ");
        int year = ConsoleIO.readInt("Year (e.g. 2025): ");
        ReportService.MonthlyReport r = reportService.monthlyReport(month, year);
        System.out.println();
        System.out.println("Total Transactions: " + r.transactions);
        System.out.println("Total Products Sold: " + r.productsSold);
        System.out.printf("Total Revenue: %,d VND%n", r.revenue);
        System.out.println("------------------------------------------");
        ConsoleIO.pause();
    }

    /** Task B14 - Best-Selling Products (BR19). */
    private void bestSelling() {
        System.out.println("---------- BEST-SELLING PRODUCTS ----------");
        int topN = ConsoleIO.readInt("Show top N products (e.g. 10): ");
        List<Object[]> rows = reportService.bestSellingProducts(topN);
        if (rows.isEmpty()) {
            System.out.println("No sales data available.");
            ConsoleIO.pause();
            return;
        }
        System.out.printf("%-12s %-24s %14s%n", "Product ID", "Product Name", "Quantity Sold");
        System.out.println("--------------------------------------------------");
        for (Object[] row : rows) {
            System.out.printf("%-12s %-24s %14d%n", row[0], row[1], ((Number) row[2]).longValue());
        }
        System.out.println("--------------------------------------------------");
        ConsoleIO.pause();
    }

    /** Task B15 - Highest-Spending Customers (BR20). */
    private void highestSpending() {
        System.out.println("---------- HIGHEST-SPENDING CUSTOMERS ----------");
        int topN = ConsoleIO.readInt("Show top N customers (e.g. 10): ");
        List<Object[]> rows = reportService.highestSpendingCustomers(topN);
        if (rows.isEmpty()) {
            System.out.println("No sales data available.");
            ConsoleIO.pause();
            return;
        }
        System.out.printf("%-12s %-22s %18s%n", "Customer ID", "Customer Name", "Total Purchase");
        System.out.println("------------------------------------------------------");
        for (Object[] row : rows) {
            System.out.printf("%-12s %-22s %,18d%n", row[0], row[1], ((Number) row[2]).longValue());
        }
        System.out.println("------------------------------------------------------");
        ConsoleIO.pause();
    }

    /** Task B16 - Low Stock Report (BR18). */
    private void lowStockReport() {
        System.out.println("---------- LOW STOCK REPORT ----------");
        List<Product> list = reportService.lowStockReport();
        if (list.isEmpty()) {
            System.out.println("No low stock products.");
            ConsoleIO.pause();
            return;
        }
        System.out.printf("%-12s %-24s %8s%n", "Product ID", "Product Name", "Stock");
        System.out.println("----------------------------------------------");
        for (Product p : list) {
            System.out.printf("%-12s %-24s %8d%n", p.getId(), p.getName(), p.getQuantity());
        }
        System.out.println("----------------------------------------------");
        ConsoleIO.pause();
    }

    /** Total revenue for a selected month (BR21). */
    private void totalRevenue() {
        System.out.println("---------- TOTAL REVENUE ----------");
        int month = ConsoleIO.readInt("Month (1-12): ");
        int year = ConsoleIO.readInt("Year (e.g. 2025): ");
        ReportService.MonthlyReport r = reportService.monthlyReport(month, year);
        System.out.printf("Total revenue for %02d/%d: %,d VND (%d completed transactions)%n",
                month, year, r.revenue, r.transactions);
        ConsoleIO.pause();
    }
}
