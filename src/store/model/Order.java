package store.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sales transaction. Status: PENDING (being built) or COMPLETED.
 * BR10: a transaction must contain at least one product before it can be confirmed.
 * BR16: final amount = total amount - discount.
 */
public class Order {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";

    private final String id;           // immutable transaction ID
    private final String customerId;
    private final String date;         // dd/MM/yyyy
    private String status;
    private final List<OrderDetail> details = new ArrayList<>();

    public Order(String id, String customerId, String date) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.status = PENDING;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getDate() { return date; }
    public String getStatus() { return status; }

    public boolean isPending() { return PENDING.equals(status); }
    public boolean isCompleted() { return COMPLETED.equals(status); }

    public void markCompleted() { this.status = COMPLETED; }

    public List<OrderDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }

    public void addDetail(OrderDetail detail) {
        details.add(detail);
    }

    public OrderDetail findDetail(String productId) {
        for (OrderDetail d : details) {
            if (d.getProductId().equalsIgnoreCase(productId)) {
                return d;
            }
        }
        return null;
    }

    /** BR10: at least one product is required. */
    public boolean hasProducts() {
        return !details.isEmpty();
    }

    /** BR11: total amount = Sum of (Product Price x Quantity). */
    public long getSubtotal() {
        long sum = 0;
        for (OrderDetail d : details) {
            sum += d.getAmount();
        }
        return sum;
    }

    /** BR13/BR14/BR15: discount depends on customer membership type. */
    public long getDiscount(Customer customer) {
        return customer.calculateDiscount(getSubtotal());
    }

    /** BR16: final amount = total amount - discount. */
    public long getFinalAmount(Customer customer) {
        return getSubtotal() - getDiscount(customer);
    }
}
