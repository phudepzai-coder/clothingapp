package store.ui;

import store.model.Product;
import store.service.ReportService;

import java.util.List;

public class ReportMenu {
    private final ReportService reportService;

    public ReportMenu(ReportService reportService) {
        this.reportService = reportService;
    }

    public void display() {
        while (true) {
            System.out.println("\n---------------- REPORTS ----------------");
            System.out.println("1. Monthly Sales Report (Task B13)");
            System.out.println("2. Daily Sales Report");
            System.out.println("3. Best-Selling Products (Task B14)");
            System.out.println("4. Highest-Spending Customers (Task B15)");
            System.out.println("5. Low Stock Report (Task B16)");
            System.out.println("6. Back to Main Menu");
            System.out.println("-----------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    monthlyReport();
                    break;
                case 2:
                    dailyReport();
                    break;
                case 3:
                    bestSellingProducts();
                    break;
                case 4:
                    highestSpendingCustomers();
                    break;
                case 5:
                    lowStockReport();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 6.");
            }
        }
    }

    private void monthlyReport() {
        System.out.println("\n----------- MONTHLY SALES REPORT -----------");
        int month = ConsoleIO.readInt("Enter Month (1-12): ");
        int year = ConsoleIO.readInt("Enter Year: ");

        ReportService.ReportSummary r = reportService.monthlyReport(month, year);
        System.out.printf("Month / Year: %d / %d%n", month, year);
        System.out.printf("Total Transactions: %d%n", r.transactions);
        System.out.printf("Total Products Sold: %d%n", r.productsSold);
        System.out.printf("Total Revenue: %s VND%n", ConsoleIO.formatMoney(r.revenue));
        System.out.println("--------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void dailyReport() {
        System.out.println("\n----------- DAILY SALES REPORT -----------");
        int day = ConsoleIO.readInt("Enter Day (1-31): ");
        int month = ConsoleIO.readInt("Enter Month (1-12): ");
        int year = ConsoleIO.readInt("Enter Year: ");

        ReportService.ReportSummary r = reportService.dailyReport(day, month, year);
        System.out.printf("Date: %02d/%02d/%d%n", day, month, year);
        System.out.printf("Total Transactions: %d%n", r.transactions);
        System.out.printf("Total Products Sold: %d%n", r.productsSold);
        System.out.printf("Total Revenue: %s VND%n", ConsoleIO.formatMoney(r.revenue));
        System.out.println("--------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void bestSellingProducts() {
        System.out.println("\n----------- BEST-SELLING PRODUCTS -----------");
        List<ReportService.BestSellingItem> list = reportService.bestSellingProducts(10);
        System.out.printf("%-12s %-20s %-15s%n", "Product ID", "Product Name", "Quantity Sold");
        System.out.println("------------------------------------------------");
        for (ReportService.BestSellingItem item : list) {
            System.out.printf("%-12s %-20s %-15d%n",
                    item.productId, item.productName, item.quantitySold);
        }
        System.out.println("------------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void highestSpendingCustomers() {
        System.out.println("\n----------- HIGHEST-SPENDING CUSTOMERS -----------");
        List<ReportService.TopCustomerItem> list = reportService.highestSpendingCustomers(10);
        System.out.printf("%-14s %-20s %-16s%n", "Customer ID", "Customer Name", "Total Purchase");
        System.out.println("----------------------------------------------------");
        for (ReportService.TopCustomerItem item : list) {
            System.out.printf("%-14s %-20s %s VND%n",
                    item.customerId, item.customerName, ConsoleIO.formatMoney(item.totalPurchase));
        }
        System.out.println("----------------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void lowStockReport() {
        System.out.println("\n----------- LOW STOCK REPORT -----------");
        List<Product> list = reportService.lowStockReport();
        System.out.printf("%-12s %-20s %-8s%n", "Product ID", "Product Name", "Stock");
        System.out.println("------------------------------------------");
        for (Product p : list) {
            System.out.printf("%-12s %-20s %-8d%n",
                    p.getId(), p.getName(), p.getQuantity());
        }
        System.out.println("------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }
}
