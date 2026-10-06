package store.ui;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Customer;
import store.service.CustomerService;

import java.util.List;

/** CUSTOMER MANAGEMENT menu (Tasks B5 - B7). */
public class CustomerMenu {

    private final CustomerService customerService;
    private final DataStore dataStore;

    public CustomerMenu(CustomerService customerService, DataStore dataStore) {
        this.customerService = customerService;
        this.dataStore = dataStore;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("---------- CUSTOMER MANAGEMENT ----------");
            System.out.println("1. Add Customer");
            System.out.println("2. Update Customer");
            System.out.println("3. Remove Customer");
            System.out.println("4. View All Customers");
            System.out.println("5. Search Customer");
            System.out.println("6. Back");
            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1: addCustomer(); break;
                case 2: updateCustomer(); break;
                case 3: removeCustomer(); break;
                case 4: viewAll(customerService.getAll(), "CUSTOMER LIST"); break;
                case 5: searchCustomer(); break;
                case 6: return;
                default: System.out.println("Invalid option. Please choose 1-6.");
            }
        }
    }

    /** Task B5 - Add Customer. */
    private void addCustomer() {
        System.out.println("---------- ADD CUSTOMER ----------");
        String id = ConsoleIO.readRequired("Customer ID: ");
        String name = ConsoleIO.readRequired("Full Name: ");
        String phone = ConsoleIO.readRequired("Phone: ");
        String address = ConsoleIO.readRequired("Address: ");
        String type;
        while (true) {
            type = ConsoleIO.readRequired("Membership Type (VIP/Regular): ");
            if (type.equalsIgnoreCase("VIP") || type.equalsIgnoreCase("Regular")) break;
            System.out.println("Membership type must be VIP or Regular.");
        }

        System.out.println("[1] Save   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            customerService.create(id, type, name, phone, address);
            dataStore.saveAll();
            System.out.println("Customer added successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to add customer. Customer ID already exists.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to add customer. " + e.getMessage());
        }
    }

    /** Task B6 - Update Customer (leave blank to skip a field). */
    private void updateCustomer() {
        System.out.println("---------- UPDATE CUSTOMER ----------");
        String id = ConsoleIO.readRequired("Enter Customer ID: ");
        Customer c = customerService.findById(id);
        if (c == null) {
            System.out.println("Failed to update. Customer not found.");
            return;
        }
        System.out.println("Current Information:");
        System.out.println("Name: " + c.getName());
        System.out.println("Phone: " + c.getPhone());
        System.out.println("Address: " + c.getAddress());
        System.out.println("Membership Type: " + c.getType());

        String name = ConsoleIO.readLine("Enter new Name (leave blank to skip): ");
        String phone = ConsoleIO.readLine("Enter new Phone (leave blank to skip): ");
        String address = ConsoleIO.readLine("Enter new Address (leave blank to skip): ");

        System.out.println("[1] Update   [2] Cancel");
        int action = ConsoleIO.readInt("Choose: ");
        if (action != 1) {
            System.out.println("Operation cancelled.");
            return;
        }
        try {
            if (!name.trim().isEmpty()) c.setName(name);
            if (!phone.trim().isEmpty()) {
                if (!phone.matches("\\d{8,12}")) {
                    throw new InvalidInputException("Phone number must contain 8-12 digits.");
                }
                c.setPhone(phone);
            }
            if (!address.trim().isEmpty()) c.setAddress(address);
            dataStore.saveAll();
            System.out.println("Customer updated successfully.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to update. " + e.getMessage());
        }
    }

    /** Remove a customer. */
    private void removeCustomer() {
        String id = ConsoleIO.readRequired("Enter Customer ID to remove: ");
        try {
            customerService.remove(id);
            dataStore.saveAll();
            System.out.println("Customer removed successfully.");
        } catch (NotFoundException e) {
            System.out.println("Failed to remove. Customer not found.");
        }
    }

    /** Task B7 - View All Customers. */
    private void viewAll(Iterable<Customer> list, String title) {
        System.out.println("-------------- " + title + " --------------");
        System.out.printf("%-6s %-22s %-14s %-10s%n", "ID", "Name", "Phone", "Type");
        System.out.println("--------------------------------------------------");
        boolean any = false;
        for (Customer c : list) {
            System.out.printf("%-6s %-22s %-14s %-10s%n",
                    c.getId(), c.getName(), c.getPhone(), c.getType());
            any = true;
        }
        System.out.println("--------------------------------------------------");
        if (!any) {
            System.out.println("No customers found.");
        }
        ConsoleIO.pause();
    }

    /** Search by name or phone number. */
    private void searchCustomer() {
        System.out.println("---------- SEARCH CUSTOMER ----------");
        String keyword = ConsoleIO.readRequired("Enter keyword: ");
        List<Customer> results = customerService.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No customers found.");
            ConsoleIO.pause();
            return;
        }
        System.out.println("Search results:");
        viewAll(results, "SEARCH RESULTS");
    }
}
