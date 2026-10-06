package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Customer;
import store.model.RegularCustomer;
import store.model.VIPCustomer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages customers. Uses polymorphic Customer subclasses (Regular / VIP).
 */
public class CustomerService {

    private final Map<String, Customer> customers = new LinkedHashMap<>();

    public Collection<Customer> getAll() {
        return Collections.unmodifiableCollection(customers.values());
    }

    public Customer findById(String id) {
        return customers.get(id);
    }

    public Customer requireCustomer(String id) throws NotFoundException {
        Customer c = customers.get(id);
        if (c == null) {
            throw new NotFoundException("Customer not found: " + id);
        }
        return c;
    }

    /** Creates the correct subclass based on membership type (polymorphism). */
    public Customer create(String id, String type, String name, String phone, String address)
            throws DuplicateIdException, InvalidInputException {
        validate(id, name, phone);
        if (customers.containsKey(id)) {   // BR2
            throw new DuplicateIdException("Customer ID already exists: " + id);
        }
        Customer c;
        if ("VIP".equalsIgnoreCase(type)) {
            c = new VIPCustomer(id, name, phone, address);
        } else if ("Regular".equalsIgnoreCase(type)) {
            c = new RegularCustomer(id, name, phone, address);
        } else {
            throw new InvalidInputException("Membership type must be VIP or Regular.");
        }
        customers.put(c.getId(), c);
        return c;
    }

    public void remove(String id) throws NotFoundException {
        requireCustomer(id);
        customers.remove(id);
    }

    /** Search by name or phone number (case-insensitive). */
    public List<Customer> search(String keyword) {
        List<Customer> result = new ArrayList<>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Customer c : customers.values()) {
            if (c.getName().toLowerCase().contains(key)
                    || c.getPhone().toLowerCase().contains(key)) {
                result.add(c);
            }
        }
        return result;
    }

    /** BR17: validate user input before processing. */
    private void validate(String id, String name, String phone) throws InvalidInputException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("Customer ID must not be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Customer name must not be empty.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new InvalidInputException("Phone number must not be empty.");
        }
        if (!phone.trim().matches("\\d{8,12}")) {
            throw new InvalidInputException("Phone number must contain 8-12 digits.");
        }
    }

    /** Used by File I/O layer. */
    public void putRaw(Customer c) {
        customers.put(c.getId(), c);
    }
}
