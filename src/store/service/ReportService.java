package store.service;

import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporting tasks (Task B13 - B16).
 * BR19: best-selling products are based on total quantity sold in a period.
 * BR20: highest-spending customers are based on total purchase value in a period.
 * BR21: revenue is calculated from completed sales transactions only.
 */
public class ReportService {

    private final SalesService salesService;
    private final ProductService productService;
    private final CustomerService customerService;

    public ReportService(SalesService salesService, ProductService productService,
                         CustomerService customerService) {
        this.salesService = salesService;
        this.productService = productService;
        this.customerService = customerService;
    }

    /** Row of the monthly report. */
    public static class MonthlyReport {
        public final int transactions;
        public final long productsSold;
        public final long revenue;

        public MonthlyReport(int transactions, long productsSold, long revenue) {
            this.transactions = transactions;
            this.productsSold = productsSold;
            this.revenue = revenue;
        }
    }

    /** Task B13 - monthly sales report (completed transactions only, BR21). */
    public MonthlyReport monthlyReport(int month, int year) {
        List<Order> orders = salesService.getCompletedInMonth(month, year);
        int transactions = orders.size();
        long productsSold = 0;
        long revenue = 0;
        for (Order o : orders) {
            Customer c = customerService.findById(o.getCustomerId());
            revenue += o.getFinalAmount(c);
            for (OrderDetail d : o.getDetails()) {
                productsSold += d.getQuantity();
            }
        }
        return new MonthlyReport(transactions, productsSold, revenue);
    }

    /** Task B14 - best-selling products by total quantity sold (BR19). */
    public List<Object[]> bestSellingProducts(int topN) {
        Map<String, long[]> totals = new LinkedHashMap<>();  // id -> [qtySold, revenue]
        for (Order o : salesService.getByStatus(Order.COMPLETED)) {
            for (OrderDetail d : o.getDetails()) {
                long[] t = totals.computeIfAbsent(d.getProductId(), k -> new long[2]);
                t[0] += d.getQuantity();
                t[1] += d.getAmount();
            }
        }
        List<Map.Entry<String, long[]>> entries = new ArrayList<>(totals.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));

        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            Map.Entry<String, long[]> e = entries.get(i);
            Product p = productService.findById(e.getKey());
            String name = p != null ? p.getName() : "(deleted)";
            result.add(new Object[]{e.getKey(), name, e.getValue()[0], e.getValue()[1]});
        }
        return result;
    }

    /** Task B15 - highest-spending customers by total purchase value (BR20). */
    public List<Object[]> highestSpendingCustomers(int topN) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Order o : salesService.getByStatus(Order.COMPLETED)) {
            Customer c = customerService.findById(o.getCustomerId());
            if (c == null) {
                continue;
            }
            totals.merge(c.getId(), o.getFinalAmount(c), Long::sum);
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(totals.entrySet());
        entries.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()));

        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            Map.Entry<String, Long> e = entries.get(i);
            Customer c = customerService.findById(e.getKey());
            result.add(new Object[]{e.getKey(), c.getName(), e.getValue(), c.getType()});
        }
        return result;
    }

    /** Task B16 - low stock report (BR18). */
    public List<Product> lowStockReport() {
        return productService.getLowStock();
    }
}
