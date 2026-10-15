package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InsufficientStockException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Customer;
import store.model.Order;
import store.model.OrderDetail;
import store.model.Product;

import java.util.*;

public class SalesService {
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final ProductService productService;
    private final CustomerService customerService;

    public SalesService(ProductService productService, CustomerService customerService) {
        this.productService = productService;
        this.customerService = customerService;
    }

    public Collection<Order> getAll() {
        return orders.values();
    }

    public Order findById(String id) {
        if (id == null) return null;
        return orders.get(id.trim().toUpperCase());
    }

    public Order requireOrder(String id) throws NotFoundException {
        Order o = findById(id);
        if (o == null) {
            throw new NotFoundException("Order not found: " + id);
        }
        return o;
    }

    public Order createOrder(String id, String customerId, String date)
            throws DuplicateIdException, InvalidInputException, NotFoundException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required.");
        }
        String key = id.trim().toUpperCase();
        if (orders.containsKey(key)) {
            throw new DuplicateIdException("Transaction ID already exists.");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidInputException("Customer ID is required.");
        }
        customerService.requireCustomer(customerId);

        if (date == null || date.trim().isEmpty()) {
            throw new InvalidInputException("Date is required.");
        }

        Order order = new Order(key, customerId.trim().toUpperCase(), date.trim(), "PENDING");
        orders.put(key, order);
        return order;
    }

    public void addOrder(Order order) {
        orders.put(order.getId().trim().toUpperCase(), order);
    }

    public void addProductToOrder(String orderId, String productId, int quantity)
            throws NotFoundException, InvalidInputException, InsufficientStockException {
        Order o = requireOrder(orderId);
        if (!"PENDING".equalsIgnoreCase(o.getStatus())) {
            throw new InvalidInputException("Cannot add products to a completed or cancelled transaction.");
        }
        Product p = productService.requireProduct(productId);
        if (quantity <= 0) {
            throw new InvalidInputException("The quantity sold must be greater than zero.");
        }
        if (quantity > p.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for " + p.getName() + " (available: " + p.getQuantity() + ").");
        }

        OrderDetail detail = new OrderDetail(p.getId(), p.getName(), p.getPrice(), quantity);
        o.addDetail(detail);
    }

    public void confirmSale(String orderId)
            throws NotFoundException, InvalidInputException, InsufficientStockException {
        Order o = requireOrder(orderId);
        if (!"PENDING".equalsIgnoreCase(o.getStatus())) {
            throw new InvalidInputException("Transaction is already completed.");
        }
        if (o.getDetails().isEmpty()) {
            throw new InvalidInputException("A sales transaction must contain at least one product.");
        }

        // Check all stock availability before committing
        for (OrderDetail d : o.getDetails()) {
            Product p = productService.requireProduct(d.getProductId());
            if (d.getQuantity() > p.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for " + p.getName() + " (available: " + p.getQuantity() + ").");
            }
        }

        // Deduct inventory
        for (OrderDetail d : o.getDetails()) {
            Product p = productService.requireProduct(d.getProductId());
            p.setQuantity(p.getQuantity() - d.getQuantity());
        }

        o.setStatus("COMPLETED");
    }

    public void cancelPendingOrder(String orderId) throws NotFoundException, InvalidInputException {
        Order o = requireOrder(orderId);
        if (!"PENDING".equalsIgnoreCase(o.getStatus())) {
            throw new InvalidInputException("Only pending transactions can be cancelled.");
        }
        orders.remove(orderId.trim().toUpperCase());
    }
}
