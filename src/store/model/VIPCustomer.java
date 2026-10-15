package store.model;

public class VIPCustomer extends Customer {

    public VIPCustomer(String id, String name, String phone, String address) {
        super(id, name, phone, address);
    }

    @Override
    public String getType() {
        return "VIP";
    }

    @Override
    public double getDiscountRate() {
        return 0.10;
    }

    @Override
    public long calculateDiscount(long subtotal) {
        return Math.round(subtotal * 0.10);
    }
}
