package store.model;

/** BR15: VIP customers receive a 10% discount. */
public class VIPCustomer extends Customer {

    public static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(String id, String name, String phone, String address) {
        super(id, name, phone, address);
    }

    @Override
    public String getType() {
        return "VIP";
    }

    @Override
    public long calculateDiscount(long subtotal) {
        return subtotal * DISCOUNT_PERCENT / 100;   // BR15
    }

    @Override
    public int getDiscountRate() {
        return DISCOUNT_PERCENT;
    }
}
