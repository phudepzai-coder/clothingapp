/* ============================================================
 * sales.js — Bán hàng: tạo giao dịch, thêm sản phẩm, xác nhận
 * Hỗ trợ phân quyền: Admin xem tất cả, Khách hàng chỉ xem đơn của mình
 * ============================================================ */

/* Hiển thị danh sách giao dịch */
async function renderSales() {
  let orders = await api("/orders");
  const user = getCurrentUser();
  const isAdmin = user && user.role === "admin";
  const isCustomer = user && user.role === "customer";

  // Nếu là khách hàng, chỉ lọc ra các đơn hàng của chính khách hàng đó
  if (isCustomer) {
    orders = orders.filter((o) => o.customerId === user.customerId);
  }

  const createOrderBtn = isAdmin
    ? `<button class="btn" onclick="orderForm()">+ Tạo giao dịch</button>`
    : `<button class="btn" onclick="renderTab('products')">🛍️ Tiếp tục mua sắm</button>`;

  $main.innerHTML = `
    <div class="page-header">
      <div>
        <h1>${isCustomer ? '📦 Đơn mua của tôi' : '🧾 Quản lý bán hàng'}</h1>
        <p>${isCustomer ? 'Lịch sử mua sắm và hóa đơn chi tiết của bạn' : 'Tạo và quản lý tất cả giao dịch bán hàng của cửa hàng'}</p>
      </div>
      ${createOrderBtn}
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
    rows.innerHTML = `<tr><td colspan="9" class="empty">${isCustomer ? 'Bạn chưa có đơn đặt hàng nào. Hãy ghé tab Sản phẩm để mua sắm nhé!' : 'Chưa có giao dịch nào'}</td></tr>`;
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
      <td style="white-space:nowrap; text-align:right">
        <button class="btn small secondary" onclick="viewOrder('${esc(o.id)}')">Chi tiết</button>
        ${isAdmin && o.status === "PENDING" ? `<button class="btn small danger" onclick="deleteOrder('${esc(o.id)}')">Hủy</button>` : ""}
      </td>
    </tr>`).join("");
}

/* Mở form tạo giao dịch mới (Admin) */
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

/* Lưu giao dịch mới */
async function saveOrder() {
  const id = document.getElementById("o-id").value.trim();
  try {
    const r = await post("/orders", {
      id,
      customerId: document.getElementById("o-customer").value,
      date: document.getElementById("o-date").value.trim(),
    });
    closeModal();
    toast(r.message || "Thành công!");
    viewOrder(id);
  } catch (e) { toast(e.message, false); }
}

/* Xem chi tiết / hóa đơn giao dịch */
async function viewOrder(id) {
  let o;
  try { o = await api("/orders/" + encodeURIComponent(id)); }
  catch (e) { toast(e.message, false); return; }

  const user = getCurrentUser();
  const isAdmin = user && user.role === "admin";
  const prods = await api("/products");

  const itemsHTML = o.details.length
    ? o.details.map((d) => `
        <tr>
          <td>${esc(d.productName)}</td>
          <td class="num">${d.quantity}</td>
          <td class="num">${money(d.unitPrice)}</td>
          <td class="num">${money(d.amount)}</td>
        </tr>`).join("")
    : `<tr><td colspan="4" class="empty">Chưa có sản phẩm trong giao dịch</td></tr>`;

  const addFormHTML = (isAdmin && o.status === "PENDING") ? `
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
  `, (isAdmin && o.status === "PENDING" && o.details.length) ? `
    <button class="btn secondary" onclick="closeModal()">Đóng</button>
    <button class="btn" onclick="confirmOrder('${esc(o.id)}')">✓ Xác nhận bán</button>
  ` : `<button class="btn secondary" onclick="closeModal()">Đóng</button>`);
}

/* Thêm sản phẩm vào giao dịch */
async function addItem(orderId) {
  try {
    const r = await post("/orders/" + encodeURIComponent(orderId) + "/items", {
      productId: document.getElementById("oi-product").value,
      quantity:  document.getElementById("oi-qty").value,
    });
    toast(r.message || "Thành công!");
    viewOrder(orderId);
  } catch (e) { toast(e.message, false); }
}

/* Xác nhận hoàn tất giao dịch */
async function confirmOrder(orderId) {
  try {
    const r = await post("/orders/" + encodeURIComponent(orderId) + "/confirm", {});
    closeModal();
    toast(r.message || "Bán hàng thành công!");
    renderSales();
  } catch (e) { toast(e.message, false); }
}

/* Hủy giao dịch đang xử lý */
async function deleteOrder(id) {
  if (!confirm("Hủy giao dịch " + id + "?")) return;
  try {
    const r = await del("/orders/" + encodeURIComponent(id));
    toast(r.message || "Đã hủy!");
    renderSales();
  } catch (e) { toast(e.message, false); }
}
