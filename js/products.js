/* ============================================================
 * products.js — Quản lý sản phẩm & Mua hàng cho Khách
 * ============================================================ */

let products = [];
let productSearchKeyword = "";
let productFilterMode = "all"; // 'all', 'available', 'lowstock'

/* Hiển thị danh sách sản phẩm với thanh tìm kiếm và bộ lọc */
async function renderProducts() {
  products = await api("/products");
  const user = getCurrentUser();
  const isAdmin = user && user.role === "admin";

  const adminAddBtn = isAdmin
    ? `<button class="btn" onclick="productForm()">+ Thêm sản phẩm</button>`
    : `<button class="btn" onclick="openLoginModal()" style="${user ? 'display:none' : ''}">Đăng nhập để mua hàng</button>`;

  $main.innerHTML = `
    <div class="page-header">
      <div>
        <h1>${isAdmin ? '🧺 Quản lý sản phẩm' : '🛍️ Cửa hàng quần áo'}</h1>
        <p>${isAdmin ? 'Danh mục sản phẩm của cửa hàng · Hỗ trợ tìm kiếm & quản lý kho' : 'Khám phá sản phẩm thời trang chất lượng · Khách VIP được giảm ngay 10%'}</p>
      </div>
      ${adminAddBtn}
    </div>

    <!-- Thanh tìm kiếm & Bộ lọc trạng thái tồn kho -->
    <div class="card" style="margin-bottom:18px; padding:16px;">
      <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
        <div style="flex:1; min-width:240px;">
          <input id="prod-search" type="text" placeholder="🔍 Tìm theo tên, loại, size, màu sắc..."
            value="${esc(productSearchKeyword)}"
            oninput="handleProductSearch(this.value)"
            style="width:100%; padding:9px 12px; border:1px solid var(--border); border-radius:8px; font-size:0.92rem;">
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn small ${productFilterMode === 'all' ? '' : 'secondary'}"
            onclick="setProductFilter('all')">Tất cả (${products.length})</button>
          <button class="btn small ${productFilterMode === 'available' ? '' : 'secondary'}"
            onclick="setProductFilter('available')">Chỉ còn hàng (${products.filter(p => p.quantity > 0).length})</button>
          ${isAdmin ? `
          <button class="btn small ${productFilterMode === 'lowstock' ? 'danger' : 'secondary'}"
            onclick="setProductFilter('lowstock')">Sắp hết (≤5) (${products.filter(p => p.quantity <= 5).length})</button>` : ''}
        </div>
      </div>
    </div>

    <div class="card">
      <table>
        <thead><tr>
          <th>ID</th><th>Tên sản phẩm</th><th>Loại</th><th>Size</th><th>Màu</th>
          <th class="num">Giá</th><th class="num">Tồn kho</th><th></th>
        </tr></thead>
        <tbody id="product-rows"></tbody>
      </table>
    </div>`;

  renderFilteredProducts();
}

function handleProductSearch(kw) {
  productSearchKeyword = kw;
  renderFilteredProducts();
}

function setProductFilter(mode) {
  productFilterMode = mode;
  renderProducts();
}

function renderFilteredProducts() {
  const rows = document.getElementById("product-rows");
  if (!rows) return;

  const user = getCurrentUser();
  const isAdmin = user && user.role === "admin";

  const kw = productSearchKeyword.trim().toLowerCase();
  const filtered = products.filter((p) => {
    // Lọc theo chế độ tồn kho
    if (productFilterMode === "available" && p.quantity <= 0) return false;
    if (productFilterMode === "lowstock" && p.quantity > 5) return false;

    // Lọc theo từ khóa (tên, loại, size, màu, ID)
    if (!kw) return true;
    return (
      (p.name && p.name.toLowerCase().includes(kw)) ||
      (p.category && p.category.toLowerCase().includes(kw)) ||
      (p.size && p.size.toLowerCase().includes(kw)) ||
      (p.color && p.color.toLowerCase().includes(kw)) ||
      (p.id && p.id.toLowerCase().includes(kw))
    );
  });

  if (!filtered.length) {
    rows.innerHTML = `<tr><td colspan="8" class="empty">Không tìm thấy sản phẩm nào phù hợp</td></tr>`;
    return;
  }

  rows.innerHTML = filtered.map((p) => {
    let actionBtn = "";
    if (isAdmin) {
      actionBtn = `
        <button class="btn small secondary" onclick="productForm('${esc(p.id)}')">Sửa</button>
        <button class="btn small danger" onclick="deleteProduct('${esc(p.id)}')">Xóa</button>`;
    } else {
      if (p.quantity > 0) {
        actionBtn = `<button class="btn small" onclick="buyProductModal('${esc(p.id)}')">🛒 Mua ngay</button>`;
      } else {
        actionBtn = `<span class="badge" style="background:#fee2e2; color:#991b1b">Hết hàng</span>`;
      }
    }

    return `
      <tr>
        <td><b>${esc(p.id)}</b></td>
        <td>${esc(p.name)}</td>
        <td>${esc(p.category)}</td>
        <td>${esc(p.size)}</td>
        <td>${esc(p.color)}</td>
        <td class="num">${money(p.price)}</td>
        <td class="num">${p.lowStock ? `<span class="badge low">${p.quantity}</span>` : p.quantity}</td>
        <td style="white-space:nowrap; text-align:right">${actionBtn}</td>
      </tr>`;
  }).join("");
}

