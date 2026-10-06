/* Clothing Sales Management System - Web UI logic */

const $main = document.getElementById("main");
const API = "/api";

let products = [];
let customers = [];

/* ---------------- request layer (server or local) ----------------
 * Khi mở trang qua server Java (localhost) thì dùng REST API như cũ.
 * Khi mở file index.html trực tiếp (file://) hoặc server không phản hồi,
 * app tự chuyển sang chế độ LOCAL: mọi thao tác chạy bằng JavaScript và
 * dữ liệu được lưu trong localStorage của trình duyệt — không cần server.
 */
let useLocal = location.protocol === "file:";
let localNoticeShown = useLocal;

async function request(method, path, params) {
  if (useLocal) return localApi(method, path, params || {});
  const opts = {};
  if (method !== "GET") {
    opts.method = method;
    opts.headers = { "Content-Type": "application/x-www-form-urlencoded" };
    opts.body = new URLSearchParams(params || {});
  }
  try {
    const res = await fetch(API + path, opts);
    let data = {};
    let isJson = true;
    try { data = await res.json(); } catch (e) { isJson = false; /* phản hồi không phải JSON */ }
    // Nếu trả về HTML (VD: trang 404 của GitHub Pages) hoặc HTTP lỗi,
    // coi như không có server -> chuyển sang chế độ local
    if (!res.ok || !isJson) {
      if (method === "GET") {
        useLocal = true;
        showLocalNotice();
        return localApi(method, path, params || {});
      }
      throw new Error("Lỗi HTTP " + res.status);
    }
    return data;
  } catch (e) {
    // Lỗi mạng (không có server) -> chuyển sang chế độ local
    if (e instanceof TypeError) {
      useLocal = true;
      showLocalNotice();
      return localApi(method, path, params || {});
    }
    throw e;
  }
}

function showLocalNotice() {
  if (localNoticeShown) return;
  localNoticeShown = true;
  toast("Chế độ Local: không cần server, dữ liệu lưu trên trình duyệt này");
}

function api(path) { return request("GET", path); }
function post(path, params) { return request("POST", path, params); }
function put(path, params) { return request("PUT", path, params); }
function del(path) { return request("DELETE", path); }

function money(v) { return Number(v).toLocaleString("vi-VN") + " VND"; }

function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function toast(msg, ok = true) {
  const el = document.createElement("div");
  el.className = "toast-msg " + (ok ? "ok" : "err");
  el.textContent = msg;
  document.getElementById("toast").appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

/* ================= LOCAL BACKEND (không cần server) ================= */

const LS_KEY = "clothingStoreData_v1";
let memDB = null;

function loadDB() {
  if (memDB) return memDB;
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) { memDB = JSON.parse(raw); return memDB; }
  } catch (e) { /* localStorage không khả dụng -> dùng bộ nhớ tạm */ }
  memDB = {
    products: [
      { id: "P1", name: "White Shirt v2", category: "Shirt", size: "M", color: "White", price: 260000, quantity: 50 },
    ],
    customers: [
      { id: "C1", type: "VIP", name: "Nguyen Van A", phone: "0901234567", address: "Hanoi" },
      { id: "C2", type: "Regular", name: "Tran B New", phone: "0911222333", address: "Saigon" },
    ],
    orders: [
      { id: "O1", customerId: "C1", date: "15/10/2026", status: "COMPLETED",
        details: [{ productId: "P1", productName: "White Shirt", unitPrice: 250000, quantity: 3 }] },
    ],
  };
  saveDB();
  return memDB;
}

function saveDB() {
  try { localStorage.setItem(LS_KEY, JSON.stringify(memDB)); } catch (e) { /* ignore */ }
}

function req(v, msg) { if (v == null || String(v).trim() === "") throw new Error(msg); return String(v).trim(); }
function toNum(v, msg) { const n = Number(v); if (v === "" || v == null || isNaN(n)) throw new Error(msg); return n; }

function orderView(db, o) {
  const c = db.customers.find((x) => x.id === o.customerId) || null;
  const subtotal = o.details.reduce((s, d) => s + d.unitPrice * d.quantity, 0);
  const discount = c && c.type === "VIP" ? Math.round(subtotal * 0.10) : 0;
  return {
    id: o.id, customerId: o.customerId,
    customerName: c ? c.name : "",
    membership: c ? c.type : "",
    date: o.date, status: o.status,
    subtotal, discount, total: subtotal - discount,
    details: o.details,
  };
}

