package store.ui;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Product;
import store.service.ProductService;

import java.util.List;

/** PRODUCT MANAGEMENT menu (Tasks B1 - B4). */
public class ProductMenu {

    private final ProductService productService;
    private final DataStore dataStore;

    public ProductMenu(ProductService productService, DataStore dataStore) {
        this.productService = productService;
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("---------- PRODUCT MANAGEMENT ----------");
            System.out.println("1. Add Product");
            System.out.println("2. Update Product");
            System.out.println("3. Remove Product");
            System.out.println("4. View All Products");
            System.out.println("5. Search Product");
            System.out.println("6. View Available Products");
            System.out.println("7. Back");
            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1: addProduct(); break;
                case 2: updateProduct(); break;
                case 3: removeProduct(); break;
                case 4: viewAll(productService.getAll(), "PRODUCT LIST"); break;
                case 5: searchProduct(); break;
                case 6: viewAll(productService.getAvailable(), "AVAILABLE PRODUCTS"); break;
                case 7: return;
                default: System.out.println("Invalid option. Please choose 1-7.");
            }
        }
    }

    /** Task B1 - Add Product. */
    private void addProduct() {
        System.out.println("---------- ADD CLOTHING PRODUCT ----------");
        String id = ConsoleIO.readRequired("Product ID: ");
        String name = ConsoleIO.readRequired("Product Name: ");
        String category = ConsoleIO.readRequired("Category: ");
        String size = ConsoleIO.readRequired("Size: ");
        String color = ConsoleIO.readRequired("Color: ");
        long price = ConsoleIO.readPrice("Price: ");
        int qty = ConsoleIO.readNonNegativeInt("Quantity: ");

        System.out.println("[1] Save   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            productService.add(new Product(id, name, category, size, color, price, qty));
            dataStore.saveAll();
            System.out.println("Product added successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to add product. Product ID already exists.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to add product. " + e.getMessage());
        }
    }

    /** Task B2 - Update Product (leave blank to skip a field). */
    private void updateProduct() {
        System.out.println("---------- UPDATE PRODUCT ----------");
        String id = ConsoleIO.readRequired("Enter Product ID to update: ");
        Product p = productService.findById(id);
        if (p == null) {
            System.out.println("Failed to update. Product not found.");
            return;
        }
        System.out.println("Current Information:");
        System.out.println("Name: " + p.getName());
        System.out.println("Category: " + p.getCategory());
        System.out.println("Size: " + p.getSize());
        System.out.println("Color: " + p.getColor());
        System.out.printf("Price: %,d%n", p.getPrice());
        System.out.println("Quantity: " + p.getQuantity());

        String name = ConsoleIO.readLine("Enter new Name (leave blank to skip): ");
        String category = ConsoleIO.readLine("Enter new Category (leave blank to skip): ");
        String size = ConsoleIO.readLine("Enter new Size (leave blank to skip): ");
        String color = ConsoleIO.readLine("Enter new Color (leave blank to skip): ");
        String priceStr = ConsoleIO.readLine("Enter new Price (leave blank to skip): ");
        String qtyStr = ConsoleIO.readLine("Enter new Quantity (leave blank to skip): ");

        System.out.println("[1] Update   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            if (!name.trim().isEmpty()) p.setName(name);
            if (!category.trim().isEmpty()) p.setCategory(category);
            if (!size.trim().isEmpty()) p.setSize(size);
            if (!color.trim().isEmpty()) p.setColor(color);
            if (!priceStr.trim().isEmpty()) {
                long price = Long.parseLong(priceStr.replace(",", "").replace(".", ""));
                if (price <= 0) throw new InvalidInputException("Price must be greater than zero.");
                p.setPrice(price);
            }
            if (!qtyStr.trim().isEmpty()) {
                int qty = Integer.parseInt(qtyStr);
                if (qty < 0) throw new InvalidInputException("Quantity cannot be negative.");
                p.setQuantity(qty);
            }
            dataStore.saveAll();
            System.out.println("Product updated successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Failed to update. Invalid number format.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to update. " + e.getMessage());
        }
    }

    /** Remove a product. */
    private void removeProduct() {
        String id = ConsoleIO.readRequired("Enter Product ID to remove: ");
        try {
            productService.remove(id);
            dataStore.saveAll();
            System.out.println("Product removed successfully.");
        } catch (NotFoundException e) {
            System.out.println("Failed to remove. Product not found.");
        }
    }

    /** Task B3 - View All Products. */
    private void viewAll(Iterable<Product> list, String title) {
        System.out.println("-------------- " + title + " --------------");
        System.out.printf("%-6s %-20s %-12s %-6s %-10s %12s %6s%n",
                "ID", "Name", "Category", "Size", "Color", "Price", "Stock");
        System.out.println("---------------------------------------------------------------");
        boolean any = false;
        for (Product p : list) {
            System.out.printf("%-6s %-20s %-12s %-6s %-10s %,12d %6d%n",
                    p.getId(), p.getName(), p.getCategory(), p.getSize(),
                    p.getColor(), p.getPrice(), p.getQuantity());
            any = true;
        }
        System.out.println("---------------------------------------------------------------");
        if (!any) {
            System.out.println("No products found.");
        }
        ConsoleIO.pause();
    }

    /** Task B4 - Search Product by name/category/size/color. */
    private void searchProduct() {
        System.out.println("---------- SEARCH PRODUCT ----------");
        String keyword = ConsoleIO.readRequired("Enter keyword: ");
        List<Product> results = productService.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No products found.");
            ConsoleIO.pause();
            return;
        }
        System.out.println("Search results:");
        viewAll(results, "SEARCH RESULTS");
    }
}
