/* ============================================================
 * auth.js — Quản lý Đăng nhập, Đăng ký và Phân quyền
 * Phân quyền:
 *   - Admin: Quản lý toàn bộ sản phẩm, khách hàng, bán hàng, kho, báo cáo
 *   - Khách hàng: Mua sắm sản phẩm, xem đơn hàng của mình, xem hạng thành viên
 * ============================================================ */

const AUTH_KEY = "clothing_store_current_user";

/* Lấy thông tin user hiện tại đang đăng nhập */
function getCurrentUser() {
  try {
    const raw = localStorage.getItem(AUTH_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

/* Lưu thông tin user */
function setCurrentUser(user) {
  if (user) {
    localStorage.setItem(AUTH_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(AUTH_KEY);
  }
  updateAuthUI();
}

/* Đăng xuất */
function logout() {
  setCurrentUser(null);
  toast("Đã đăng xuất thành công!");
  renderProducts();
}

/* Mở Modal Đăng nhập */
function openLoginModal() {
  openModal("Đăng nhập hệ thống", `
    <div style="margin-bottom:14px; color:var(--muted); font-size:0.88rem;">
      Đăng nhập tài khoản <b>Quản trị viên</b> hoặc <b>Khách hàng</b>
    </div>
    <div class="field">
      <label>Tên đăng nhập / Số điện thoại</label>
      <input id="login-user" placeholder="Admin: 'admin' | Khách: SĐT (VD: 0901234567)" autocomplete="username">
    </div>
    <div class="field">
      <label>Mật khẩu</label>
      <input id="login-pass" type="password" placeholder="Nhập mật khẩu (Mặc định: 123)" autocomplete="current-password">
    </div>
    <div style="font-size:0.85rem; color:var(--muted); margin-top:8px;">
      💡 Tài khoản mẫu:<br>
      • <b>Admin:</b> <code>admin</code> | MK: <code>123</code><br>
      • <b>Khách VIP:</b> <code>0901234567</code> | MK: <code>123</code>
    </div>
  `, `
    <button class="btn secondary" onclick="openRegisterModal()">Chưa có tài khoản? Đăng ký</button>
    <button class="btn" onclick="handleLogin()">Đăng nhập</button>
  `);
}

/* Xử lý Đăng nhập */
async function handleLogin() {
  const username = document.getElementById("login-user").value.trim();
  const password = document.getElementById("login-pass").value;

  if (!username) { toast("Vui lòng nhập tên đăng nhập / SĐT!", false); return; }
  if (!password) { toast("Vui lòng nhập mật khẩu!", false); return; }

  try {
    const acc = await fbGet("/accounts/" + fbId(username));
    if (!acc) {
      throw new Error("Tài khoản không tồn tại. Vui lòng kiểm tra lại hoặc Đăng ký!");
    }
    if (String(acc.password) !== String(password)) {
      throw new Error("Mật khẩu không chính xác!");
    }

    setCurrentUser(acc);
    closeModal();
    toast(`Xin chào ${acc.name}! Đăng nhập thành công.`);

    // Chuyển về tab phù hợp
    if (acc.role === "admin") {
      renderProducts();
    } else {
      renderProducts();
    }
  } catch (e) {
    toast(e.message, false);
  }
}

/* Mở Modal Đăng ký khách hàng mới */
function openRegisterModal() {
  openModal("Đăng ký tài khoản Khách hàng", `
    <div class="field">
      <label>Họ và tên</label>
      <input id="reg-name" placeholder="VD: Nguyễn Văn Nam">
    </div>
    <div class="field">
      <label>Số điện thoại (dùng làm tên đăng nhập)</label>
      <input id="reg-phone" placeholder="VD: 0912345678 (8-12 chữ số)">
    </div>
    <div class="field">
      <label>Mật khẩu</label>
      <input id="reg-pass" type="password" placeholder="Nhập mật khẩu">
    </div>
    <div class="field">
      <label>Địa chỉ</label>
      <input id="reg-address" placeholder="VD: Quận 1, TP. Hồ Chí Minh">
    </div>
  `, `
    <button class="btn secondary" onclick="openLoginModal()">Đã có tài khoản? Đăng nhập</button>
    <button class="btn" onclick="handleRegister()">Đăng ký ngay</button>
  `);
}

/* Xử lý Đăng ký */
async function handleRegister() {
  const name = document.getElementById("reg-name").value.trim();
  const phone = document.getElementById("reg-phone").value.trim();
  const password = document.getElementById("reg-pass").value;
  const address = document.getElementById("reg-address").value.trim();

  if (!name) { toast("Họ tên không được để trống!", false); return; }
  if (!phone.match(/^\d{8,12}$/)) { toast("Số điện thoại phải chứa 8-12 chữ số!", false); return; }
  if (!password || password.length < 3) { toast("Mật khẩu phải từ 3 ký tự trở lên!", false); return; }
  if (!address) { toast("Địa chỉ không được để trống!", false); return; }

  try {
    // Kiểm tra xem số điện thoại đã đăng ký chưa
    const existing = await fbGet("/accounts/" + fbId(phone));
    if (existing) {
      throw new Error("Số điện thoại này đã được đăng ký tài khoản!");
    }

    // Tạo mã khách hàng mới (VD: C09, C10...)
    const allCusts = fbList(await fbGet("/customers"));
    const newId = "C" + String(allCusts.length + 1).padStart(2, "0");

    // 1. Thêm khách hàng vào /customers
    const customerData = {
      id: newId,
      name,
      phone,
      address,
      type: "Regular", // Khách hàng mới là Regular
    };
    await fbPut("/customers/" + fbId(newId), customerData);

    // 2. Thêm tài khoản vào /accounts
    const accountData = {
      username: phone,
      password,
      role: "customer",
      customerId: newId,
      name,
      phone,
      address,
      type: "Regular",
    };
    await fbPut("/accounts/" + fbId(phone), accountData);

    // Tự động đăng nhập
    setCurrentUser(accountData);
    closeModal();
    toast(`Đăng ký thành công! Chào mừng ${name}.`);
    renderProducts();
  } catch (e) {
    toast(e.message, false);
  }
}

/* Cập nhật giao diện Sidebar và Menu theo quyền User */
function updateAuthUI() {
  const user = getCurrentUser();
  const userBox = document.getElementById("user-profile-box");
  const nav = document.getElementById("nav");

  if (!userBox || !nav) return;

  if (!user) {
    // Khách vãng lai (Chưa đăng nhập)
    userBox.innerHTML = `
      <div style="padding:10px; background:rgba(255,255,255,0.06); border-radius:10px; margin-bottom:12px;">
        <div style="font-size:0.82rem; color:var(--sidebar-text); margin-bottom:6px;">Khách vãng lai</div>
        <button class="btn small" style="width:100%" onclick="openLoginModal()">🔑 Đăng nhập / Đăng ký</button>
      </div>`;

    nav.innerHTML = `
      <button class="nav-btn active" data-tab="products">🧺 Sản phẩm</button>
      <button class="nav-btn" data-tab="customers">👥 Khách hàng</button>
      <button class="nav-btn" data-tab="sales">🧾 Bán hàng</button>
      <button class="nav-btn" data-tab="inventory">📦 Kho hàng</button>
      <button class="nav-btn" data-tab="reports">📊 Báo cáo</button>`;
  } else if (user.role === "admin") {
    // Quản trị viên (Admin)
    userBox.innerHTML = `
      <div style="padding:10px; background:rgba(255,255,255,0.08); border-radius:10px; margin-bottom:12px; border:1px solid rgba(255,255,255,0.12)">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <b style="color:#fff; font-size:0.88rem;">👑 ${esc(user.name)}</b>
          <span class="badge" style="background:#4338ca; color:#fff; font-size:0.7rem;">Admin</span>
        </div>
        <div style="font-size:0.75rem; color:var(--sidebar-text); margin-top:4px;">Toàn quyền quản trị</div>
        <button class="btn small secondary" style="width:100%; margin-top:8px; padding:4px 8px; font-size:0.78rem;" onclick="logout()">Đăng xuất</button>
      </div>`;

    nav.innerHTML = `
      <button class="nav-btn active" data-tab="products">🧺 Quản lý sản phẩm</button>
      <button class="nav-btn" data-tab="customers">👥 Quản lý khách hàng</button>
      <button class="nav-btn" data-tab="sales">🧾 Quản lý bán hàng</button>
      <button class="nav-btn" data-tab="inventory">📦 Quản lý kho hàng</button>
      <button class="nav-btn" data-tab="reports">📊 Báo cáo & Doanh thu</button>`;
  } else {
    // Khách hàng (Customer)
    const isVip = user.type === "VIP";
    userBox.innerHTML = `
      <div style="padding:10px; background:rgba(255,255,255,0.08); border-radius:10px; margin-bottom:12px; border:1px solid rgba(255,255,255,0.12)">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <b style="color:#fff; font-size:0.88rem;">👤 ${esc(user.name)}</b>
          <span class="badge ${isVip ? 'vip' : 'regular'}" style="font-size:0.7rem;">${esc(user.type || 'Regular')}</span>
        </div>
        <div style="font-size:0.75rem; color:var(--sidebar-text); margin-top:4px;">
          Mã KH: <b>${esc(user.customerId || '')}</b> ${isVip ? '· Giảm 10%' : ''}
        </div>
        <button class="btn small secondary" style="width:100%; margin-top:8px; padding:4px 8px; font-size:0.78rem;" onclick="logout()">Đăng xuất</button>
      </div>`;

    nav.innerHTML = `
      <button class="nav-btn active" data-tab="products">🛍️ Mua sắm sản phẩm</button>
      <button class="nav-btn" data-tab="sales">📦 Đơn hàng của tôi</button>`;
  }
}
