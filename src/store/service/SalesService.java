package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InsufficientStockException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages sales transactions.
 * BR7:  product must exist before being added.
 * BR8:  quantity sold must be > 0.
 * BR9:  quantity sold cannot exceed available stock.
 * BR10: a transaction must contain at least one product.
 * BR11: total = sum(price x quantity).
 * BR12: stock reduced immediately after a successful sale.
 * BR16: final amount = total - discount.
 * BR21: revenue is calculated from COMPLETED transactions only.
 */
public class SalesService {

    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final ProductService productService;
    private final CustomerService customerService;

    public SalesService(ProductService productService, CustomerService customerService) {
        this.productService = productService;
        this.customerService = customerService;
    }

    public Collection<Order> getAll() {
        return Collections.unmodifiableCollection(orders.values());
    }

    public Order findById(String id) {
        return orders.get(id);
    }

    public Order requireOrder(String id) throws NotFoundException {
        Order o = orders.get(id);
        if (o == null) {
            throw new NotFoundException("Transaction not found: " + id);
        }
        return o;
    }

    /** Task B8 - Create a new (pending) sales transaction. */
    public Order createOrder(String id, String customerId, String date)
            throws DuplicateIdException, NotFoundException, InvalidInputException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("Transaction ID must not be empty.");
        }
        if (orders.containsKey(id)) {
            throw new DuplicateIdException("Transaction ID already exists: " + id);
        }
        Customer c = customerService.requireCustomer(customerId);
        validateDate(date);
        Order order = new Order(id.trim(), c.getId(), date.trim());
        orders.put(order.getId(), order);
        return order;
    }

    /** Task B9 - Add a product to a pending transaction (with stock check). */
    public void addProductToOrder(String orderId, String productId, int qty)
            throws NotFoundException, InsufficientStockException, InvalidInputException {
        Order order = requireOrder(orderId);
        if (!order.isPending()) {
            throw new InvalidInputException("Transaction is already completed and cannot be modified.");
        }
        Product p = productService.requireProduct(productId);   // BR7
        if (qty <= 0) {                                          // BR8
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        if (p.getQuantity() < qty) {                             // BR9
            throw new InsufficientStockException("Insufficient stock. Available: " + p.getQuantity());
        }
        OrderDetail existing = order.findDetail(productId);
        if (existing != null) {
            int newQty = existing.getQuantity() + qty;
            if (p.getQuantity() < newQty) {                      // BR9 for the whole line
                throw new InsufficientStockException(
                        "Insufficient stock. Available: " + p.getQuantity());
            }
            existing.setQuantity(newQty);
        } else {
            order.addDetail(new OrderDetail(p.getId(), p.getName(), p.getPrice(), qty));
        }
    }

    /** Task B10 / confirm sale - completes the transaction and updates inventory. */
    public void confirmSale(String orderId)
            throws NotFoundException, InvalidInputException, InsufficientStockException {
        Order order = requireOrder(orderId);
        if (!order.isPending()) {
            throw new InvalidInputException("Transaction is already completed.");
        }
        if (!order.hasProducts()) {                              // BR10
            throw new InvalidInputException("A sales transaction must contain at least one product.");
        }
        // Reduce stock for every line (BR12). Throws if stock changed meanwhile.
        for (OrderDetail d : order.getDetails()) {
            productService.reduceStock(d.getProductId(), d.getQuantity());
        }
        order.markCompleted();
    }

    /** Cancel a pending transaction and return reserved stock (nothing was deducted yet). */
    public void cancelPendingOrder(String orderId) throws NotFoundException {
        Order order = requireOrder(orderId);
        if (order.isPending()) {
            orders.remove(order.getId());
        }
    }

    /** Transactions filtered by status. */
    public List<Order> getByStatus(String status) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders.values()) {
            if (o.getStatus().equalsIgnoreCase(status)) {
                result.add(o);
            }
        }
        return result;
    }

    /** BR21: only COMPLETED transactions count for revenue. */
    public List<Order> getCompletedInMonth(int month, int year) {
        List<Order> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Order o : orders.values()) {
            if (!o.isCompleted()) {
                continue;
            }
            try {
                LocalDate d = LocalDate.parse(o.getDate(), fmt);
                if (d.getMonthValue() == month && d.getYear() == year) {
                    result.add(o);
                }
            } catch (DateTimeParseException ignored) {
                // skip malformed dates
            }
        }
        return result;
    }

    /** Used by File I/O layer. */
    public void putRaw(Order o) {
        orders.put(o.getId(), o);
    }

    private void validateDate(String date) throws InvalidInputException {
        if (date == null || date.trim().isEmpty()) {
            throw new InvalidInputException("Date must not be empty.");
        }
        try {
            LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Date must be in format dd/MM/yyyy, e.g. 15/12/2025.");
        }
    }
}
