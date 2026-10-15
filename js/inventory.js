/* ============================================================
 * inventory.js — Quản lý kho hàng (low stock & cập nhật tồn kho)
 * ============================================================ */

let allProducts = [];

/* Hiển thị tổng quan kho hàng */
async function renderInventory() {
  allProducts = await api("/products");
  const low = allProducts.filter((p) => p.lowStock);

  $main.innerHTML = `
    <div class="page-header">
      <div><h1>📦 Kho hàng</h1><p>Sản phẩm low stock khi số lượng ≤ 5</p></div>
    </div>
    <div class="stats">
      <div class="stat"><div class="label">Tổng số sản phẩm</div><div class="value">${allProducts.length}</div></div>
      <div class="stat"><div class="label">Sắp hết hàng (≤5)</div><div class="value">${low.length}</div></div>
      <div class="stat"><div class="label">Tổng tồn kho</div><div class="value">${allProducts.reduce((s, p) => s + p.quantity, 0)}</div></div>
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

/* Mở form cập nhật tồn kho */
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

/* Lưu số lượng tồn kho mới */
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
