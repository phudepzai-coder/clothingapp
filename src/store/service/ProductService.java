package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Product;

import java.util.*;

public class ProductService {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public Collection<Product> getAll() {
        return products.values();
    }

    public Product findById(String id) {
        if (id == null) return null;
        return products.get(id.trim().toUpperCase());
    }

    public Product requireProduct(String id) throws NotFoundException {
        Product p = findById(id);
        if (p == null) {
            throw new NotFoundException("Product not found: " + id);
        }
        return p;
    }

    public void add(Product product) throws DuplicateIdException, InvalidInputException {
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            throw new InvalidInputException("Product ID is required.");
        }
        String key = product.getId().trim().toUpperCase();
        if (products.containsKey(key)) {
            throw new DuplicateIdException("Product ID already exists.");
        }
        validateProduct(product);
        products.put(key, product);
    }

    public void update(String id, String name, String category, String size, String color, Long price, Integer quantity)
            throws NotFoundException, InvalidInputException {
        Product p = requireProduct(id);
        if (name != null && !name.trim().isEmpty()) p.setName(name.trim());
        if (category != null && !category.trim().isEmpty()) p.setCategory(category.trim());
        if (size != null && !size.trim().isEmpty()) p.setSize(size.trim());
        if (color != null && !color.trim().isEmpty()) p.setColor(color.trim());
        if (price != null) {
            if (price <= 0) throw new InvalidInputException("Price must be greater than zero.");
            p.setPrice(price);
        }
        if (quantity != null) {
            if (quantity < 0) throw new InvalidInputException("Quantity cannot be negative.");
            p.setQuantity(quantity);
        }
    }

    public void updateStock(String id, int newQuantity) throws NotFoundException, InvalidInputException {
        Product p = requireProduct(id);
        if (newQuantity < 0) throw new InvalidInputException("Quantity cannot be negative.");
        p.setQuantity(newQuantity);
    }

    public void remove(String id) throws NotFoundException {
        String key = id.trim().toUpperCase();
        if (!products.containsKey(key)) {
            throw new NotFoundException("Product not found: " + id);
        }
        products.remove(key);
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(products.values());
        }
        String kw = keyword.trim().toLowerCase();
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.getName().toLowerCase().contains(kw)
                    || p.getCategory().toLowerCase().contains(kw)
                    || p.getSize().toLowerCase().equalsIgnoreCase(kw)
                    || p.getColor().toLowerCase().contains(kw)
                    || p.getId().toLowerCase().contains(kw)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> getAvailableProducts() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isAvailable()) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> getLowStockProducts() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isLowStock()) {
                result.add(p);
            }
        }
        return result;
    }

    private void validateProduct(Product p) throws InvalidInputException {
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new InvalidInputException("Product name is required.");
        }
        if (p.getCategory() == null || p.getCategory().trim().isEmpty()) {
            throw new InvalidInputException("Product category is required.");
        }
        if (p.getSize() == null || p.getSize().trim().isEmpty()) {
            throw new InvalidInputException("Product size is required.");
        }
        if (p.getColor() == null || p.getColor().trim().isEmpty()) {
            throw new InvalidInputException("Product color is required.");
        }
        if (p.getPrice() <= 0) {
            throw new InvalidInputException("Price must be greater than zero.");
        }
        if (p.getQuantity() < 0) {
            throw new InvalidInputException("Quantity cannot be negative.");
        }
    }
}
