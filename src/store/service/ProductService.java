package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InsufficientStockException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages products and inventory.
 * Collections used: Map (product catalog by ID), List (search results), Set (categories).
 */
public class ProductService {
    public static final int LOW_STOCK_THRESHOLD = 5;   // BR18

    private final Map<String, Product> products = new LinkedHashMap<>();

    public Collection<Product> getAll() {
        return Collections.unmodifiableCollection(products.values());
    }

    public Product findById(String id) {
        Product p = products.get(id);
        return p;
    }

    public boolean exists(String id) {
        return products.containsKey(id);
    }

    /** Set of distinct categories (demonstrates Set usage). */
    public Set<String> getCategories() {
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product p : products.values()) {
            categories.add(p.getCategory());
        }
        return categories;
    }

    public void add(Product p) throws DuplicateIdException, InvalidInputException {
        validate(p);
        if (exists(p.getId())) {   // BR1
            throw new DuplicateIdException("Product ID already exists: " + p.getId());
        }
        products.put(p.getId(), p);
    }

    public void remove(String id) throws NotFoundException {
        Product p = requireProduct(id);
        products.remove(p.getId());
    }

    /** BR17: validate before processing. */
    private void validate(Product p) throws InvalidInputException {
        if (p.getId() == null || p.getId().trim().isEmpty()) {
            throw new InvalidInputException("Product ID must not be empty.");
        }
        if (p.getName() == null || p.getName().trim().isEmpty()) {       // BR3
            throw new InvalidInputException("Product name must not be empty.");
        }
        if (p.getCategory() == null || p.getCategory().trim().isEmpty()) { // BR3
            throw new InvalidInputException("Category must not be empty.");
        }
        if (p.getSize() == null || p.getSize().trim().isEmpty()) {       // BR4
            throw new InvalidInputException("Size must not be empty.");
        }
        if (p.getColor() == null || p.getColor().trim().isEmpty()) {     // BR4
            throw new InvalidInputException("Color must not be empty.");
        }
        if (p.getPrice() <= 0) {                                         // BR5
            throw new InvalidInputException("Price must be greater than zero.");
        }
        if (p.getQuantity() < 0) {                                       // BR6
            throw new InvalidInputException("Quantity cannot be negative.");
        }
    }

    public Product requireProduct(String id) throws NotFoundException {
        Product p = products.get(id);
        if (p == null) {
            throw new NotFoundException("Product not found: " + id);     // BR7
        }
        return p;
    }

    /** Search by name, category, size, or color (case-insensitive). */
    public List<Product> search(String keyword) {
        List<Product> result = new ArrayList<>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Product p : products.values()) {
            if (p.getName().toLowerCase().contains(key)
                    || p.getCategory().toLowerCase().contains(key)
                    || p.getSize().toLowerCase().contains(key)
                    || p.getColor().toLowerCase().contains(key)) {
                result.add(p);
            }
        }
        return result;
    }

    /** Products currently available (stock > 0). */
    public List<Product> getAvailable() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isAvailable()) {
                result.add(p);
            }
        }
        return result;
    }

    /** BR18: products with quantity <= 5. */
    public List<Product> getLowStock() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isLowStock()) {
                result.add(p);
            }
        }
        return result;
    }

    /** BR12: stock is reduced immediately after a successful sale. */
    public void reduceStock(String productId, int qty)
            throws NotFoundException, InsufficientStockException {
        Product p = requireProduct(productId);
        if (qty <= 0) {   // BR8
            throw new InsufficientStockException("Quantity sold must be greater than zero.");
        }
        if (p.getQuantity() < qty) {   // BR9
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + p.getQuantity() + ", requested: " + qty);
        }
        p.setQuantity(p.getQuantity() - qty);
    }

    /** Restore stock when a pending order is cancelled/removed. */
    public void restoreStock(String productId, int qty) throws NotFoundException {
        Product p = requireProduct(productId);
        p.setQuantity(p.getQuantity() + qty);
    }

    /** Inventory management: update product stock (Task B12). */
    public void updateStock(String productId, int newQty)
            throws NotFoundException, InvalidInputException {
        Product p = requireProduct(productId);
        if (newQty < 0) {   // BR6
            throw new InvalidInputException("Quantity cannot be negative.");
        }
        p.setQuantity(newQty);
    }

    /** Used by File I/O layer to load saved products directly. */
    public void putRaw(Product p) {
        products.put(p.getId(), p);
    }
}
