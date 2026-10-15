package store.io;

import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;
import store.model.RegularCustomer;
import store.model.VIPCustomer;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.SalesService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataStore {
    private final Path dataDir;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SalesService salesService;

    public DataStore(Path dataDir, ProductService productService, CustomerService customerService, SalesService salesService) {
        this.dataDir = dataDir;
        this.productService = productService;
        this.customerService = customerService;
        this.salesService = salesService;
    }

    public void init() {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            loadProducts();
            loadCustomers();
            loadOrders();
        } catch (Exception e) {
            System.err.println("Note: Initializing data files - " + e.getMessage());
        }
    }

    public void loadProducts() {
        Path file = dataDir.resolve("products.txt");
        if (!Files.exists(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 7) {
                    Product p = new Product(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(),
                            parts[3].trim(), parts[4].trim(),
                            Long.parseLong(parts[5].trim()),
                            Integer.parseInt(parts[6].trim())
                    );
                    productService.add(p);
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadCustomers() {
        Path file = dataDir.resolve("customers.txt");
        if (!Files.exists(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String id = parts[0].trim();
                    String type = parts[1].trim();
                    String name = parts[2].trim();
                    String phone = parts[3].trim();
                    String address = parts[4].trim();
                    Customer c;
                    if ("VIP".equalsIgnoreCase(type)) {
                        c = new VIPCustomer(id, name, phone, address);
                    } else {
                        c = new RegularCustomer(id, name, phone, address);
                    }
                    customerService.add(c);
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadOrders() {
        Path file = dataDir.resolve("orders.txt");
        if (!Files.exists(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            Order currentOrder = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if ("ORDER".equalsIgnoreCase(parts[0].trim()) && parts.length >= 5) {
                    currentOrder = new Order(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim());
                    salesService.addOrder(currentOrder);
                } else if ("ITEM".equalsIgnoreCase(parts[0].trim()) && currentOrder != null && parts.length >= 5) {
                    OrderDetail d = new OrderDetail(
                            parts[1].trim(), parts[2].trim(),
                            Long.parseLong(parts[3].trim()),
                            Integer.parseInt(parts[4].trim())
                    );
                    currentOrder.addDetail(d);
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void saveAll() {
        saveProducts();
        saveCustomers();
        saveOrders();
    }

    public void saveProducts() {
        Path file = dataDir.resolve("products.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Product p : productService.getAll()) {
                writer.write(String.format("%s|%s|%s|%s|%s|%d|%d%n",
                        p.getId(), p.getName(), p.getCategory(), p.getSize(), p.getColor(), p.getPrice(), p.getQuantity()));
            }
        } catch (IOException e) {
            System.err.println("Error saving products: " + e.getMessage());
        }
    }

    public void saveCustomers() {
        Path file = dataDir.resolve("customers.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Customer c : customerService.getAll()) {
                writer.write(String.format("%s|%s|%s|%s|%s%n",
                        c.getId(), c.getType(), c.getName(), c.getPhone(), c.getAddress()));
            }
        } catch (IOException e) {
            System.err.println("Error saving customers: " + e.getMessage());
        }
    }

    public void saveOrders() {
        Path file = dataDir.resolve("orders.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Order o : salesService.getAll()) {
                writer.write(String.format("ORDER|%s|%s|%s|%s%n",
                        o.getId(), o.getCustomerId(), o.getDate(), o.getStatus()));
                for (OrderDetail d : o.getDetails()) {
                    writer.write(String.format("ITEM|%s|%s|%d|%d%n",
                            d.getProductId(), d.getProductName(), d.getUnitPrice(), d.getQuantity()));
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving orders: " + e.getMessage());
        }
    }
}
