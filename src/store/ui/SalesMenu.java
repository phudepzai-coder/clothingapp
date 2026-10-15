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

import java.util.Collection;

public class SalesMenu {
    private final SalesService salesService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final DataStore dataStore;

    public SalesMenu(SalesService salesService, ProductService productService, CustomerService customerService, DataStore dataStore) {
        this.salesService = salesService;
        this.productService = productService;
        this.customerService = customerService;
        this.dataStore = dataStore;
    }

    public void display() {
        while (true) {
            System.out.println("\n---------------- SALES MANAGEMENT ----------------");
            System.out.println("1. Create Sales Transaction (Task B8)");
            System.out.println("2. Add Product to Transaction (Task B9)");
            System.out.println("3. Calculate Total Bill / Confirm Sale (Task B10)");
            System.out.println("4. View Transaction History");
            System.out.println("5. Cancel Pending Transaction");
            System.out.println("6. Back to Main Menu");
            System.out.println("--------------------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    createTransaction();
                    break;
                case 2:
                    addProductToTransaction();
                    break;
                case 3:
                    calculateTotalBill();
                    break;
                case 4:
                    viewHistory();
                    break;
                case 5:
                    cancelTransaction();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 6.");
            }
        }
    }

    private void createTransaction() {
        System.out.println("\n----------- CREATE SALES TRANSACTION -----------");
        String id = ConsoleIO.readLine("Transaction ID: ");
        String customerId = ConsoleIO.readLine("Customer ID: ");
        String date = ConsoleIO.readLine("Date: ");

        int action = ConsoleIO.readInt("[1] Continue  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            salesService.createOrder(id, customerId, date);
            dataStore.saveOrders();
            System.out.println("Output:\nTransaction created successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to create transaction. Transaction ID already exists.");
        } catch (NotFoundException e) {
            System.out.println("Failed to create transaction. Customer not found.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to create transaction. " + e.getMessage());
        }
    }

    private void addProductToTransaction() {
        System.out.println("\n----------- ADD PRODUCT TO TRANSACTION -----------");
        String orderId = ConsoleIO.readLine("Transaction ID: ");
        Order o = salesService.findById(orderId);
        if (o == null) {
            System.out.println("Transaction not found.");
            return;
        }
        if (!"PENDING".equalsIgnoreCase(o.getStatus())) {
            System.out.println("This transaction is already completed.");
            return;
        }

        String productId = ConsoleIO.readLine("Product ID: ");
        Product p = productService.findById(productId);
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        System.out.println("Product Name: " + p.getName());
        System.out.println("Available Stock: " + p.getQuantity());
        int quantity = ConsoleIO.readInt("Quantity: ");

        int action = ConsoleIO.readInt("[1] Add  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            salesService.addProductToOrder(orderId, productId, quantity);
            dataStore.saveOrders();
            System.out.println("Output:\nProduct added to transaction successfully.");
        } catch (InsufficientStockException e) {
            System.out.println("Failed to add product. Insufficient stock.");
        } catch (Exception e) {
            System.out.println("Failed to add product. " + e.getMessage());
        }
    }

    private void calculateTotalBill() {
        String orderId = ConsoleIO.readLine("\nEnter Transaction ID: ");
        Order o = salesService.findById(orderId);
        if (o == null) {
            System.out.println("Transaction not found.");
            return;
        }

        Customer c = customerService.findById(o.getCustomerId());
        String customerName = (c != null) ? c.getName() : "Unknown";
        String membership = (c != null) ? c.getType() : "Regular";

        System.out.println("\n---------------- BILL SUMMARY ----------------");
        System.out.println("Transaction ID: " + o.getId());
        System.out.println("Customer: " + customerName);
        System.out.println("Membership: " + membership);
        System.out.println("Date: " + o.getDate());
        System.out.println("Status: " + o.getStatus());

        System.out.printf("%-18s %-6s %-12s %-12s%n", "Product", "Qty", "Price", "Amount");
        System.out.println("------------------------------------------------");
        for (OrderDetail d : o.getDetails()) {
            System.out.printf("%-18s %-6d %-12s %-12s%n",
                    d.getProductName(), d.getQuantity(),
                    ConsoleIO.formatMoney(d.getUnitPrice()),
                    ConsoleIO.formatMoney(d.getAmount()));
        }
        System.out.println("------------------------------------------------");

        long subtotal = o.getSubtotal();
        long discount = o.getDiscount(c);
        long total = o.getFinalAmount(c);

        System.out.printf("Subtotal: %s VND%n", ConsoleIO.formatMoney(subtotal));
        if ("VIP".equalsIgnoreCase(membership)) {
            System.out.printf("VIP Discount (10%%): %s VND%n", ConsoleIO.formatMoney(discount));
        } else {
            System.out.printf("Discount (0%%): 0 VND%n");
        }
        System.out.println("------------------------------------------------");
        System.out.printf("Total Amount: %s VND%n", ConsoleIO.formatMoney(total));
        System.out.println("------------------------------------------------");

        if ("PENDING".equalsIgnoreCase(o.getStatus())) {
            int action = ConsoleIO.readInt("[1] Confirm Sale  [2] Cancel\nChoose: ");
            if (action == 1) {
                try {
                    salesService.confirmSale(orderId);
                    dataStore.saveAll();
                    System.out.println("Output:\nSale completed successfully.");
                } catch (InsufficientStockException e) {
                    System.out.println("Failed to complete sale. Insufficient stock.");
                } catch (Exception e) {
                    System.out.println("Failed to complete sale. " + e.getMessage());
                }
            }
        } else {
            ConsoleIO.pressEnterToReturn();
        }
    }

    private void viewHistory() {
        Collection<Order> all = salesService.getAll();
        System.out.println("\n---------------- TRANSACTION HISTORY ----------------");
        System.out.printf("%-8s %-12s %-8s %-10s %-12s%n", "ID", "Date", "CustID", "Status", "Amount");
        System.out.println("-----------------------------------------------------");
        for (Order o : all) {
            Customer c = customerService.findById(o.getCustomerId());
            System.out.printf("%-8s %-12s %-8s %-10s %s VND%n",
                    o.getId(), o.getDate(), o.getCustomerId(), o.getStatus(),
                    ConsoleIO.formatMoney(o.getFinalAmount(c)));
        }
        System.out.println("-----------------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void cancelTransaction() {
        String id = ConsoleIO.readLine("\nEnter Transaction ID to cancel: ");
        try {
            salesService.cancelPendingOrder(id);
            dataStore.saveOrders();
            System.out.println("Transaction cancelled successfully.");
        } catch (Exception e) {
            System.out.println("Failed to cancel transaction. " + e.getMessage());
        }
    }
}