function localApi(method, path, params) {
  const db = loadDB();
  // tách query string (VD: /reports/monthly?month=10&year=2026)
  const qIndex = path.indexOf("?");
  if (qIndex >= 0) {
    const raw = path.slice(qIndex + 1);
    path = path.slice(0, qIndex);
    for (const pair of raw.split("&")) {
      if (!pair) continue;
      const eq = pair.indexOf("=");
      const k = eq < 0 ? pair : pair.slice(0, eq);
      const v = eq < 0 ? "" : decodeURIComponent(pair.slice(eq + 1));
      if (!(k in params)) params[k] = v;
    }
  }
  const seg = path.replace(/^\/+|\/+$/g, "").split("/"); // "products/P1/stock" ...
  const resource = seg[0] || "";
  const id = seg[1] || null;
  const action = seg[2] || null;

  /* ---------- products ---------- */
  if (resource === "products") {
    if (method === "GET" && !id) {
      return db.products.map((p) => ({
        ...p, lowStock: p.quantity <= 5, available: p.quantity > 0,
      }));
    }
    if (method === "POST" && !id) {
      const pid = req(params.id, "Product ID is required.");
      if (db.products.some((p) => p.id === pid)) throw new Error("Product ID already exists.");
      const price = toNum(params.price, "Price must be a number.");
      if (price <= 0) throw new Error("Price must be greater than zero.");
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty < 0) throw new Error("Quantity cannot be negative.");
      db.products.push({
        id: pid,
        name: req(params.name, "Product name is required."),
        category: req(params.category, "Category is required."),
        size: req(params.size, "Size is required."),
        color: req(params.color, "Color is required."),
        price, quantity: qty,
      });
      saveDB();
      return { ok: true, message: "Product added successfully." };
    }
    if (method === "PUT" && id) {
      const p = db.products.find((x) => x.id === id);
      if (!p) throw new Error("Product not found: " + id);
      if (params.name && params.name.trim()) p.name = params.name.trim();
      if (params.category && params.category.trim()) p.category = params.category.trim();
      if (params.size && params.size.trim()) p.size = params.size.trim();
      if (params.color && params.color.trim()) p.color = params.color.trim();
      if (params.price && String(params.price).trim() !== "") {
        const price = toNum(params.price, "Price must be a number.");
        if (price <= 0) throw new Error("Price must be greater than zero.");
        p.price = price;
      }
      if (params.quantity && String(params.quantity).trim() !== "") {
        const qty = toNum(params.quantity, "Quantity must be a number.");
        if (qty < 0) throw new Error("Quantity cannot be negative.");
        p.quantity = qty;
      }
      saveDB();
      return { ok: true, message: "Product updated successfully." };
    }
    if (method === "DELETE" && id) {
      const i = db.products.findIndex((x) => x.id === id);
      if (i < 0) throw new Error("Product not found: " + id);
      db.products.splice(i, 1);
      saveDB();
      return { ok: true, message: "Product removed successfully." };
    }
    if (method === "POST" && id && action === "stock") {
      const p = db.products.find((x) => x.id === id);
      if (!p) throw new Error("Product not found: " + id);
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty < 0) throw new Error("Quantity cannot be negative.");
      p.quantity = qty;
      saveDB();
      return { ok: true, message: "Product stock updated successfully." };
    }
  }

  /* ---------- customers ---------- */
  if (resource === "customers") {
    if (method === "GET" && !id) {
      return db.customers.map((c) => ({
        ...c, discountRate: c.type === "VIP" ? 0.10 : 0,
      }));
    }
    if (method === "POST" && !id) {
      const cid = req(params.id, "Customer ID is required.");
      if (db.customers.some((c) => c.id === cid)) throw new Error("Customer ID already exists.");
      const type = params.type === "VIP" ? "VIP" : "Regular";
      const phone = req(params.phone, "Phone number is required.");
      if (!phone.match(/^\d{8,12}$/)) throw new Error("Phone number must contain 8-12 digits.");
      db.customers.push({
        id: cid, type,
        name: req(params.name, "Customer name is required."),
        phone,
        address: req(params.address, "Address is required."),
      });
      saveDB();
      return { ok: true, message: "Customer added successfully." };
    }
    if (method === "PUT" && id) {
      const c = db.customers.find((x) => x.id === id);
      if (!c) throw new Error("Customer not found: " + id);
      if (params.name && params.name.trim()) c.name = params.name.trim();
      if (params.phone && params.phone.trim()) {
        const phone = params.phone.trim();
        if (!phone.match(/^\d{8,12}$/)) throw new Error("Phone number must contain 8-12 digits.");
        c.phone = phone;
      }
      if (params.address && params.address.trim()) c.address = params.address.trim();
      saveDB();
      return { ok: true, message: "Customer updated successfully." };
    }
    if (method === "DELETE" && id) {
      const i = db.customers.findIndex((x) => x.id === id);
      if (i < 0) throw new Error("Customer not found: " + id);
      db.customers.splice(i, 1);
      saveDB();
      return { ok: true, message: "Customer removed successfully." };
    }
  }

  /* ---------- orders ---------- */
  if (resource === "orders") {
    if (method === "GET" && !id) return db.orders.map((o) => orderView(db, o));
    if (method === "GET" && id) {
      const o = db.orders.find((x) => x.id === id);
      if (!o) throw new Error("Order not found: " + id);
      return orderView(db, o);
    }
    if (method === "POST" && !id) {
      const oid = req(params.id, "Order ID is required.");
      if (db.orders.some((o) => o.id === oid)) throw new Error("Order ID already exists.");
      const cid = req(params.customerId, "Customer is required.");
      if (!db.customers.some((c) => c.id === cid)) throw new Error("Customer not found: " + cid);
      db.orders.push({
        id: oid, customerId: cid,
        date: req(params.date, "Date is required."),
        status: "PENDING", details: [],
      });
      saveDB();
      return { ok: true, message: "Transaction created successfully." };
    }
    if (method === "POST" && id && action === "items") {
      const o = db.orders.find((x) => x.id === id);
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Order is already completed.");
      const p = db.products.find((x) => x.id === params.productId);
      if (!p) throw new Error("Product not found: " + params.productId);
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty <= 0) throw new Error("Quantity sold must be greater than zero.");
      if (qty > p.quantity) throw new Error("Insufficient stock for " + p.id + " (only " + p.quantity + " left).");
      const exist = o.details.find((d) => d.productId === p.id);
      if (exist) { exist.quantity += qty; exist.amount = exist.unitPrice * exist.quantity; }
      else o.details.push({ productId: p.id, productName: p.name, unitPrice: p.price, quantity: qty });
      saveDB();
      return { ok: true, message: "Product added to transaction successfully." };
    }
    if (method === "POST" && id && action === "confirm") {
      const o = db.orders.find((x) => x.id === id);
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Order is already completed.");
      if (!o.details.length) throw new Error("Transaction must have at least one product.");
      for (const d of o.details) {
        const p = db.products.find((x) => x.id === d.productId);
        if (!p) throw new Error("Product no longer exists: " + d.productId);
        if (d.quantity > p.quantity) throw new Error("Insufficient stock for " + p.id);
        p.quantity -= d.quantity;
      }
      o.status = "COMPLETED";
      saveDB();
      return { ok: true, message: "Sale completed successfully." };
    }
    if (method === "DELETE" && id) {
      const o = db.orders.find((x) => x.id === id);
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Only pending transactions can be cancelled.");
      db.orders = db.orders.filter((x) => x.id !== id);
      saveDB();
      return { ok: true, message: "Transaction cancelled." };
    }
  }

  /* ---------- reports ---------- */
  if (resource === "reports" && method === "GET") {
    const completed = db.orders.filter((o) => o.status === "COMPLETED");
    if (id === "monthly") {
      const month = Number(params.month) || new Date().getMonth() + 1;
      const year = Number(params.year) || new Date().getFullYear();
      let transactions = 0, productsSold = 0, revenue = 0;
      for (const o of completed) {
        const m = o.date.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
        if (!m) continue;
        if (Number(m[2]) !== month || Number(m[3]) !== year) continue;
        transactions++;
        const v = orderView(db, o);
        productsSold += o.details.reduce((s, d) => s + d.quantity, 0);
        revenue += v.total;
      }
      return { month, year, transactions, productsSold, revenue };
    }
    if (id === "best") {
      const map = new Map();
      for (const o of completed) for (const d of o.details) {
        const cur = map.get(d.productId) || { productId: d.productId, productName: d.productName, quantitySold: 0, revenue: 0 };
        cur.quantitySold += d.quantity;
        cur.revenue += d.unitPrice * d.quantity;
        map.set(d.productId, cur);
      }
      return [...map.values()].sort((a, b) => b.quantitySold - a.quantitySold).slice(0, Number(params.n) || 10);
    }
    if (id === "topcustomers") {
      const map = new Map();
      for (const o of completed) {
        const c = db.customers.find((x) => x.id === o.customerId);
        if (!c) continue;
        const cur = map.get(c.id) || { customerId: c.id, customerName: c.name, type: c.type, totalPurchase: 0 };
        cur.totalPurchase += orderView(db, o).total;
        map.set(c.id, cur);
      }
      return [...map.values()].sort((a, b) => b.totalPurchase - a.totalPurchase).slice(0, Number(params.n) || 10);
    }
    if (id === "lowstock") {
      return db.products.filter((p) => p.quantity <= 5);
    }
  }

  throw new Error("Unknown endpoint: " + method + " /" + path);
}

