package store.service;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.model.Customer;
import store.model.RegularCustomer;
import store.model.VIPCustomer;

import java.util.*;

public class CustomerService {
    private final Map<String, Customer> customers = new LinkedHashMap<>();

    public Collection<Customer> getAll() {
        return customers.values();
    }

    public Customer findById(String id) {
        if (id == null) return null;
        return customers.get(id.trim().toUpperCase());
    }

    public Customer requireCustomer(String id) throws NotFoundException {
        Customer c = findById(id);
        if (c == null) {
            throw new NotFoundException("Customer not found: " + id);
        }
        return c;
    }

    public Customer create(String id, String type, String name, String phone, String address)
            throws DuplicateIdException, InvalidInputException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("Customer ID is required.");
        }
        String key = id.trim().toUpperCase();
        if (customers.containsKey(key)) {
            throw new DuplicateIdException("Customer ID already exists.");
        }
        validateCustomerInput(name, phone, address);

        Customer c;
        if ("VIP".equalsIgnoreCase(type != null ? type.trim() : "")) {
            c = new VIPCustomer(key, name.trim(), phone.trim(), address.trim());
        } else {
            c = new RegularCustomer(key, name.trim(), phone.trim(), address.trim());
        }
        customers.put(key, c);
        return c;
    }

    public void add(Customer customer) throws DuplicateIdException {
        String key = customer.getId().trim().toUpperCase();
        if (customers.containsKey(key)) {
            throw new DuplicateIdException("Customer ID already exists.");
        }
        customers.put(key, customer);
    }

    public void update(String id, String name, String phone, String address)
            throws NotFoundException, InvalidInputException {
        Customer c = requireCustomer(id);
        if (name != null && !name.trim().isEmpty()) c.setName(name.trim());
        if (phone != null && !phone.trim().isEmpty()) {
            String p = phone.trim();
            if (!p.matches("\\d{8,12}")) {
                throw new InvalidInputException("Phone number must contain 8-12 digits.");
            }
            c.setPhone(p);
        }
        if (address != null && !address.trim().isEmpty()) c.setAddress(address.trim());
    }

    public void remove(String id) throws NotFoundException {
        String key = id.trim().toUpperCase();
        if (!customers.containsKey(key)) {
            throw new NotFoundException("Customer not found: " + id);
        }
        customers.remove(key);
    }

    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(customers.values());
        }
        String kw = keyword.trim().toLowerCase();
        List<Customer> result = new ArrayList<>();
        for (Customer c : customers.values()) {
            if (c.getName().toLowerCase().contains(kw)
                    || c.getPhone().contains(kw)
                    || c.getId().toLowerCase().contains(kw)) {
                result.add(c);
            }
        }
        return result;
    }

    private void validateCustomerInput(String name, String phone, String address) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Customer name is required.");
        }
        if (phone == null || !phone.trim().matches("\\d{8,12}")) {
            throw new InvalidInputException("Phone number must contain 8-12 digits.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new InvalidInputException("Address is required.");
        }
    }
}
