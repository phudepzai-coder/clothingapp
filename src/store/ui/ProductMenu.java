package store.ui;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Product;
import store.service.ProductService;

import java.util.Collection;
import java.util.List;

public class ProductMenu {
    private final ProductService productService;
    private final DataStore dataStore;

    public ProductMenu(ProductService productService, DataStore dataStore) {
        this.productService = productService;
        this.dataStore = dataStore;
    }

    public void display() {
        while (true) {
            System.out.println("\n---------------- PRODUCT MANAGEMENT ----------------");
            System.out.println("1. Add Product (Task B1)");
            System.out.println("2. Update Product (Task B2)");
            System.out.println("3. View All Products (Task B3)");
            System.out.println("4. Search Product (Task B4)");
            System.out.println("5. View Available Products");
            System.out.println("6. Remove Product");
            System.out.println("7. Back to Main Menu");
            System.out.println("-----------------------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    addProduct();
                    break;
                case 2:
                    updateProduct();
                    break;
                case 3:
                    viewAllProducts();
                    break;
                case 4:
                    searchProduct();
                    break;
                case 5:
                    viewAvailableProducts();
                    break;
                case 6:
                    removeProduct();
                    break;
                case 7:
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 7.");
            }
        }
    }

    private void addProduct() {
        System.out.println("\n----------- ADD CLOTHING PRODUCT -----------");
        String id = ConsoleIO.readLine("Product ID: ");
        String name = ConsoleIO.readLine("Product Name: ");
        String category = ConsoleIO.readLine("Category: ");
        String size = ConsoleIO.readLine("Size: ");
        String color = ConsoleIO.readLine("Color: ");
        long price = ConsoleIO.readLong("Price: ");
        int quantity = ConsoleIO.readInt("Quantity: ");

        int action = ConsoleIO.readInt("[1] Save  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            Product p = new Product(id, name, category, size, color, price, quantity);
            productService.add(p);
            dataStore.saveProducts();
            System.out.println("Output:\nProduct added successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to add product. Product ID already exists.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to add product. " + e.getMessage());
        }
    }

    private void updateProduct() {
        System.out.println("\n----------- UPDATE PRODUCT -----------");
        String id = ConsoleIO.readLine("Enter Product ID to update: ");
        Product p = productService.findById(id);
        if (p == null) {
            System.out.println("Failed to update product. Product not found.");
            return;
        }

        System.out.println("\nCurrent Information:");
        System.out.println("Name: " + p.getName());
        System.out.println("Category: " + p.getCategory());
        System.out.println("Size: " + p.getSize());
        System.out.println("Color: " + p.getColor());
        System.out.println("Price: " + ConsoleIO.formatMoney(p.getPrice()));
        System.out.println("Quantity: " + p.getQuantity());

        String newPriceStr = ConsoleIO.readOptionalLine("\nEnter new Price (leave blank to skip): ").trim();
        String newColor = ConsoleIO.readOptionalLine("Enter new Color (leave blank to skip): ").trim();
        String newQtyStr = ConsoleIO.readOptionalLine("Enter new Quantity (leave blank to skip): ").trim();

        int action = ConsoleIO.readInt("[1] Update  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        Long newPrice = newPriceStr.isEmpty() ? null : Long.parseLong(newPriceStr.replace(",", "").replace(".", ""));
        Integer newQty = newQtyStr.isEmpty() ? null : Integer.parseInt(newQtyStr);

        try {
            productService.update(id, null, null, null, newColor.isEmpty() ? null : newColor, newPrice, newQty);
            dataStore.saveProducts();
            System.out.println("Output:\nProduct updated successfully.");
        } catch (Exception e) {
            System.out.println("Failed to update product. " + e.getMessage());
        }
    }

    private void viewAllProducts() {
        printProductTable("---------------- PRODUCT LIST ----------------", productService.getAll());
        ConsoleIO.pressEnterToReturn();
    }

    private void searchProduct() {
        System.out.println("\n----------- SEARCH PRODUCT -----------");
        String keyword = ConsoleIO.readLine("Enter keyword: ");
        List<Product> results = productService.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No products found.");
        } else {
            printProductTable("Search results:", results);
        }
        ConsoleIO.pressEnterToReturn();
    }

    private void viewAvailableProducts() {
        printProductTable("---------------- AVAILABLE PRODUCTS ----------------", productService.getAvailableProducts());
        ConsoleIO.pressEnterToReturn();
    }

    private void removeProduct() {
        System.out.println("\n----------- REMOVE PRODUCT -----------");
        String id = ConsoleIO.readLine("Enter Product ID to remove: ");
        try {
            productService.remove(id);
            dataStore.saveProducts();
            System.out.println("Product removed successfully.");
        } catch (NotFoundException e) {
            System.out.println("Failed to remove product. " + e.getMessage());
        }
    }

    private void printProductTable(String title, Collection<Product> list) {
        System.out.println("\n" + title);
        System.out.printf("%-6s %-16s %-12s %-6s %-10s %-12s %-6s%n", "ID", "Name", "Category", "Size", "Color", "Price", "Stock");
        System.out.println("----------------------------------------------------------------------");
        for (Product p : list) {
            System.out.printf("%-6s %-16s %-12s %-6s %-10s %-12s %-6d%n",
                    p.getId(), p.getName(), p.getCategory(), p.getSize(), p.getColor(),
                    ConsoleIO.formatMoney(p.getPrice()), p.getQuantity());
        }
        System.out.println("----------------------------------------------------------------------");
    }
}
