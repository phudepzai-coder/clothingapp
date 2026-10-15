package store.ui;

import store.io.DataStore;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.ReportService;
import store.service.SalesService;

public class MainMenu {
    private final ProductMenu productMenu;
    private final CustomerMenu customerMenu;
    private final SalesMenu salesMenu;
    private final InventoryMenu inventoryMenu;
    private final ReportMenu reportMenu;
    private final DataStore dataStore;

    public MainMenu(ProductService productService, CustomerService customerService,
                    SalesService salesService, ReportService reportService, DataStore dataStore) {
        this.productMenu = new ProductMenu(productService, dataStore);
        this.customerMenu = new CustomerMenu(customerService, dataStore);
        this.salesMenu = new SalesMenu(salesService, productService, customerService, dataStore);
        this.inventoryMenu = new InventoryMenu(productService, dataStore);
        this.reportMenu = new ReportMenu(reportService);
        this.dataStore = dataStore;
    }

    public void start() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("     CLOTHING SALES MANAGEMENT SYSTEM     ");
            System.out.println("==========================================");
            System.out.println("1. Manage Products");
            System.out.println("2. Manage Customers");
            System.out.println("3. Sales Management");
            System.out.println("4. Inventory Management");
            System.out.println("5. Reports");
            System.out.println("6. Exit");
            System.out.println("------------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    productMenu.display();
                    break;
                case 2:
                    customerMenu.display();
                    break;
                case 3:
                    salesMenu.display();
                    break;
                case 4:
                    inventoryMenu.display();
                    break;
                case 5:
                    reportMenu.display();
                    break;
                case 6:
                    dataStore.saveAll();
                    System.out.println("\nData saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 6.");
            }
        }
    }
}
