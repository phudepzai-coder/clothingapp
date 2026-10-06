package store.model;

/** BR14: Regular customers receive no discount. */
public class RegularCustomer extends Customer {

    public RegularCustomer(String id, String name, String phone, String address) {
        super(id, name, phone, address);
    }

    @Override
    public String getType() {
        return "Regular";
    }

    @Override
    public long calculateDiscount(long subtotal) {
        return 0;   // BR14: no discount
    }

    @Override
    public int getDiscountRate() {
        return 0;
    }
}
