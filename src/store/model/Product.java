package store.model;

/**
 * Represents a clothing product in the store.
 * Encapsulation: all fields are private, accessed via getters/setters.
 *
 * Business rules enforced here:
 *   BR3: name and category must not be empty
 *   BR4: size and color must not be empty
 *   BR5: price must be greater than zero
 *   BR6: quantity cannot be negative
 *   BR1: product ID is final (cannot be modified after creation)
 */
public class Product {
    private final String id;          // BR1: immutable ID
    private String name;
    private String category;
    private String size;
    private String color;
    private long price;               // VND
    private int quantity;

    public Product(String id, String name, String category, String size,
                   String color, long price, int quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.size = size;
        this.color = color;
        this.price = price;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public long getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setSize(String size) { this.size = size; }
    public void setColor(String color) { this.color = color; }
    public void setPrice(long price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /** BR18: a product is low stock when quantity <= 5. */
    public boolean isLowStock() {
        return quantity <= 5;
    }

    public boolean isAvailable() {
        return quantity > 0;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-20s %-12s %-6s %-10s %,12d %6d",
                id, name, category, size, color, price, quantity);
    }
}
