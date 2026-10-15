/* ============================================================
 * api.js — Validation & Request layer (giao tiếp với Firebase)
 *
 * Mọi thao tác đi qua api()/post()/put()/del() với đường dẫn:
 *   GET  /products              → danh sách sản phẩm
 *   POST /products              → thêm sản phẩm
 *   PUT  /products/P1           → sửa sản phẩm
 *   POST /products/P1/stock     → cập nhật tồn kho
 *   ...tương tự cho customers, orders, reports
 * ============================================================ */

/* ---------------- Validation helpers ---------------- */

function req(v, msg) {
  if (v == null || String(v).trim() === "") throw new Error(msg);
  return String(v).trim();
}

function toNum(v, msg) {
  const n = Number(v);
  if (v === "" || v == null || isNaN(n)) throw new Error(msg);
  return n;
}

/* ---------------- Request layer ---------------- */

async function request(method, path, params) {
  params = params || {};

  // Tách query string (VD: /reports/monthly?month=10&year=2026)
  const qIndex = path.indexOf("?");
  const query = {};
  if (qIndex >= 0) {
    for (const pair of path.slice(qIndex + 1).split("&")) {
      if (!pair) continue;
      const eq = pair.indexOf("=");
      const k = eq < 0 ? pair : pair.slice(0, eq);
      query[k] = eq < 0 ? "" : decodeURIComponent(pair.slice(eq + 1));
    }
    path = path.slice(0, qIndex);
  }

  const seg = path.replace(/^\/+|\/+$/g, "").split("/");
  const resource = seg[0] || "";
  const id = seg[1] ? decodeURIComponent(seg[1]) : null;
  const action = seg[2] || null;

  /* ---------- products ---------- */
  if (resource === "products") {
    if (method === "GET" && !id) {
      return fbList(await fbGet("/products"))
        .map((p) => ({ ...p, lowStock: p.quantity <= 5, available: p.quantity > 0 }));
    }
    if (method === "POST" && !id) {
      const pid = req(params.id, "Product ID is required.");
      if (await fbGet("/products/" + fbId(pid))) throw new Error("Product ID already exists.");
      const price = toNum(params.price, "Price must be a number.");
      if (price <= 0) throw new Error("Price must be greater than zero.");
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty < 0) throw new Error("Quantity cannot be negative.");
      await fbPut("/products/" + fbId(pid), {
        id: pid,
        name: req(params.name, "Product name is required."),
        category: req(params.category, "Category is required."),
        size: req(params.size, "Size is required."),
        color: req(params.color, "Color is required."),
        price, quantity: qty,
      });
      return { ok: true, message: "Product added successfully." };
    }
    if (method === "PUT" && id) {
      const p = await fbGet("/products/" + fbId(id));
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
      await fbPut("/products/" + fbId(id), p);
      return { ok: true, message: "Product updated successfully." };
    }
    if (method === "DELETE" && id) {
      const p = await fbGet("/products/" + fbId(id));
      if (!p) throw new Error("Product not found: " + id);
      await fbDelete("/products/" + fbId(id));
      return { ok: true, message: "Product removed successfully." };
    }
    if (method === "POST" && id && action === "stock") {
      const p = await fbGet("/products/" + fbId(id));
      if (!p) throw new Error("Product not found: " + id);
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty < 0) throw new Error("Quantity cannot be negative.");
      p.quantity = qty;
      await fbPut("/products/" + fbId(id), p);
      return { ok: true, message: "Product stock updated successfully." };
    }
  }

  /* ---------- customers ---------- */
  if (resource === "customers") {
    if (method === "GET" && !id) {
      return fbList(await fbGet("/customers"))
        .map((c) => ({ ...c, discountRate: c.type === "VIP" ? 0.10 : 0 }));
    }
    if (method === "POST" && !id) {
      const cid = req(params.id, "Customer ID is required.");
      if (await fbGet("/customers/" + fbId(cid))) throw new Error("Customer ID already exists.");
      const type = params.type === "VIP" ? "VIP" : "Regular";
      const phone = req(params.phone, "Phone number is required.");
      if (!phone.match(/^\d{8,12}$/)) throw new Error("Phone number must contain 8-12 digits.");
      await fbPut("/customers/" + fbId(cid), {
        id: cid, type,
        name: req(params.name, "Customer name is required."),
        phone,
        address: req(params.address, "Address is required."),
      });
      return { ok: true, message: "Customer added successfully." };
    }
    if (method === "PUT" && id) {
      const c = await fbGet("/customers/" + fbId(id));
      if (!c) throw new Error("Customer not found: " + id);
      if (params.name && params.name.trim()) c.name = params.name.trim();
      if (params.phone && params.phone.trim()) {
        const phone = params.phone.trim();
        if (!phone.match(/^\d{8,12}$/)) throw new Error("Phone number must contain 8-12 digits.");
        c.phone = phone;
      }
      if (params.address && params.address.trim()) c.address = params.address.trim();
      await fbPut("/customers/" + fbId(id), c);
      return { ok: true, message: "Customer updated successfully." };
    }
    if (method === "DELETE" && id) {
      const c = await fbGet("/customers/" + fbId(id));
      if (!c) throw new Error("Customer not found: " + id);
      await fbDelete("/customers/" + fbId(id));
      return { ok: true, message: "Customer removed successfully." };
    }
  }

  /* ---------- orders ---------- */
  if (resource === "orders") {
    if (method === "GET" && !id) {
      const [orders, custs] = await Promise.all([fbGet("/orders"), fbGet("/customers")]);
      return fbList(orders).map((o) => {
        const c = fbList(custs).find((x) => x.id === o.customerId);
        return orderView({ ...o, customerName: c ? c.name : "", membership: c ? c.type : "" });
      });
    }
    if (method === "GET" && id) {
      const o = await fbGet("/orders/" + fbId(id));
      if (!o) throw new Error("Order not found: " + id);
      const c = await fbGet("/customers/" + fbId(o.customerId)).catch(() => null);
      return orderView({ ...o, customerName: c ? c.name : "", membership: c ? c.type : "" });
    }
    if (method === "POST" && !id) {
      const oid = req(params.id, "Order ID is required.");
      if (await fbGet("/orders/" + fbId(oid))) throw new Error("Order ID already exists.");
      const cid = req(params.customerId, "Customer is required.");
      if (!(await fbGet("/customers/" + fbId(cid)))) throw new Error("Customer not found: " + cid);
      await fbPut("/orders/" + fbId(oid), {
        id: oid, customerId: cid,
        date: req(params.date, "Date is required."),
        status: "PENDING", details: {},
      });
      return { ok: true, message: "Transaction created successfully." };
    }
    if (method === "POST" && id && action === "items") {
      const o = await fbGet("/orders/" + fbId(id));
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Order is already completed.");
      const p = await fbGet("/products/" + fbId(params.productId));
      if (!p) throw new Error("Product not found: " + params.productId);
      const qty = toNum(params.quantity, "Quantity must be a number.");
      if (qty <= 0) throw new Error("Quantity sold must be greater than zero.");
      if (qty > p.quantity) throw new Error("Insufficient stock for " + p.id + " (only " + p.quantity + " left).");
      const details = o.details || {};
      const exist = Object.values(details).find((d) => d.productId === p.id);
      if (exist) exist.quantity += qty;
      else details["d" + Date.now()] = { productId: p.id, productName: p.name, unitPrice: p.price, quantity: qty };
      o.details = details;
      await fbPut("/orders/" + fbId(id), o);
      return { ok: true, message: "Product added to transaction successfully." };
    }
    if (method === "POST" && id && action === "confirm") {
      const o = await fbGet("/orders/" + fbId(id));
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Order is already completed.");
      const details = fbList(o.details);
      if (!details.length) throw new Error("Transaction must have at least one product.");
      for (const d of details) {
        const p = await fbGet("/products/" + fbId(d.productId));
        if (!p) throw new Error("Product no longer exists: " + d.productId);
        if (d.quantity > p.quantity) throw new Error("Insufficient stock for " + p.id);
        p.quantity -= d.quantity;
        await fbPut("/products/" + fbId(d.productId), p);
      }
      o.status = "COMPLETED";
      await fbPut("/orders/" + fbId(id), o);
      return { ok: true, message: "Sale completed successfully." };
    }
    if (method === "DELETE" && id) {
      const o = await fbGet("/orders/" + fbId(id));
      if (!o) throw new Error("Order not found: " + id);
      if (o.status !== "PENDING") throw new Error("Only pending transactions can be cancelled.");
      await fbDelete("/orders/" + fbId(id));
      return { ok: true, message: "Transaction cancelled." };
    }
  }

  /* ---------- reports ---------- */
  if (resource === "reports" && method === "GET") {
    const completed = fbList(await fbGet("/orders")).filter((o) => o.status === "COMPLETED");
    if (id === "monthly") {
      const month = Number(query.month) || new Date().getMonth() + 1;
      const year = Number(query.year) || new Date().getFullYear();
      let transactions = 0, productsSold = 0, revenue = 0;
      for (const o of completed) {
        const m = String(o.date).match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
        if (!m) continue;
        if (Number(m[2]) !== month || Number(m[3]) !== year) continue;
        transactions++;
        const v = orderView({ ...o, membership: o.membership || "" });
        productsSold += fbList(o.details).reduce((s, d) => s + d.quantity, 0);
        revenue += v.total;
      }
      return { month, year, transactions, productsSold, revenue };
    }
    if (id === "daily") {
      const day = Number(query.day) || new Date().getDate();
      const month = Number(query.month) || new Date().getMonth() + 1;
      const year = Number(query.year) || new Date().getFullYear();
      let transactions = 0, productsSold = 0, revenue = 0;
      for (const o of completed) {
        const m = String(o.date).match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
        if (!m) continue;
        if (Number(m[1]) !== day || Number(m[2]) !== month || Number(m[3]) !== year) continue;
        transactions++;
        const v = orderView({ ...o, membership: o.membership || "" });
        productsSold += fbList(o.details).reduce((s, d) => s + d.quantity, 0);
        revenue += v.total;
      }
      return { day, month, year, transactions, productsSold, revenue };
    }
    if (id === "best") {
      const map = new Map();
      for (const o of completed) for (const d of fbList(o.details)) {
        const cur = map.get(d.productId) || { productId: d.productId, productName: d.productName, quantitySold: 0, revenue: 0 };
        cur.quantitySold += d.quantity;
        cur.revenue += d.unitPrice * d.quantity;
        map.set(d.productId, cur);
      }
      return [...map.values()].sort((a, b) => b.quantitySold - a.quantitySold).slice(0, Number(query.n) || 10);
    }
    if (id === "topcustomers") {
      const map = new Map();
      const custs = fbList(await fbGet("/customers"));
      for (const o of completed) {
        const c = custs.find((x) => x.id === o.customerId);
        if (!c) continue;
        const cur = map.get(c.id) || { customerId: c.id, customerName: c.name, type: c.type, totalPurchase: 0 };
        cur.totalPurchase += orderView({ ...o, membership: o.membership || "" }).total;
        map.set(c.id, cur);
      }
      return [...map.values()].sort((a, b) => b.totalPurchase - a.totalPurchase).slice(0, Number(query.n) || 10);
    }
    if (id === "lowstock") {
      return (await request("GET", "/products")).filter((p) => p.quantity <= 5);
    }
  }

  throw new Error("Unknown endpoint: " + method + " /" + path);
}

/* ---------------- Tổng hợp thông tin hiển thị của 1 order ---------------- */
function orderView(o) {
  const details = fbList(o.details);
  const subtotal = details.reduce((s, d) => s + d.unitPrice * d.quantity, 0);
  const discount = o.membership === "VIP" ? Math.round(subtotal * 0.10) : 0;
  return {
    id: o.id, customerId: o.customerId,
    customerName: o.customerName || "",
    membership: o.membership || "",
    date: o.date, status: o.status,
    subtotal, discount, total: subtotal - discount,
    details: details.map((d) => ({ ...d, amount: d.unitPrice * d.quantity })),
  };
}

/* ---------------- Shortcuts ---------------- */
function api(path) { return request("GET", path); }
function post(path, params) { return request("POST", path, params); }
function put(path, params) { return request("PUT", path, params); }
function del(path) { return request("DELETE", path); }
