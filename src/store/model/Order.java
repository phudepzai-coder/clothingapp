package store.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String id;
    private final String customerId;
    private final String date;
    private String status; // "PENDING" or "COMPLETED"
    private final List<OrderDetail> details = new ArrayList<>();

    public Order(String id, String customerId, String date, String status) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }

    public void addDetail(OrderDetail detail) {
        for (OrderDetail d : details) {
            if (d.getProductId().equalsIgnoreCase(detail.getProductId())) {
                d.addQuantity(detail.getQuantity());
                return;
            }
        }
        details.add(detail);
    }

    public long getSubtotal() {
        long sum = 0;
        for (OrderDetail d : details) {
            sum += d.getAmount();
        }
        return sum;
    }

    public long getDiscount(Customer customer) {
        if (customer == null) return 0;
        return customer.calculateDiscount(getSubtotal());
    }

    public long getFinalAmount(Customer customer) {
        return getSubtotal() - getDiscount(customer);
    }
}
