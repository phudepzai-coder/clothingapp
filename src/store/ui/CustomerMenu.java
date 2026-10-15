package store.ui;

import store.exceptions.DuplicateIdException;
import store.exceptions.InvalidInputException;
import store.exceptions.NotFoundException;
import store.io.DataStore;
import store.model.Customer;
import store.service.CustomerService;

import java.util.Collection;
import java.util.List;

public class CustomerMenu {
    private final CustomerService customerService;
    private final DataStore dataStore;

    public CustomerMenu(CustomerService customerService, DataStore dataStore) {
        this.customerService = customerService;
        this.dataStore = dataStore;
    }

    public void display() {
        while (true) {
            System.out.println("\n---------------- CUSTOMER MANAGEMENT ----------------");
            System.out.println("1. Add Customer (Task B5)");
            System.out.println("2. Update Customer (Task B6)");
            System.out.println("3. View All Customers (Task B7)");
            System.out.println("4. Search Customers by Name / Phone");
            System.out.println("5. Remove Customer");
            System.out.println("6. Back to Main Menu");
            System.out.println("------------------------------------------------------");

            int choice = ConsoleIO.readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    addCustomer();
                    break;
                case 2:
                    updateCustomer();
                    break;
                case 3:
                    viewAllCustomers();
                    break;
                case 4:
                    searchCustomer();
                    break;
                case 5:
                    removeCustomer();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 6.");
            }
        }
    }

    private void addCustomer() {
        System.out.println("\n----------- ADD CUSTOMER -----------");
        String id = ConsoleIO.readLine("Customer ID: ");
        String name = ConsoleIO.readLine("Full Name: ");
        String phone = ConsoleIO.readLine("Phone: ");
        String address = ConsoleIO.readLine("Address: ");
        String type = ConsoleIO.readLine("Membership Type (Regular / VIP): ");

        int action = ConsoleIO.readInt("[1] Save  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            customerService.create(id, type, name, phone, address);
            dataStore.saveCustomers();
            System.out.println("Output:\nCustomer added successfully.");
        } catch (DuplicateIdException e) {
            System.out.println("Failed to add customer. Customer ID already exists.");
        } catch (InvalidInputException e) {
            System.out.println("Failed to add customer. " + e.getMessage());
        }
    }

    private void updateCustomer() {
        System.out.println("\n----------- UPDATE CUSTOMER -----------");
        String id = ConsoleIO.readLine("Enter Customer ID: ");
        Customer c = customerService.findById(id);
        if (c == null) {
            System.out.println("Failed to update customer. Customer not found.");
            return;
        }

        System.out.println("\nCurrent Information:");
        System.out.println("Name: " + c.getName());
        System.out.println("Phone: " + c.getPhone());
        System.out.println("Address: " + c.getAddress());
        System.out.println("Membership Type: " + c.getType());

        String newPhone = ConsoleIO.readOptionalLine("\nEnter new Phone (leave blank to skip): ").trim();
        String newAddress = ConsoleIO.readOptionalLine("Enter new Address (leave blank to skip): ").trim();

        int action = ConsoleIO.readInt("[1] Update  [2] Cancel\nChoose: ");
        if (action != 1) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            customerService.update(id, null, newPhone.isEmpty() ? null : newPhone, newAddress.isEmpty() ? null : newAddress);
            dataStore.saveCustomers();
            System.out.println("Output:\nCustomer updated successfully.");
        } catch (Exception e) {
            System.out.println("Failed to update customer. " + e.getMessage());
        }
    }

    private void viewAllCustomers() {
        printCustomerTable("---------------- CUSTOMER LIST ----------------", customerService.getAll());
        ConsoleIO.pressEnterToReturn();
    }

    private void searchCustomer() {
        System.out.println("\n----------- SEARCH CUSTOMER -----------");
        String keyword = ConsoleIO.readLine("Enter keyword (name or phone): ");
        List<Customer> results = customerService.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No customers found.");
        } else {
            printCustomerTable("Search results:", results);
        }
        ConsoleIO.pressEnterToReturn();
    }

    private void removeCustomer() {
        System.out.println("\n----------- REMOVE CUSTOMER -----------");
        String id = ConsoleIO.readLine("Enter Customer ID to remove: ");
        try {
            customerService.remove(id);
            dataStore.saveCustomers();
            System.out.println("Customer removed successfully.");
        } catch (NotFoundException e) {
            System.out.println("Failed to remove customer. " + e.getMessage());
        }
    }

    private void printCustomerTable(String title, Collection<Customer> list) {
        System.out.println("\n" + title);
        System.out.printf("%-6s %-20s %-15s %-10s%n", "ID", "Name", "Phone", "Type");
        System.out.println("------------------------------------------------");
        for (Customer c : list) {
            System.out.printf("%-6s %-20s %-15s %-10s%n",
                    c.getId(), c.getName(), c.getPhone(), c.getType());
        }
        System.out.println("------------------------------------------------");
    }
}
