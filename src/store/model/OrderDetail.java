package store.model;

public class OrderDetail {
    private final String productId;
    private final String productName;
    private final long unitPrice;
    private int quantity;

    public OrderDetail(String productId, String productName, long unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int extra) {
        this.quantity += extra;
    }

    public long getAmount() {
        return unitPrice * quantity;
    }
}
