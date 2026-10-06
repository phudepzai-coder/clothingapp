# Clothing Store Management App (Java)

Clothing sales management system with **two interfaces**: a console menu and a modern
web UI (HTML/CSS/JS served by a built-in Java web server — no external libraries).

## How to compile & run

### Web UI (recommended)

```bash
cd ClothingStoreApp
javac -encoding UTF-8 -d out src\store\*.java src\store\model\*.java src\store\exceptions\*.java src\store\service\*.java src\store\io\*.java src\store\ui\*.java src\store\web\*.java
java -cp out store.Main web
```

Then open **http://localhost:8080** (the `web` mode opens the browser automatically).
Or simply double-click `run.bat` on Windows.

### Web UI without a server (no localhost needed)

You can also open `web/index.html` directly (double-click it). The app detects that
there is no server and automatically switches to **Local mode**: all actions run in
the browser and data is stored in the browser's `localStorage` (seeded with the same
sample data as `data/*.txt`). If the Java server is running, the app uses the real
REST API and the `data/` files instead — no configuration needed either way.

### Console UI

```bash
java -cp out store.Main
```

## Web UI

- `web/index.html`, `web/style.css`, `web/app.js` — single-page app (Vietnamese UI)
  with tabs for Products, Customers, Sales, Inventory and Reports, modal forms,
  and Success/Fail toast messages.
- `store/web/WebServer.java` — built-in `com.sun.net.httpserver` serving static files
  and a JSON REST API (`/api/products`, `/api/customers`, `/api/orders`, `/api/reports/...`)
  backed by the same services as the console app, so both UIs share one data store.

## Features

- **Product Management** — add, update (skip blank fields), remove, view all, search by
  name/category/size/color, view available products.
- **Customer Management** — add, update, remove, view all, search by name/phone.
- **Sales Management** — create transaction, add products with stock check, bill summary
  with membership discount, confirm sale, view details & history.
- **Inventory Management** — low stock list (qty <= 5), update stock; stock is reduced
  automatically after a completed sale.
- **Reports** — monthly sales report, best-selling products, highest-spending customers,
  low stock report, total revenue per month.

## Business rules implemented

| Rule | Meaning |
|------|---------|
| BR1/BR2 | Product/Customer IDs unique and immutable |
| BR3-BR6 | Name/category/size/color not empty, price > 0, quantity >= 0 |
| BR7-BR9 | Product must exist, qty sold > 0, qty <= stock |
| BR10-BR12 | Transaction needs >= 1 product, total = sum(price x qty), stock reduced after sale |
| BR13-BR16 | VIP = 10% discount, Regular = 0%, final = total - discount |
| BR17 | All input validated before processing |
| BR18 | Low stock when quantity <= 5 |
| BR19-BR21 | Best sellers by qty sold, top spenders by purchase value, revenue from completed sales only |

## OOP principles

- **Encapsulation** — `Product`, `Customer`, `Order`, `OrderDetail` with private fields.
- **Inheritance** — `Customer` ← `RegularCustomer`, `VIPCustomer`.
- **Polymorphism** — overridden `calculateDiscount()` per customer type.
- **Collections** — `List`, `Map`, `Set` used throughout services.
- **Exception handling** — custom exceptions: `DuplicateIdException`, `NotFoundException`,
  `InsufficientStockException`, `InvalidInputException`.
- **File I/O** — data persisted to `data/products.txt`, `data/customers.txt`, `data/orders.txt`
  (pipe-separated). Loaded on start, saved after every change and on exit.

## Project structure

```
src/store/
├── Main.java              # entry point
├── model/                 # Product, Customer, RegularCustomer, VIPCustomer, Order, OrderDetail
├── exceptions/            # custom exceptions
├── service/               # ProductService, CustomerService, SalesService, ReportService
├── io/                    # DataStore (file persistence)
└── ui/                    # console menus (MainMenu + 5 sub-menus, ConsoleIO helper)
```