/* Modal Mua hàng trực tiếp cho Khách hàng */
function buyProductModal(productId) {
  const user = getCurrentUser();
  if (!user) {
    toast("Vui lòng đăng nhập hoặc đăng ký để đặt mua hàng!", false);
    openLoginModal();
    return;
  }

  const p = products.find((x) => x.id === productId);
  if (!p) return;

  const isVip = user.type === "VIP";
  const discountRate = isVip ? 0.10 : 0;

  openModal("Đặt mua sản phẩm — " + p.name, `
    <div class="bill-line"><span>Sản phẩm:</span><b>${esc(p.name)} (${esc(p.id)})</b></div>
    <div class="bill-line"><span>Đơn giá:</span><b>${money(p.price)}</b></div>
    <div class="bill-line"><span>Còn tồn trong kho:</span><b>${p.quantity} cái</b></div>
    <div class="bill-line"><span>Người mua:</span><span>${esc(user.name)} (${esc(user.type || 'Regular')})</span></div>
    
    <div class="field" style="margin-top:14px">
      <label>Chọn số lượng mua:</label>
      <input id="buy-qty" type="number" min="1" max="${p.quantity}" value="1"
        oninput="updateBuyCalculation(${p.price}, ${discountRate})">
    </div>

    <div style="background:var(--bg); padding:12px; border-radius:8px; margin-top:12px;">
      <div class="bill-line"><span>Tạm tính:</span><span id="buy-subtotal">${money(p.price)}</span></div>
      <div class="bill-line"><span>Giảm giá (${isVip ? 'VIP 10%' : '0%'}):</span><span id="buy-discount">-${money(p.price * discountRate)}</span></div>
      <div class="bill-line total"><span>TỔNG THANH TOÁN:</span><span id="buy-total">${money(p.price - (p.price * discountRate))}</span></div>
    </div>
  `, `
    <button class="btn secondary" onclick="closeModal()">Hủy</button>
    <button class="btn" onclick="confirmCustomerOrder('${esc(p.id)}')">Xác nhận đặt hàng</button>
  `);
}

function updateBuyCalculation(unitPrice, discountRate) {
  const qty = Math.max(1, parseInt(document.getElementById("buy-qty").value) || 1);
  const subtotal = unitPrice * qty;
  const discount = Math.round(subtotal * discountRate);
  const total = subtotal - discount;

  document.getElementById("buy-subtotal").textContent = money(subtotal);
  document.getElementById("buy-discount").textContent = "-" + money(discount);
  document.getElementById("buy-total").textContent = money(total);
}

/* Khách hàng xác nhận đặt mua */
async function confirmCustomerOrder(productId) {
  const user = getCurrentUser();
  if (!user) return;

  const qty = parseInt(document.getElementById("buy-qty").value) || 1;
  const today = new Date().toISOString().slice(0, 10).split("-").reverse().join("/");
  const orderId = "T" + Date.now().toString().slice(-4);

  try {
    // 1. Tạo order
    await post("/orders", {
      id: orderId,
      customerId: user.customerId,
      date: today,
    });

    // 2. Thêm sản phẩm vào order
    await post("/orders/" + encodeURIComponent(orderId) + "/items", {
      productId,
      quantity: qty,
    });

    // 3. Xác nhận hoàn tất và trừ kho
    await post("/orders/" + encodeURIComponent(orderId) + "/confirm", {});

    closeModal();
    toast(`🎉 Đặt hàng thành công! Mã đơn: ${orderId}`);
    renderProducts();
  } catch (e) {
    toast(e.message, false);
  }
}

/* ================== PHẦN CỦA ADMIN: THÊM / SỬA / XÓA ================== */

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
    id:       document.getElementById("f-id").value.trim(),
    name:     document.getElementById("f-name").value.trim(),
    category: document.getElementById("f-cat").value.trim(),
    size:     document.getElementById("f-size").value.trim(),
    color:    document.getElementById("f-color").value.trim(),
    price:    document.getElementById("f-price").value,
    quantity: document.getElementById("f-qty").value,
  };
  try {
    const r = id
      ? await put("/products/" + encodeURIComponent(id), data)
      : await post("/products", data);
    closeModal();
    toast(r.message || "Thành công!");
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
