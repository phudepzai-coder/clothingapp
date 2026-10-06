package store;

import store.io.DataStore;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.ReportService;
import store.service.SalesService;
import store.ui.MainMenu;
import store.web.WebServer;

import java.nio.file.Paths;

/**
 * Entry point of the Clothing Sales Management System.
 * Wires services together, loads saved data, and shows the main menu.
 *
 * Usage:
 *   java store.Main        -> console menu + web UI at http://localhost:8080
 *   java store.Main web    -> web UI only (opens the browser automatically)
 */
public class Main {

    public static void main(String[] args) {
        ProductService productService = new ProductService();
        CustomerService customerService = new CustomerService();
        SalesService salesService = new SalesService(productService, customerService);
        ReportService reportService =
                new ReportService(salesService, productService, customerService);

        DataStore dataStore = new DataStore("data", productService, customerService, salesService);
        dataStore.loadAll();
        System.out.println("Data loaded from ./data (products, customers, orders).");

        WebServer webServer = null;
        try {
            webServer = new WebServer(8080, productService, customerService,
                    salesService, reportService, dataStore, Paths.get("web"));
            webServer.start();
            System.out.println("Web UI running at http://localhost:" + webServer.getPort());
        } catch (Exception e) {
            System.out.println("[Warn] Could not start web server: " + e.getMessage());
        }

        boolean webOnly = args.length > 0 && args[0].equalsIgnoreCase("web");
        if (webOnly) {
            if (webServer != null) {
                webServer.openBrowser();
                System.out.println("Press Ctrl+C to stop the server.");
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException ignored) {
                }
            }
            return;
        }

        new MainMenu(productService, customerService, salesService, reportService, dataStore).run();
    }
}
