package store.ui;

import store.io.DataStore;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.ReportService;
import store.service.SalesService;

/** Main menu of the Clothing Sales Management System. */
public class MainMenu {

    private final ProductService productService;
    private final CustomerService customerService;
    private final SalesService salesService;
    private final ReportService reportService;
    private final DataStore dataStore;

    public MainMenu(ProductService productService, CustomerService customerService,
                    SalesService salesService, ReportService reportService,
                    DataStore dataStore) {
        this.productService = productService;
        this.customerService = customerService;
        this.salesService = salesService;
        this.reportService = reportService;
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            System.out.println("==========================================");
            System.out.println("    CLOTHING SALES MANAGEMENT SYSTEM      ");
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
                case 1: new ProductMenu(productService, dataStore).run(); break;
                case 2: new CustomerMenu(customerService, dataStore).run(); break;
                case 3: new SalesMenu(salesService, customerService, productService, dataStore).run(); break;
                case 4: new InventoryMenu(productService, dataStore).run(); break;
                case 5: new ReportMenu(reportService).run(); break;
                case 6:
                    dataStore.saveAll();
                    System.out.println("Data saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please choose 1-6.");
            }
        }
    }
}