/* ---------------- modal ---------------- */

function openModal(title, bodyHTML, footerHTML) {
  document.getElementById("modal-title").textContent = title;
  document.getElementById("modal-body").innerHTML = bodyHTML;
  document.getElementById("modal-footer").innerHTML = footerHTML || "";
  document.getElementById("modal-backdrop").classList.remove("hidden");
}

function closeModal() {
  document.getElementById("modal-backdrop").classList.add("hidden");
}

document.getElementById("modal-backdrop").addEventListener("click", (e) => {
  if (e.target.id === "modal-backdrop") closeModal();
});

/* ---------------- navigation ---------------- */

document.getElementById("nav").addEventListener("click", (e) => {
  const btn = e.target.closest(".nav-btn");
  if (!btn) return;
  document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  renderTab(btn.dataset.tab);
});

function renderTab(tab) {
  if (tab === "products") renderProducts();
  else if (tab === "customers") renderCustomers();
  else if (tab === "sales") renderSales();
  else if (tab === "inventory") renderInventory();
  else if (tab === "reports") renderReports();
}

/* ================= PRODUCTS ================= */

async function renderProducts() {
  products = await api("/products");
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>🧺 Quản lý sản phẩm</h1><p>Danh mục quần áo của cửa hàng</p></div>
      <button class="btn" onclick="productForm()">+ Thêm sản phẩm</button>
    </div>
    <div class="card">
      <table>
        <thead><tr>
          <th>ID</th><th>Tên</th><th>Loại</th><th>Size</th><th>Màu</th>
          <th class="num">Giá</th><th class="num">Tồn kho</th><th></th>
        </tr></thead>
        <tbody id="product-rows"></tbody>
      </table>
    </div>`;
  const rows = document.getElementById("product-rows");
  if (!products.length) {
    rows.innerHTML = `<tr><td colspan="8" class="empty">Chưa có sản phẩm nào</td></tr>`;
    return;
  }
  rows.innerHTML = products.map((p) => `
    <tr>
      <td><b>${esc(p.id)}</b></td>
      <td>${esc(p.name)}</td>
      <td>${esc(p.category)}</td>
      <td>${esc(p.size)}</td>
      <td>${esc(p.color)}</td>
      <td class="num">${money(p.price)}</td>
      <td class="num">${p.lowStock ? `<span class="badge low">${p.quantity}</span>` : p.quantity}</td>
      <td style="white-space:nowrap">
        <button class="btn small secondary" onclick="productForm('${esc(p.id)}')">Sửa</button>
        <button class="btn small danger" onclick="deleteProduct('${esc(p.id)}')">Xóa</button>
      </td>
    </tr>`).join("");
}

function productForm(id) {
  const p = products.find((x) => x.id === id) || {};
  openModal(p.id ? "Sửa sản phẩm — " + p.id : "Thêm sản phẩm", `
    <div class="field"><label>ID sản phẩm</label>
      <input id="f-id" value="${esc(p.id || "")}" ${p.id ? "disabled" : ""}></div>
    <div class="field"><label>Tên sản phẩm</label>
      <input id="f-name" value="${esc(p.name || "")}"></div>
    <div class="form-row">
      <div class="field"><label>Loại</label><input id="f-cat" value="${esc(p.category || "")}"></div>
      <div class="field"><label>Size</label><input id="f-size" value="${esc(p.size || "")}"></div>
    </div>
    <div class="form-row">
      <div class="field"><label>Màu</label><input id="f-color" value="${esc(p.color || "")}"></div>
      <div class="field"><label>Giá (VND)</label><input id="f-price" type="number" min="1" value="${p.price || ""}"></div>
    </div>
    <div class="field"><label>Số lượng</label>
      <input id="f-qty" type="number" min="0" value="${p.quantity ?? 0}"></div>
  `, `
    <button class="btn secondary" onclick="closeModal()">Hủy</button>
    <button class="btn" onclick="saveProduct(${p.id ? `'${esc(p.id)}'` : "null"})">Lưu</button>
  `);
}

async function saveProduct(id) {
  const data = {
    id: document.getElementById("f-id").value.trim(),
    name: document.getElementById("f-name").value.trim(),
    category: document.getElementById("f-cat").value.trim(),
    size: document.getElementById("f-size").value.trim(),
    color: document.getElementById("f-color").value.trim(),
    price: document.getElementById("f-price").value,
    quantity: document.getElementById("f-qty").value,
  };
  try {
    if (id) await put("/products/" + encodeURIComponent(id), data);
    else await post("/products", data);
    closeModal();
    toast(data.message || "Thành công!");
    renderProducts();
  } catch (e) { toast(e.message, false); }
}

async function deleteProduct(id) {
  if (!confirm("Xóa sản phẩm " + id + "?")) return;
  try {
    const r = await del("/products/" + encodeURIComponent(id));
    toast(r.message || "Đã xóa!");
    renderProducts();
  } catch (e) { toast(e.message, false); }
}

/* ================= CUSTOMERS ================= */

async function renderCustomers() {
  customers = await api("/customers");
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>👥 Quản lý khách hàng</h1><p>VIP được giảm 10% · Regular không giảm giá</p></div>
      <button class="btn" onclick="customerForm()">+ Thêm khách hàng</button>
    </div>
    <div class="card">
      <table>
        <thead><tr>
          <th>ID</th><th>Tên</th><th>Điện thoại</th><th>Địa chỉ</th><th>Loại TV</th><th></th>
        </tr></thead>
        <tbody id="customer-rows"></tbody>
      </table>
    </div>`;
  const rows = document.getElementById("customer-rows");
  if (!customers.length) {
    rows.innerHTML = `<tr><td colspan="6" class="empty">Chưa có khách hàng nào</td></tr>`;
    return;
  }
  rows.innerHTML = customers.map((c) => `
    <tr>
      <td><b>${esc(c.id)}</b></td>
      <td>${esc(c.name)}</td>
      <td>${esc(c.phone)}</td>
      <td>${esc(c.address)}</td>
      <td><span class="badge ${c.type === "VIP" ? "vip" : "regular"}">${esc(c.type)}</span></td>
      <td style="white-space:nowrap">
        <button class="btn small secondary" onclick="customerForm('${esc(c.id)}')">Sửa</button>
        <button class="btn small danger" onclick="deleteCustomer('${esc(c.id)}')">Xóa</button>
      </td>
    </tr>`).join("");
}

