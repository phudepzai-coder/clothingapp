package store.ui;

import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Product;
import store.service.ProductService;

import java.util.List;

public class InventoryMenu {
    private final ProductService productService;
    private final DataStore dataStore;

    public InventoryMenu(ProductService productService, DataStore dataStore) {
        this.productService = productService;
        this.dataStore = dataStore;
    }

    public void display() {
        while (true) {
            System.out.println("\n---------------- INVENTORY MANAGEMENT ----------------");
            System.out.println("1. Low Stock Products (Task B11)");
            System.out.println("2. Update Product Stock (Task B12)");
            System.out.println("3. Back to Main Menu");
            System.out.println("------------------------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    lowStockProducts();
                    break;
                case 2:
                    updateStock();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 3.");
            }
        }
    }

    private void lowStockProducts() {
        List<Product> list = productService.getLowStockProducts();
        System.out.println("\n----------- LOW STOCK PRODUCTS -----------");
        System.out.printf("%-6s %-16s %-6s %-10s %-6s%n", "ID", "Product Name", "Size", "Color", "Stock");
        System.out.println("------------------------------------------------");
        for (Product p : list) {
            System.out.printf("%-6s %-16s %-6s %-10s %-6d%n",
                    p.getId(), p.getName(), p.getSize(), p.getColor(), p.getQuantity());
        }
        System.out.println("------------------------------------------------");
        ConsoleIO.pressEnterToReturn();
    }

    private void updateStock() {
        System.out.println("\n----------- UPDATE STOCK -----------");
        String id = ConsoleIO.readLine("Product ID: ");
        Product p = productService.findById(id);
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        System.out.println("Product Name: " + p.getName());
        System.out.println("Current Stock: " + p.getQuantity());
        int newQty = ConsoleIO.readInt("Enter new quantity: ");

        int action = ConsoleIO.readInt("[1] Update  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            productService.updateStock(id, newQty);
            dataStore.saveProducts();
            System.out.println("Output:\nProduct stock updated successfully.");
        } catch (NotFoundException | InvalidInputException e) {
            System.out.println("Failed to update stock. " + e.getMessage());
        }
    }
}
