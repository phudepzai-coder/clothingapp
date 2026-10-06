package store.ui;

import store.exceptions.DuplicateIdException;
import store.exceptions.InsufficientStockException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.SalesService;

import java.util.ArrayList;
import java.util.List;

/** SALES MANAGEMENT menu (Tasks B8 - B10 + history). */
public class SalesMenu {

    private final SalesService salesService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final DataStore dataStore;

    public SalesMenu(SalesService salesService, CustomerService customerService,
                     ProductService productService, DataStore dataStore) {
        this.salesService = salesService;
        this.customerService = customerService;
        this.productService = productService;
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("---------- SALES MANAGEMENT ----------");
            System.out.println("1. Create Sales Transaction");
            System.out.println("2. Add Product to Transaction");
            System.out.println("3. Calculate Total Bill / Confirm Sale");
            System.out.println("4. View Transaction Details");
            System.out.println("5. View Transaction History");
            System.out.println("6. Back");
            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1: createTransaction(); break;
                case 2: addProductToTransaction(); break;
                case 3: calculateBill(); break;
                case 4: viewTransactionDetails(); break;
                case 5: viewHistory(); break;
                case 6: return;
                default: System.out.println("Invalid option. Please choose 1-6.");
            }
        }
    }

    /** Task B8 - Create Sales Transaction. */
    private void createTransaction() {
        System.out.println("---------- CREATE SALES TRANSACTION ----------");
        String id = ConsoleIO.readRequired("Transaction ID: ");
        String customerId = ConsoleIO.readRequired("Customer ID: ");
        String date = ConsoleIO.readRequired("Date (dd/MM/yyyy): ");

        System.out.println("[1] Continue   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            salesService.createOrder(id, customerId, date);
            dataStore.saveAll();
            System.out.println("Transaction created successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to create transaction. Transaction ID already exists.");
        } catch (NotFoundException e) {
            System.out.println("Failed to create transaction. Customer not found.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to create transaction. " + e.getMessage());
        }
    }

    /** Task B9 - Add Product to Transaction. */
    private void addProductToTransaction() {
        System.out.println("---------- ADD PRODUCT TO TRANSACTION ----------");
        String tid = ConsoleIO.readRequired("Transaction ID: ");
        Order order = salesService.findById(tid);
        if (order == null) {
            System.out.println("Failed. Transaction not found.");
            return;
        }
        if (!order.isPending()) {
            System.out.println("Failed. Transaction is already completed.");
            return;
        }
        String pid = ConsoleIO.readRequired("Product ID: ");
        Product p = productService.findById(pid);
        if (p == null) {
            System.out.println("Failed. Product not found.");
            return;
        }
        System.out.println("Product Name: " + p.getName());
        System.out.println("Available Stock: " + p.getQuantity());
        int qty = ConsoleIO.readPositiveInt("Quantity: ");

        System.out.println("[1] Add   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            salesService.addProductToOrder(tid, pid, qty);
            dataStore.saveAll();
            System.out.println("Product added to transaction successfully.");
        } catch (InsufficientStockException e) {
            System.out.println("Failed to add product. Insufficient stock.");
        } catch (NotFoundException | InvalidInputException e) {
            System.out.println("Failed to add product. " + e.getMessage());
        }
    }

    /** Task B10 - Calculate Total Bill, then optionally confirm the sale. */
    private void calculateBill() {
        System.out.println("---------- BILL SUMMARY ----------");
        String tid = ConsoleIO.readRequired("Transaction ID: ");
        Order order = salesService.findById(tid);
        if (order == null) {
            System.out.println("Failed. Transaction not found.");
            return;
        }
        if (!order.hasProducts()) {
            System.out.println("Failed. A sales transaction must contain at least one product.");
            ConsoleIO.pause();
            return;
        }
        Customer customer = customerService.findById(order.getCustomerId());
        if (customer == null) {
            System.out.println("Failed. Customer of this transaction no longer exists.");
            ConsoleIO.pause();
            return;
        }
        printBill(order, customer);

        if (!order.isPending()) {
            System.out.println("This transaction has already been completed.");
            ConsoleIO.pause();
            return;
        }
        System.out.println("[1] Confirm Sale   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Sale cancelled. The transaction remains pending.");
            ConsoleIO.pause();
            return;
        }
        try {
            salesService.confirmSale(tid);
            dataStore.saveAll();
            System.out.println("Sale completed successfully.");
        } catch (InsufficientStockException e) {
            System.out.println("Failed to confirm sale. " + e.getMessage());
        } catch (NotFoundException | InvalidInputException e) {
            System.out.println("Failed to confirm sale. " + e.getMessage());
        }
        ConsoleIO.pause();
    }

    /** Task: View Transaction Details (read-only bill view). */
    private void viewTransactionDetails() {
        String tid = ConsoleIO.readRequired("Transaction ID: ");
        Order order = salesService.findById(tid);
        if (order == null) {
            System.out.println("Transaction not found.");
            ConsoleIO.pause();
            return;
        }
        Customer customer = customerService.findById(order.getCustomerId());
        printBill(order, customer);
        ConsoleIO.pause();
    }

    /** Task: View Transaction History. */
    private void viewHistory() {
        System.out.println("-------------- TRANSACTION HISTORY --------------");
        System.out.printf("%-8s %-10s %-12s %-14s %-12s %15s%n",
                "ID", "Date", "Customer", "Membership", "Status", "Final Amount");
        System.out.println("------------------------------------------------------------------");
        List<Order> all = new ArrayList<>(salesService.getAll());
        boolean any = false;
        for (Order o : all) {
            Customer c = customerService.findById(o.getCustomerId());
            long amount = 0;
            if (o.isCompleted() && c != null) {
                amount = o.getFinalAmount(c);
            }
            System.out.printf("%-8s %-10s %-12s %-14s %-12s %,15d%n",
                    o.getId(), o.getDate(),
                    c != null ? c.getName() : o.getCustomerId(),
                    c != null ? c.getType() : "-",
                    o.getStatus(), amount);
            any = true;
        }
        System.out.println("------------------------------------------------------------------");
        if (!any) {
            System.out.println("No transactions found.");
        }
        ConsoleIO.pause();
    }

    /** Bill summary exactly like the sample output. */
    private void printBill(Order order, Customer customer) {
        System.out.println("Transaction ID: " + order.getId());
        System.out.println("Customer: " + (customer != null ? customer.getName() : order.getCustomerId()));
        System.out.println("Membership: " + (customer != null ? customer.getType() : "-"));
        System.out.println();
        System.out.printf("%-16s %5s %12s %14s%n", "Product", "Qty", "Price", "Amount");
        System.out.println("--------------------------------------------------");
        for (OrderDetail d : order.getDetails()) {
            System.out.printf("%-16s %5d %,12d %,14d%n",
                    d.getProductName(), d.getQuantity(), d.getUnitPrice(), d.getAmount());
        }
        System.out.println("--------------------------------------------------");
        long subtotal = order.getSubtotal();
        long discount = customer != null ? order.getDiscount(customer) : 0;
        long total = order.getFinalAmount(customer);
        System.out.printf("%-34s %,14d%n", "Subtotal:", subtotal);
        String label = customer != null && customer.getDiscountRate() > 0
                ? customer.getType() + " Discount (" + customer.getDiscountRate() + "%):"
                : "Discount:";
        System.out.printf("%-34s %,14d%n", label, discount);
        System.out.println("--------------------------------------------------");
        System.out.printf("%-34s %,14d%n", "Total Amount:", total);
        System.out.println("--------------------------------------------------");
    }
}