function customerForm(id) {
  const c = customers.find((x) => x.id === id) || {};
  openModal(c.id ? "Sửa khách hàng — " + c.id : "Thêm khách hàng", `
    <div class="field"><label>ID khách hàng</label>
      <input id="c-id" value="${esc(c.id || "")}" ${c.id ? "disabled" : ""}></div>
    <div class="field"><label>Họ tên</label>
      <input id="c-name" value="${esc(c.name || "")}"></div>
    <div class="form-row">
      <div class="field"><label>Điện thoại</label><input id="c-phone" value="${esc(c.phone || "")}"></div>
      <div class="field"><label>Loại thành viên</label>
        <select id="c-type" ${c.id ? "disabled" : ""}>
          <option value="Regular" ${c.type === "Regular" ? "selected" : ""}>Regular</option>
          <option value="VIP" ${c.type === "VIP" ? "selected" : ""}>VIP (giảm 10%)</option>
        </select></div>
    </div>
    <div class="field"><label>Địa chỉ</label><input id="c-address" value="${esc(c.address || "")}"></div>
  `, `
    <button class="btn secondary" onclick="closeModal()">Hủy</button>
    <button class="btn" onclick="saveCustomer(${c.id ? `'${esc(c.id)}'` : "null"})">Lưu</button>
  `);
}

