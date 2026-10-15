package store.model;

public class RegularCustomer extends Customer {

    public RegularCustomer(String id, String name, String phone, String address) {
        super(id, name, phone, address);
    }

    @Override
    public String getType() {
        return "Regular";
    }

    @Override
    public double getDiscountRate() {
        return 0.0;
    }

    @Override
    public long calculateDiscount(long subtotal) {
        return 0L;
    }
}
