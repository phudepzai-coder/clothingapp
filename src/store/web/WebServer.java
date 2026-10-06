package store.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import store.exceptions.DuplicateIdException;
import store.exceptions.InsufficientStockException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;
import store.service.CustomerService;
import store.service.ProductService;
import store.service.ReportService;
import store.service.SalesService;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Small built-in web server (JDK's com.sun.net.httpserver, no external libraries).
 * Serves the HTML UI from the ./web folder and a JSON REST API backed by the
 * same services used by the console menu, so both UIs share one source of truth.
 */
public class WebServer {

    private final ProductService productService;
    private final CustomerService customerService;
    private final SalesService salesService;
    private final ReportService reportService;
    private final DataStore dataStore;
    private final Path webDir;
    private final HttpServer server;

    public WebServer(int preferredPort, ProductService productService,
                     CustomerService customerService, SalesService salesService,
                     ReportService reportService, DataStore dataStore, Path webDir)
            throws IOException {
        this.productService = productService;
        this.customerService = customerService;
        this.salesService = salesService;
        this.reportService = reportService;
        this.dataStore = dataStore;
        this.webDir = webDir;

        IOException lastError = null;
        HttpServer created = null;
        int chosenPort = preferredPort;
        for (int p = preferredPort; p < preferredPort + 20 && created == null; p++) {
            try {
                created = HttpServer.create(new InetSocketAddress(p), 0);
                chosenPort = p;
            } catch (IOException e) {
                lastError = e;
            }
        }
        if (created == null) {
            throw lastError != null ? lastError : new IOException("No free port found");
        }
        this.server = created;
        this.port = chosenPort;

        server.createContext("/", this::handleStatic);
        server.createContext("/api", this::handleApi);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    private final int port;

    public void start() {
        server.start();
    }

    public int getPort() {
        return port;
    }

    /** Opens the default browser on the UI page. */
    public void openBrowser() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/"));
            }
        } catch (Exception ignored) {
            // user can open the URL manually
        }
    }

    // ================================================================ API

    private void handleApi(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        Map<String, String> form = readBody(ex);
        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
        try {
            routeApi(ex, path, method, form, query);
        } catch (Exception e) {
            int status = 500;
            if (e instanceof NotFoundException) status = 404;
            else if (e instanceof DuplicateIdException || e instanceof InvalidInputException
                    || e instanceof InsufficientStockException) status = 400;
            sendJson(ex, status, "{\"error\":\"" + Json.esc(String.valueOf(e.getMessage())) + "\"}");
        }
    }

    private void routeApi(HttpExchange ex, String path, String method, Map<String, String> form,
                          Map<String, String> query) throws IOException, DuplicateIdException,
                          InsufficientStockException, InvalidInputException, NotFoundException {
        String[] seg = path.split("/");
        // seg[0]="" seg[1]="api" seg[2]="products" ...
        String resource = seg.length > 2 ? seg[2] : "";
        String id = seg.length > 3 ? seg[3] : null;
        String action = seg.length > 4 ? seg[4] : null;

        switch (resource) {
            case "products":
                if ("GET".equals(method) && id == null) {
                    sendJson(ex, 200, productsJson(productService.getAll()));
                } else if ("POST".equals(method) && id == null) {
                    productService.add(new Product(
                            form.get("id"), form.get("name"), form.get("category"),
                            form.get("size"), form.get("color"),
                            parseLong(form.get("price")), parseInt(form.get("quantity"))));
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Product added successfully.\"}");
                } else if ("PUT".equals(method) && id != null) {
                    updateProduct(id, form);
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Product updated successfully.\"}");
                } else if ("DELETE".equals(method) && id != null) {
                    productService.remove(id);
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Product removed successfully.\"}");
                } else if ("POST".equals(method) && id != null && "stock".equals(action)) {
                    productService.updateStock(id, parseInt(form.get("quantity")));
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Product stock updated successfully.\"}");
                } else {
                    sendJson(ex, 405, "{\"error\":\"Method not allowed.\"}");
                }
                break;

            case "customers":
                if ("GET".equals(method) && id == null) {
                    sendJson(ex, 200, customersJson(customerService.getAll()));
                } else if ("POST".equals(method) && id == null) {
                    customerService.create(form.get("id"), form.get("type"),
                            form.get("name"), form.get("phone"), form.get("address"));
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Customer added successfully.\"}");
                } else if ("PUT".equals(method) && id != null) {
                    Customer c = customerService.requireCustomer(id);
                    if (notBlank(form.get("name"))) c.setName(form.get("name").trim());
                    if (notBlank(form.get("phone"))) {
                        String phone = form.get("phone").trim();
                        if (!phone.matches("\\d{8,12}")) {
                            throw new InvalidInputException("Phone number must contain 8-12 digits.");
                        }
                        c.setPhone(phone);
                    }
                    if (notBlank(form.get("address"))) c.setAddress(form.get("address").trim());
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Customer updated successfully.\"}");
                } else if ("DELETE".equals(method) && id != null) {
                    customerService.remove(id);
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Customer removed successfully.\"}");
                } else {
                    sendJson(ex, 405, "{\"error\":\"Method not allowed.\"}");
                }
                break;

            case "orders":
                if ("GET".equals(method) && id == null) {
                    sendJson(ex, 200, ordersJson(salesService.getAll()));
                } else if ("GET".equals(method) && id != null) {
                    sendJson(ex, 200, orderJson(salesService.requireOrder(id)));
                } else if ("POST".equals(method) && id == null) {
                    salesService.createOrder(form.get("id"), form.get("customerId"), form.get("date"));
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Transaction created successfully.\"}");
                } else if ("POST".equals(method) && id != null && "items".equals(action)) {
                    salesService.addProductToOrder(id, form.get("productId"), parseInt(form.get("quantity")));
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Product added to transaction successfully.\"}");
                } else if ("POST".equals(method) && id != null && "confirm".equals(action)) {
                    salesService.confirmSale(id);
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Sale completed successfully.\"}");
                } else if ("DELETE".equals(method) && id != null) {
                    salesService.cancelPendingOrder(id);
                    dataStore.saveAll();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Transaction cancelled.\"}");
                } else {
                    sendJson(ex, 405, "{\"error\":\"Method not allowed.\"}");
                }
                break;

            case "reports":
                if ("GET".equals(method) && "monthly".equals(id)) {
                    int month = Integer.parseInt(query.getOrDefault("month", "1"));
                    int year = Integer.parseInt(query.getOrDefault("year", "2025"));
                    ReportService.MonthlyReport r = reportService.monthlyReport(month, year);
                    sendJson(ex, 200, "{\"month\":" + month + ",\"year\":" + year
                            + ",\"transactions\":" + r.transactions
                            + ",\"productsSold\":" + r.productsSold
                            + ",\"revenue\":" + r.revenue + "}");
                } else if ("GET".equals(method) && "best".equals(id)) {
                    sendJson(ex, 200, bestSellingJson(reportService.bestSellingProducts(
                            parseInt(query.getOrDefault("n", "10")))));
                } else if ("GET".equals(method) && "topcustomers".equals(id)) {
                    sendJson(ex, 200, topCustomersJson(reportService.highestSpendingCustomers(
                            parseInt(query.getOrDefault("n", "10")))));
                } else if ("GET".equals(method) && "lowstock".equals(id)) {
                    sendJson(ex, 200, productsJson(reportService.lowStockReport()));
                } else {
                    sendJson(ex, 405, "{\"error\":\"Method not allowed.\"}");
                }
                break;

            default:
                sendJson(ex, 404, "{\"error\":\"Unknown API endpoint.\"}");
        }
    }

    private void updateProduct(String id, Map<String, String> form)
            throws NotFoundException, InvalidInputException {
        Product p = productService.requireProduct(id);
        if (notBlank(form.get("name"))) p.setName(form.get("name").trim());
        if (notBlank(form.get("category"))) p.setCategory(form.get("category").trim());
        if (notBlank(form.get("size"))) p.setSize(form.get("size").trim());
        if (notBlank(form.get("color"))) p.setColor(form.get("color").trim());
        if (notBlank(form.get("price"))) {
            long price = parseLong(form.get("price"));
            if (price <= 0) throw new InvalidInputException("Price must be greater than zero.");
            p.setPrice(price);
        }
        if (notBlank(form.get("quantity"))) {
            int qty = parseInt(form.get("quantity"));
            if (qty < 0) throw new InvalidInputException("Quantity cannot be negative.");
            p.setQuantity(qty);
        }
    }

    // ============================================================ JSON

    private String productsJson(Iterable<Product> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Product p : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(Json.esc(p.getId()))
              .append("\",\"name\":\"").append(Json.esc(p.getName()))
              .append("\",\"category\":\"").append(Json.esc(p.getCategory()))
              .append("\",\"size\":\"").append(Json.esc(p.getSize()))
              .append("\",\"color\":\"").append(Json.esc(p.getColor()))
              .append("\",\"price\":").append(p.getPrice())
              .append(",\"quantity\":").append(p.getQuantity())
              .append(",\"lowStock\":").append(p.isLowStock())
              .append(",\"available\":").append(p.isAvailable())
              .append("}");
        }
        return sb.append("]").toString();
    }

    private String customersJson(Iterable<Customer> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Customer c : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(Json.esc(c.getId()))
              .append("\",\"name\":\"").append(Json.esc(c.getName()))
              .append("\",\"phone\":\"").append(Json.esc(c.getPhone()))
              .append("\",\"address\":\"").append(Json.esc(c.getAddress()))
              .append("\",\"type\":\"").append(Json.esc(c.getType()))
              .append("\",\"discountRate\":").append(c.getDiscountRate())
              .append("}");
        }
        return sb.append("]").toString();
    }

    private String ordersJson(Iterable<Order> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Order o : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(orderJson(o));
        }
        return sb.append("]").toString();
    }

    private String orderJson(Order o) {
        Customer c = customerService.findById(o.getCustomerId());
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":\"").append(Json.esc(o.getId()))
          .append("\",\"customerId\":\"").append(Json.esc(o.getCustomerId()))
          .append("\",\"customerName\":\"").append(Json.esc(c != null ? c.getName() : ""))
          .append("\",\"membership\":\"").append(Json.esc(c != null ? c.getType() : ""))
          .append("\",\"date\":\"").append(Json.esc(o.getDate()))
          .append("\",\"status\":\"").append(Json.esc(o.getStatus()))
          .append("\",\"subtotal\":").append(o.getSubtotal())
          .append(",\"discount\":").append(c != null ? o.getDiscount(c) : 0)
          .append(",\"total\":").append(c != null ? o.getFinalAmount(c) : o.getSubtotal())
          .append(",\"details\":[");
        boolean first = true;
        for (OrderDetail d : o.getDetails()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"productId\":\"").append(Json.esc(d.getProductId()))
              .append("\",\"productName\":\"").append(Json.esc(d.getProductName()))
              .append("\",\"unitPrice\":").append(d.getUnitPrice())
              .append(",\"quantity\":").append(d.getQuantity())
              .append(",\"amount\":").append(d.getAmount())
              .append("}");
        }
        return sb.append("]}").toString();
    }

    private String bestSellingJson(List<Object[]> rows) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object[] r : rows) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"productId\":\"").append(Json.esc(String.valueOf(r[0])))
              .append("\",\"productName\":\"").append(Json.esc(String.valueOf(r[1])))
              .append("\",\"quantitySold\":").append(((Number) r[2]).longValue())
              .append(",\"revenue\":").append(((Number) r[3]).longValue())
              .append("}");
        }
        return sb.append("]").toString();
    }

    private String topCustomersJson(List<Object[]> rows) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object[] r : rows) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"customerId\":\"").append(Json.esc(String.valueOf(r[0])))
              .append("\",\"customerName\":\"").append(Json.esc(String.valueOf(r[1])))
              .append("\",\"totalPurchase\":").append(((Number) r[2]).longValue())
              .append(",\"type\":\"").append(Json.esc(String.valueOf(r[3])))
              .append("\"}");
        }
        return sb.append("]").toString();
    }

    // ======================================================== helpers

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s == null ? "" : s.replace(",", "").replace(".", "").trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number: " + s);
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number: " + s);
        }
    }

    private Map<String, String> readBody(HttpExchange ex) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        java.io.InputStream in = ex.getRequestBody();
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return parseQuery(new String(buf.toByteArray(), StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            try {
                map.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (Exception e) {
                map.put(k, v);
            }
        }
        return map;
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ==================================================== static files

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = webDir.resolve(path.substring(1)).normalize();
        Path base = webDir.toAbsolutePath().normalize();
        if (!file.toAbsolutePath().normalize().startsWith(base) || !Files.exists(file)) {
            byte[] notFound = "404 - Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(notFound);
            }
            return;
        }
        String name = file.getFileName().toString().toLowerCase();
        String type = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".js") ? "application/javascript; charset=utf-8"
                : name.endsWith(".png") ? "image/png"
                : name.endsWith(".svg") ? "image/svg+xml"
                : "application/octet-stream";
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Minimal JSON string escaper. */
    static final class Json {
        private Json() {
        }

        static String esc(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