async function saveCustomer(id) {
  const data = {
    id: document.getElementById("c-id").value.trim(),
    name: document.getElementById("c-name").value.trim(),
    phone: document.getElementById("c-phone").value.trim(),
    address: document.getElementById("c-address").value.trim(),
    type: document.getElementById("c-type").value,
  };
  try {
    if (id) await put("/customers/" + encodeURIComponent(id), data);
    else await post("/customers", data);
    closeModal();
    toast(data.message || "Thành công!");
    renderCustomers();
  } catch (e) { toast(e.message, false); }
}

async function deleteCustomer(id) {
  if (!confirm("Xóa khách hàng " + id + "?")) return;
  try {
    const r = await del("/customers/" + encodeURIComponent(id));
    toast(r.message || "Đã xóa!");
    renderCustomers();
  } catch (e) { toast(e.message, false); }
}

/* ================= SALES ================= */

async function renderSales() {
  const orders = await api("/orders");
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>🧾 Bán hàng</h1><p>Tạo và quản lý các giao dịch bán hàng</p></div>
      <button class="btn" onclick="orderForm()">+ Tạo giao dịch</button>
    </div>
    <div class="card">
      <table>
        <thead><tr>
          <th>Mã GD</th><th>Ngày</th><th>Khách hàng</th><th>Loại TV</th>
          <th class="num">Tạm tính</th><th class="num">Giảm giá</th><th class="num">Tổng cộng</th>
          <th>Trạng thái</th><th></th>
        </tr></thead>
        <tbody id="order-rows"></tbody>
      </table>
    </div>`;
  const rows = document.getElementById("order-rows");
  if (!orders.length) {
    rows.innerHTML = `<tr><td colspan="9" class="empty">Chưa có giao dịch nào</td></tr>`;
    return;
  }
  rows.innerHTML = orders.map((o) => `
    <tr>
      <td><b>${esc(o.id)}</b></td>
      <td>${esc(o.date)}</td>
      <td>${esc(o.customerName)}</td>
      <td>${o.membership ? `<span class="badge ${o.membership === "VIP" ? "vip" : "regular"}">${esc(o.membership)}</span>` : "-"}</td>
      <td class="num">${money(o.subtotal)}</td>
      <td class="num">-${money(o.discount)}</td>
      <td class="num"><b>${money(o.total)}</b></td>
      <td><span class="badge ${o.status === "COMPLETED" ? "completed" : "pending"}">${o.status === "COMPLETED" ? "Hoàn tất" : "Đang xử lý"}</span></td>
      <td style="white-space:nowrap">
        <button class="btn small secondary" onclick="viewOrder('${esc(o.id)}')">Chi tiết</button>
        ${o.status === "PENDING" ? `<button class="btn small danger" onclick="deleteOrder('${esc(o.id)}')">Hủy</button>` : ""}
      </td>
    </tr>`).join("");
}

async function orderForm() {
  customers = await api("/customers");
  if (!customers.length) { toast("Cần thêm khách hàng trước!", false); return; }
  const today = new Date().toISOString().slice(0, 10).split("-").reverse().join("/");
  openModal("Tạo giao dịch bán hàng", `
    <div class="field"><label>Mã giao dịch</label><input id="o-id" placeholder="VD: T01"></div>
    <div class="field"><label>Khách hàng</label>
      <select id="o-customer">${customers.map((c) =>
        `<option value="${esc(c.id)}">${esc(c.id)} — ${esc(c.name)} (${esc(c.type)})</option>`).join("")}
      </select></div>
    <div class="field"><label>Ngày (dd/MM/yyyy)</label><input id="o-date" value="${today}"></div>
  `, `
    <button class="btn secondary" onclick="closeModal()">Hủy</button>
    <button class="btn" onclick="saveOrder()">Tạo</button>
  `);
}

async function saveOrder() {
  const id = document.getElementById("o-id").value.trim();
  try {
    const r = await post("/orders", {
      id: id,
      customerId: document.getElementById("o-customer").value,
      date: document.getElementById("o-date").value.trim(),
    });
    closeModal();
    toast(r.message || "Thành công!");
    viewOrder(id);
  } catch (e) { toast(e.message, false); }
}

async function viewOrder(id) {
  let o;
  try { o = await api("/orders/" + encodeURIComponent(id)); }
  catch (e) { toast(e.message, false); return; }

  const prods = await api("/products");
  const itemsHTML = o.details.length ? o.details.map((d) => `
    <tr>
      <td>${esc(d.productName)}</td>
      <td class="num">${d.quantity}</td>
      <td class="num">${money(d.unitPrice)}</td>
      <td class="num">${money(d.amount ?? d.unitPrice * d.quantity)}</td>
    </tr>`).join("") : `<tr><td colspan="4" class="empty">Chưa có sản phẩm trong giao dịch</td></tr>`;

  const addFormHTML = o.status === "PENDING" ? `
    <div class="form-row" style="margin-top:16px">
      <div class="field"><label>Sản phẩm</label>
        <select id="oi-product">${prods.map((p) =>
          `<option value="${esc(p.id)}">${esc(p.id)} — ${esc(p.name)} (còn ${p.quantity})</option>`).join("")}
        </select></div>
      <div class="field"><label>Số lượng</label><input id="oi-qty" type="number" min="1" value="1"></div>
    </div>
    <button class="btn small" onclick="addItem('${esc(o.id)}')">+ Thêm vào giao dịch</button>` : "";

  openModal("Hóa đơn — " + o.id, `
    <div class="bill-line"><span>Mã GD:</span><b>${esc(o.id)}</b></div>
    <div class="bill-line"><span>Khách hàng:</span><b>${esc(o.customerName)}</b></div>
    <div class="bill-line"><span>Thành viên:</span><b>${esc(o.membership || "-")}</b></div>
    <div class="bill-line"><span>Ngày:</span><b>${esc(o.date)}</b></div>
    <table style="margin-top:12px">
      <thead><tr><th>Sản phẩm</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr></thead>
      <tbody>${itemsHTML}</tbody>
    </table>
    <div style="margin-top:14px">
      <div class="bill-line"><span>Tạm tính:</span><span>${money(o.subtotal)}</span></div>
      <div class="bill-line"><span>Giảm giá (${o.membership === "VIP" ? "10%" : "0%"}):</span><span>-${money(o.discount)}</span></div>
      <div class="bill-line total"><span>TỔNG CỘNG:</span><span>${money(o.total)}</span></div>
    </div>
    ${addFormHTML}
  `, o.status === "PENDING" && o.details.length ? `
    <button class="btn secondary" onclick="closeModal()">Đóng</button>
    <button class="btn" onclick="confirmOrder('${esc(o.id)}')">✓ Xác nhận bán</button>
  ` : `<button class="btn secondary" onclick="closeModal()">Đóng</button>`);
}

async function addItem(orderId) {
  try {
    const r = await post("/orders/" + encodeURIComponent(orderId) + "/items", {
      productId: document.getElementById("oi-product").value,
      quantity: document.getElementById("oi-qty").value,
    });
    toast(r.message || "Thành công!");
    viewOrder(orderId);
  } catch (e) { toast(e.message, false); }
}

async function confirmOrder(orderId) {
  try {
    const r = await post("/orders/" + encodeURIComponent(orderId) + "/confirm", {});
    closeModal();
    toast(r.message || "Bán hàng thành công!");
    renderSales();
  } catch (e) { toast(e.message, false); }
}

async function deleteOrder(id) {
  if (!confirm("Hủy giao dịch " + id + "?")) return;
  try {
    const r = await del("/orders/" + encodeURIComponent(id));
    toast(r.message || "Đã hủy!");
    renderSales();
  } catch (e) { toast(e.message, false); }
}

/* ================= INVENTORY ================= */

let allProducts = [];

async function renderInventory() {
  allProducts = await api("/products");
  const low = allProducts.filter((p) => p.lowStock);
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>📦 Kho hàng</h1><p>Sản phẩm low stock khi số lượng ≤ 5</p></div>
    </div>
    <div class="stats">
      <div class="stat"><div class="label">Tổng số sản phẩm</div><div class="value">${all.length}</div></div>
      <div class="stat"><div class="label">Sắp hết hàng (≤5)</div><div class="value">${low.length}</div></div>
      <div class="stat"><div class="label">Tổng tồn kho</div><div class="value">${all.reduce((s, p) => s + p.quantity, 0)}</div></div>
    </div>
    <div class="card">
      <h3 style="margin-bottom:12px">⚠️ Sản phẩm sắp hết hàng</h3>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Size</th><th>Màu</th><th class="num">Tồn kho</th><th></th></tr></thead>
        <tbody id="low-rows"></tbody>
      </table>
    </div>`;
  const rows = document.getElementById("low-rows");
  if (!low.length) {
    rows.innerHTML = `<tr><td colspan="6" class="empty">Kho hàng khỏe mạnh, không có sản phẩm sắp hết 👍</td></tr>`;
    return;
  }
  rows.innerHTML = low.map((p) => `
    <tr>
      <td><b>${esc(p.id)}</b></td>
      <td>${esc(p.name)}</td>
      <td>${esc(p.size)}</td>
      <td>${esc(p.color)}</td>
      <td class="num"><span class="badge low">${p.quantity}</span></td>
      <td><button class="btn small" onclick="stockForm('${esc(p.id)}')">Cập nhật kho</button></td>
    </tr>`).join("");
}

