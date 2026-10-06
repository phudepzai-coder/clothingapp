package store.ui;

import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Product;
import store.service.ProductService;

import java.util.List;

/** INVENTORY MANAGEMENT menu (Tasks B11 - B12). */
public class InventoryMenu {

    private final ProductService productService;
    private final DataStore dataStore;

    public InventoryMenu(ProductService productService, DataStore dataStore) {
        this.productService = productService;
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("---------- INVENTORY MANAGEMENT ----------");
            System.out.println("1. View Low Stock Products");
            System.out.println("2. Update Product Stock");
            System.out.println("3. Back");
            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1: lowStock(); break;
                case 2: updateStock(); break;
                case 3: return;
                default: System.out.println("Invalid option. Please choose 1-3.");
            }
        }
    }

    /** Task B11 - Low Stock Products (BR18: quantity <= 5). */
    private void lowStock() {
        System.out.println("---------- LOW STOCK PRODUCTS ----------");
        List<Product> list = productService.getLowStock();
        if (list.isEmpty()) {
            System.out.println("No low stock products. Inventory is healthy.");
            ConsoleIO.pause();
            return;
        }
        System.out.printf("%-6s %-20s %-6s %-10s %6s%n", "ID", "Product Name", "Size", "Color", "Stock");
        System.out.println("-----------------------------------------------------");
        for (Product p : list) {
            System.out.printf("%-6s %-20s %-6s %-10s %6d%n",
                    p.getId(), p.getName(), p.getSize(), p.getColor(), p.getQuantity());
        }
        System.out.println("-----------------------------------------------------");
        ConsoleIO.pause();
    }

    /** Task B12 - Update Product Stock. */
    private void updateStock() {
        System.out.println("---------- UPDATE STOCK ----------");
        String id = ConsoleIO.readRequired("Product ID: ");
        Product p = productService.findById(id);
        if (p == null) {
            System.out.println("Failed. Product not found.");
            return;
        }
        System.out.println("Product Name: " + p.getName());
        System.out.println("Current Stock: " + p.getQuantity());
        int newQty = ConsoleIO.readNonNegativeInt("Enter new quantity: ");

        System.out.println("[1] Update   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            productService.updateStock(id, newQty);
            dataStore.saveAll();
            System.out.println("Product stock updated successfully.");
        } catch (NotFoundException e) {
            System.out.println("Failed to update. Product not found.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to update. " + e.getMessage());
        }
    }
}
