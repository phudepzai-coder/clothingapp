package store.model;

/**
 * Abstract customer - base class of the inheritance hierarchy.
 * Polymorphism: subclasses override calculateDiscount() according to customer type.
 */
public abstract class Customer {
    protected final String id;   // BR2: immutable ID
    protected String name;
    protected String phone;
    protected String address;

    protected Customer(String id, String name, String phone, String address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }

    /** Membership type label, e.g. "VIP" or "Regular". */
    public abstract String getType();

    /** Discount amount (VND) computed from the subtotal. BR13. */
    public abstract long calculateDiscount(long subtotal);

    /** Discount rate in percent (0, 10, ...). */
    public abstract int getDiscountRate();
}