function stockForm(id) {
  const p = allProducts.find((x) => x.id === id) || {};
  openModal("Cập nhật tồn kho — " + id, `
    <div class="bill-line"><span>Sản phẩm:</span><b>${esc(p.name || id)}</b></div>
    <div class="bill-line"><span>Tồn kho hiện tại:</span><b>${p.quantity ?? "-"}</b></div>
    <div class="field" style="margin-top:12px"><label>Số lượng mới</label>
      <input id="s-qty" type="number" min="0"></div>
  `, `
    <button class="btn secondary" onclick="closeModal()">Hủy</button>
    <button class="btn" onclick="saveStock('${esc(id)}')">Cập nhật</button>
  `);
}

async function saveStock(id) {
  try {
    const r = await post("/products/" + encodeURIComponent(id) + "/stock", {
      quantity: document.getElementById("s-qty").value,
    });
    closeModal();
    toast(r.message || "Thành công!");
    renderInventory();
  } catch (e) { toast(e.message, false); }
}

/* ================= REPORTS ================= */

async function renderReports() {
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>📊 Báo cáo</h1><p>Thống kê bán hàng của cửa hàng</p></div>
    </div>
    <div class="card" style="margin-bottom:18px">
      <h3 style="margin-bottom:12px">Báo cáo doanh thu theo tháng</h3>
      <div style="display:flex;gap:10px;align-items:end;flex-wrap:wrap">
        <div class="field" style="margin:0"><label>Tháng</label>
          <input id="r-month" type="number" min="1" max="12" value="${new Date().getMonth() + 1}"></div>
        <div class="field" style="margin:0"><label>Năm</label>
          <input id="r-year" type="number" value="${new Date().getFullYear()}"></div>
        <button class="btn" onclick="monthlyReport()">Xem báo cáo</button>
      </div>
      <div id="monthly-result" style="margin-top:16px"></div>
    </div>
    <div class="card" style="margin-bottom:18px">
      <h3 style="margin-bottom:12px">🏆 Sản phẩm bán chạy</h3>
      <table><thead><tr><th>Mã SP</th><th>Tên</th><th class="num">Số lượng bán</th><th class="num">Doanh thu</th></tr></thead>
      <tbody id="best-rows"></tbody></table>
    </div>
    <div class="card" style="margin-bottom:18px">
      <h3 style="margin-bottom:12px">💎 Khách hàng chi tiêu cao nhất</h3>
      <table><thead><tr><th>Mã KH</th><th>Tên</th><th>Loại TV</th><th class="num">Tổng chi tiêu</th></tr></thead>
      <tbody id="top-rows"></tbody></table>
    </div>
    <div class="card">
      <h3 style="margin-bottom:12px">⚠️ Báo cáo tồn kho thấp</h3>
      <table><thead><tr><th>Mã SP</th><th>Tên</th><th class="num">Tồn kho</th></tr></thead>
      <tbody id="lowstock-rows"></tbody></table>
    </div>`;
  await Promise.all([loadBest(), loadTop(), loadLowStock()]);
  monthlyReport();
}

async function monthlyReport() {
  const month = document.getElementById("r-month").value;
  const year = document.getElementById("r-year").value;
  try {
    const r = await api(`/reports/monthly?month=${month}&year=${year}`);
    document.getElementById("monthly-result").innerHTML = `
      <div class="stats" style="margin:0">
        <div class="stat"><div class="label">Số giao dịch</div><div class="value">${r.transactions}</div></div>
        <div class="stat"><div class="label">Sản phẩm bán ra</div><div class="value">${r.productsSold}</div></div>
        <div class="stat"><div class="label">Doanh thu</div><div class="value">${money(r.revenue)}</div></div>
      </div>`;
  } catch (e) { toast(e.message, false); }
}

async function loadBest() {
  const rows = await api("/reports/best?n=10");
  document.getElementById("best-rows").innerHTML = rows.length
    ? rows.map((r) => `<tr><td><b>${esc(r.productId)}</b></td><td>${esc(r.productName)}</td>
        <td class="num">${r.quantitySold}</td><td class="num">${money(r.revenue)}</td></tr>`).join("")
    : `<tr><td colspan="4" class="empty">Chưa có dữ liệu bán hàng</td></tr>`;
}

async function loadTop() {
  const rows = await api("/reports/topcustomers?n=10");
  document.getElementById("top-rows").innerHTML = rows.length
    ? rows.map((r) => `<tr><td><b>${esc(r.customerId)}</b></td><td>${esc(r.customerName)}</td>
        <td><span class="badge ${r.type === "VIP" ? "vip" : "regular"}">${esc(r.type)}</span></td>
        <td class="num">${money(r.totalPurchase)}</td></tr>`).join("")
    : `<tr><td colspan="4" class="empty">Chưa có dữ liệu bán hàng</td></tr>`;
}

async function loadLowStock() {
  const rows = await api("/reports/lowstock");
  document.getElementById("lowstock-rows").innerHTML = rows.length
    ? rows.map((p) => `<tr><td><b>${esc(p.id)}</b></td><td>${esc(p.name)}</td>
        <td class="num"><span class="badge low">${p.quantity}</span></td></tr>`).join("")
    : `<tr><td colspan="3" class="empty">Không có sản phẩm nào sắp hết hàng</td></tr>`;
}

/* ---------------- boot ---------------- */
showLocalNotice();
renderTab("products");
