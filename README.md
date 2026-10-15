# 👕 Clothing Store Management System

A comprehensive Clothing Sales Management System developed in accordance with the project requirements. The project features both:
1. **Core Java OOP Application (Console UI)** — strictly adhering to Object-Oriented Programming (OOP) principles, custom exceptions, file persistence (`.txt`), and menu-driven workflow (Tasks B1 – B16).
2. **Interactive Web Application (Web UI)** — a modern, responsive Single-Page Application (SPA) powered by **Firebase Realtime Database**, ready for one-click deployment via **GitHub Pages**.

---

## 🌟 Highlights & Features

### 1. Product Management
- **Add Product (Task B1):** Validates unique ID, non-empty fields, positive price, non-negative quantity.
- **Update Product (Task B2):** Allows updating price, color, quantity (supports skipping fields).
- **View All Products (Task B3):** Displays products in a formatted table.
- **Search Products (Task B4):** Searches by name, category, size, color, or ID.
- **View Available Products:** Filter products with stock > 0.
- **Remove Product:** Deletes products with existence validation.

### 2. Customer Management
- **Add Customer (Task B5):** Manages ID, full name, phone (8–12 digits), address, and membership type (`Regular` vs. `VIP`).
- **Update Customer (Task B6):** Updates phone and address.
- **View All Customers (Task B7):** Formatted customer table.
- **Search Customers:** Instant lookup by customer name or phone number.
- **Remove Customer:** Deletes customer records.

### 3. Sales Management
- **Create Transaction (Task B8):** Creates pending sales transaction associated with customer and date.
- **Add Products to Transaction (Task B9):** Verifies product existence, ensures quantity sold > 0 and <= available stock.
- **Bill Summary & Confirmation (Task B10):**
  - Polymorphic membership discounts (`VIP`: 10% discount, `Regular`: 0%).
  - Automatically calculates Subtotal, Discount, and Final Amount.
  - Immediately reduces product inventory upon sale confirmation.
- **Transaction History:** View full purchase history and detailed bills.

### 4. Inventory Management
- **Low Stock Report (Task B11):** Automatically identifies products where quantity <= 5.
- **Update Stock (Task B12):** Quick stock replenishment for inventory control.

### 5. Reports & Analytics
- **Monthly Sales Report (Task B13):** Total transactions, units sold, and net revenue for a chosen month/year.
- **Daily Sales Report:** Realtime reporting for any specific day.
- **Best-Selling Products (Task B14):** Ranked by total quantity sold.
- **Highest-Spending Customers (Task B15):** Ranked by total expenditure.
- **Low Stock Report (Task B16):** Quick overview of items needing restock.

### 6. Authentication & Role-Based Access Control (Web UI)
- **Admin Role:** Full administrative access to manage products, customers, stock inventory, order processing, and business analytics.
- **Customer Role:** Registration for new accounts, login with phone number, online shopping, real-time discount calculation (10% off for VIPs), and private order history tracking.
- **Demo Accounts:**
  - 👑 **Admin:** Username: `admin` | Password: `123`
  - 💎 **VIP Customer:** Username: `0901234567` | Password: `123`
  - 👤 **Regular Customer:** Username: `0987654321` | Password: `123`

---

## 📐 OOP Architecture & Design Patterns

- **Encapsulation:** All fields in `Product`, `Customer`, `Order`, and `OrderDetail` are private, accessed via getters/setters.
- **Inheritance:** `RegularCustomer` and `VIPCustomer` extend the base abstract class `Customer`.
- **Polymorphism:** `calculateDiscount(long subtotal)` and `getDiscountRate()` are overridden dynamically based on customer type.
- **Collections:** Extensively utilizes `List`, `Map` (`LinkedHashMap`, `HashMap`), and `Set`.
- **Exception Handling:** Custom business exceptions:
  - `DuplicateIdException`
  - `NotFoundException`
  - `InsufficientStockException`
  - `InvalidInputException`
- **File I/O Persistence:** Data is loaded upon startup and persisted to `data/products.txt`, `data/customers.txt`, and `data/orders.txt`.

---

## 📂 Project Structure

```
ClothingStoreApp/
├── src/store/
│   ├── Main.java              # Java Application Entry Point
│   ├── model/                 # Product, Customer, RegularCustomer, VIPCustomer, Order, OrderDetail
│   ├── exceptions/            # Custom checked exceptions
│   ├── service/               # ProductService, CustomerService, SalesService, ReportService
│   ├── io/                    # DataStore (File I/O .txt reader/writer)
│   └── ui/                    # Console menus (MainMenu, ProductMenu, CustomerMenu, etc.)
│
├── data/                      # Persistent storage (.txt files)
│   ├── products.txt
│   ├── customers.txt
│   └── orders.txt
│
├── js/                        # Modular Web UI logic
│   ├── firebase.js            # Firebase Realtime Database connector
│   ├── api.js                 # API request layer & validation
│   ├── ui.js                  # Shared UI components (modal, toast, nav)
│   ├── products.js            # Product management with search & filter
│   ├── customers.js           # Customer management with search
│   ├── sales.js               # Sales order creation & billing
│   ├── inventory.js           # Inventory stock tracking
│   ├── reports.js             # Daily & Monthly reports
│   └── main.js                # App entry point
│
├── index.html                 # Web UI Single Page Application
├── style.css                  # Modern responsive styling
└── README.md
```

---

## 🚀 How to Run

### 1. Running the Java Console Application
Open terminal in the project directory:
```bash
# Compile
javac -encoding UTF-8 -d out src/store/*.java src/store/*/*.java

# Run
java -cp out store.Main
```

### 2. Running the Web Application
Simply double-click `index.html` to open it in any web browser, or host it on **GitHub Pages**:
1. Push this repository to GitHub.
2. Go to **Settings > Pages**.
3. Under **Branch**, select `main` (root) and click **Save**.
4. Access your live website at `https://<your-username>.github.io/<repo-name>/`.
