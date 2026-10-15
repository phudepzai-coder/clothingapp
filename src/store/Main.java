package store;

import store.io.DataStore;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.ReportService;
import store.service.SalesService;
import store.ui.MainMenu;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        ProductService productService = new ProductService();
        CustomerService customerService = new CustomerService();
        SalesService salesService = new SalesService(productService, customerService);
        ReportService reportService = new ReportService(productService, customerService, salesService);

        Path dataDir = Paths.get("data");
        DataStore dataStore = new DataStore(dataDir, productService, customerService, salesService);
        dataStore.init();

        // Graceful shutdown hook to save data on abrupt exit
        Runtime.getRuntime().addShutdownHook(new Thread(dataStore::saveAll));

        MainMenu menu = new MainMenu(productService, customerService, salesService, reportService, dataStore);
        menu.start();
    }
}
