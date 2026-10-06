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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * File I/O layer (plain-text, pipe-separated format).
 *   products.txt  : id|name|category|size|color|price|quantity
 *   customers.txt : id|type|name|phone|address
 *   orders.txt    : id|customerId|date|status  followed by detail lines: >productId|name|price|qty
 */
public class DataStore {

    private final String folder;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SalesService salesService;

    public DataStore(String folder, ProductService productService,
                     CustomerService customerService, SalesService salesService) {
        this.folder = folder;
        this.productService = productService;
        this.customerService = customerService;
        this.salesService = salesService;
    }

    // ---------------------------------------------------------------- load

    public void loadAll() {
        loadProducts();
        loadCustomers();
        loadOrders();
    }

    private void loadProducts() {
        File f = file("products.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;
                try {
                    productService.putRaw(new Product(parts[0], parts[1], parts[2], parts[3],
                            parts[4], Long.parseLong(parts[5]), Integer.parseInt(parts[6])));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            System.out.println("[Warn] Could not load products.txt: " + e.getMessage());
        }
    }

    private void loadCustomers() {
        File f = file("customers.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                if ("VIP".equalsIgnoreCase(parts[1])) {
                    customerService.putRaw(new VIPCustomer(parts[0], parts[2], parts[3], parts[4]));
                } else {
                    customerService.putRaw(new RegularCustomer(parts[0], parts[2], parts[3], parts[4]));
                }
            }
        } catch (IOException e) {
            System.out.println("[Warn] Could not load customers.txt: " + e.getMessage());
        }
    }

    private void loadOrders() {
        File f = file("orders.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            Order current = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (line.startsWith(">")) {          // order detail line
                    if (current == null) continue;
                    String[] parts = line.substring(1).split("\\|", -1);
                    if (parts.length < 4) continue;
                    try {
                        current.addDetail(new OrderDetail(parts[0], parts[1],
                                Long.parseLong(parts[2]), Integer.parseInt(parts[3])));
                    } catch (NumberFormatException ignored) {
                    }
                } else {                              // order header line
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 4) continue;
                    Order o = new Order(parts[0], parts[1], parts[2]);
                    if ("COMPLETED".equalsIgnoreCase(parts[3])) {
                        o.markCompleted();
                    }
                    salesService.putRaw(o);
                    current = o;
                }
            }
        } catch (IOException e) {
            System.out.println("[Warn] Could not load orders.txt: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- save

    public void saveAll() {
        saveProducts();
        saveCustomers();
        saveOrders();
    }

    private void saveProducts() {
        List<String> lines = new ArrayList<>();
        for (Product p : productService.getAll()) {
            lines.add(String.join("|", p.getId(), p.getName(), p.getCategory(),
                    p.getSize(), p.getColor(), String.valueOf(p.getPrice()),
                    String.valueOf(p.getQuantity())));
        }
        writeFile("products.txt", lines);
    }

    private void saveCustomers() {
        List<String> lines = new ArrayList<>();
        for (Customer c : customerService.getAll()) {
            lines.add(String.join("|", c.getId(), c.getType(), c.getName(),
                    c.getPhone(), c.getAddress()));
        }
        writeFile("customers.txt", lines);
    }

    private void saveOrders() {
        List<String> lines = new ArrayList<>();
        for (Order o : salesService.getAll()) {
            lines.add(String.join("|", o.getId(), o.getCustomerId(), o.getDate(), o.getStatus()));
            for (OrderDetail d : o.getDetails()) {
                lines.add(">" + String.join("|", d.getProductId(), d.getProductName(),
                        String.valueOf(d.getUnitPrice()), String.valueOf(d.getQuantity())));
            }
        }
        writeFile("orders.txt", lines);
    }

    private void writeFile(String name, List<String> lines) {
        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file(name)))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("[Warn] Could not save " + name + ": " + e.getMessage());
        }
    }

    private File file(String name) {
        return new File(folder, name);
    }
}
