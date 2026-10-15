/* ============================================================
 * customers.js — Quản lý khách hàng (Tìm kiếm, Thêm, Sửa, Xóa)
 * ============================================================ */

let customers = [];
let customerSearchKeyword = "";

/* Hiển thị danh sách khách hàng */
async function renderCustomers() {
  customers = await api("/customers");
  $main.innerHTML = `
    <div class="page-header">
      <div>
        <h1>👥 Quản lý khách hàng</h1>
        <p>VIP được giảm 10% · Regular không giảm giá · Tìm kiếm nhanh theo tên hoặc SĐT</p>
      </div>
      <button class="btn" onclick="customerForm()">+ Thêm khách hàng</button>
    </div>

    <!-- Thanh tìm kiếm khách hàng -->
    <div class="card" style="margin-bottom:18px; padding:16px;">
      <input id="cust-search" type="text" placeholder="🔍 Tìm khách hàng theo tên hoặc số điện thoại..."
        value="${esc(customerSearchKeyword)}"
        oninput="handleCustomerSearch(this.value)"
        style="width:100%; max-width:400px; padding:9px 12px; border:1px solid var(--border); border-radius:8px; font-size:0.92rem;">
    </div>

    <div class="card">
      <table>
        <thead><tr>
          <th>ID</th><th>Tên</th><th>Điện thoại</th><th>Địa chỉ</th><th>Loại TV</th><th></th>
        </tr></thead>
        <tbody id="customer-rows"></tbody>
      </table>
    </div>`;

  renderFilteredCustomers();
}

function handleCustomerSearch(kw) {
  customerSearchKeyword = kw;
  renderFilteredCustomers();
}

function renderFilteredCustomers() {
  const rows = document.getElementById("customer-rows");
  if (!rows) return;

  const kw = customerSearchKeyword.trim().toLowerCase();
  const filtered = customers.filter((c) => {
    if (!kw) return true;
    return (
      (c.name && c.name.toLowerCase().includes(kw)) ||
      (c.phone && c.phone.includes(kw)) ||
      (c.id && c.id.toLowerCase().includes(kw))
    );
  });

  if (!filtered.length) {
    rows.innerHTML = `<tr><td colspan="6" class="empty">Không tìm thấy khách hàng nào</td></tr>`;
    return;
  }

  rows.innerHTML = filtered.map((c) => `
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

/* Mở form thêm / sửa khách hàng */
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

/* Lưu khách hàng (thêm mới hoặc cập nhật) */
async function saveCustomer(id) {
  const data = {
    id:      document.getElementById("c-id").value.trim(),
    name:    document.getElementById("c-name").value.trim(),
    phone:   document.getElementById("c-phone").value.trim(),
    address: document.getElementById("c-address").value.trim(),
    type:    document.getElementById("c-type").value,
  };
  try {
    const r = id
      ? await put("/customers/" + encodeURIComponent(id), data)
      : await post("/customers", data);
    closeModal();
    toast(r.message || "Thành công!");
    renderCustomers();
  } catch (e) { toast(e.message, false); }
}

/* Xóa khách hàng */
async function deleteCustomer(id) {
  if (!confirm("Xóa khách hàng " + id + "?")) return;
  try {
    const r = await del("/customers/" + encodeURIComponent(id));
    toast(r.message || "Đã xóa!");
    renderCustomers();
  } catch (e) { toast(e.message, false); }
}
