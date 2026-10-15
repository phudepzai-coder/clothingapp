package store.service;

import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;

import java.util.*;

public class ReportService {
    private final ProductService productService;
    private final CustomerService customerService;
    private final SalesService salesService;

    public ReportService(ProductService productService, CustomerService customerService, SalesService salesService) {
        this.productService = productService;
        this.customerService = customerService;
        this.salesService = salesService;
    }

    public static class ReportSummary {
        public int transactions = 0;
        public int productsSold = 0;
        public long revenue = 0L;
    }

    public ReportSummary monthlyReport(int month, int year) {
        ReportSummary r = new ReportSummary();
        for (Order o : salesService.getAll()) {
            if (!"COMPLETED".equalsIgnoreCase(o.getStatus())) continue;
            String date = o.getDate();
            if (date == null) continue;
            String[] parts = date.split("/");
            if (parts.length == 3) {
                try {
                    int m = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    if (m == month && y == year) {
                        r.transactions++;
                        for (OrderDetail d : o.getDetails()) {
                            r.productsSold += d.getQuantity();
                        }
                        Customer c = customerService.findById(o.getCustomerId());
                        r.revenue += o.getFinalAmount(c);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return r;
    }

    public ReportSummary dailyReport(int day, int month, int year) {
        ReportSummary r = new ReportSummary();
        for (Order o : salesService.getAll()) {
            if (!"COMPLETED".equalsIgnoreCase(o.getStatus())) continue;
            String date = o.getDate();
            if (date == null) continue;
            String[] parts = date.split("/");
            if (parts.length == 3) {
                try {
                    int d = Integer.parseInt(parts[0].trim());
                    int m = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    if (d == day && m == month && y == year) {
                        r.transactions++;
                        for (OrderDetail dt : o.getDetails()) {
                            r.productsSold += dt.getQuantity();
                        }
                        Customer c = customerService.findById(o.getCustomerId());
                        r.revenue += o.getFinalAmount(c);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return r;
    }

    public static class BestSellingItem {
        public String productId;
        public String productName;
        public int quantitySold;
        public long revenue;

        public BestSellingItem(String productId, String productName, int quantitySold, long revenue) {
            this.productId = productId;
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }
    }

    public List<BestSellingItem> bestSellingProducts(int limit) {
        Map<String, BestSellingItem> map = new HashMap<>();
        for (Order o : salesService.getAll()) {
            if (!"COMPLETED".equalsIgnoreCase(o.getStatus())) continue;
            for (OrderDetail d : o.getDetails()) {
                BestSellingItem item = map.get(d.getProductId());
                if (item == null) {
                    item = new BestSellingItem(d.getProductId(), d.getProductName(), 0, 0L);
                    map.put(d.getProductId(), item);
                }
                item.quantitySold += d.getQuantity();
                item.revenue += d.getAmount();
            }
        }
        List<BestSellingItem> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Integer.compare(b.quantitySold, a.quantitySold));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    public static class TopCustomerItem {
        public String customerId;
        public String customerName;
        public String type;
        public long totalPurchase;

        public TopCustomerItem(String customerId, String customerName, String type, long totalPurchase) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.type = type;
            this.totalPurchase = totalPurchase;
        }
    }

    public List<TopCustomerItem> highestSpendingCustomers(int limit) {
        Map<String, Long> spendingMap = new HashMap<>();
        for (Order o : salesService.getAll()) {
            if (!"COMPLETED".equalsIgnoreCase(o.getStatus())) continue;
            Customer c = customerService.findById(o.getCustomerId());
            long finalAmt = o.getFinalAmount(c);
            spendingMap.put(o.getCustomerId(), spendingMap.getOrDefault(o.getCustomerId(), 0L) + finalAmt);
        }

        List<TopCustomerItem> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : spendingMap.entrySet()) {
            Customer c = customerService.findById(entry.getKey());
            String name = (c != null) ? c.getName() : "Unknown";
            String type = (c != null) ? c.getType() : "Regular";
            list.add(new TopCustomerItem(entry.getKey(), name, type, entry.getValue()));
        }
        list.sort((a, b) -> Long.compare(b.totalPurchase, a.totalPurchase));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    public List<Product> lowStockReport() {
        return productService.getLowStockProducts();
    }
}
