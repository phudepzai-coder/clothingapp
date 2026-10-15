/* ============================================================
 * reports.js — Báo cáo: doanh thu ngày/tháng, bán chạy, khách VIP, low stock
 * ============================================================ */

/* Hiển thị trang báo cáo */
async function renderReports() {
  const now = new Date();
  $main.innerHTML = `
    <div class="page-header">
      <div><h1>📊 Báo cáo & Thống kê</h1><p>Theo dõi doanh thu ngày/tháng, sản phẩm bán chạy, khách VIP</p></div>
    </div>

    <!-- Báo cáo doanh thu -->
    <div class="card" style="margin-bottom:18px">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; flex-wrap:wrap; gap:10px;">
        <h3>💰 Báo cáo doanh thu</h3>
        <div style="display:flex; gap:8px;">
          <button id="btn-mode-month" class="btn small" onclick="switchReportMode('month')">Theo Tháng</button>
          <button id="btn-mode-day" class="btn small secondary" onclick="switchReportMode('day')">Theo Ngày</button>
        </div>
      </div>

      <!-- Form lọc theo Tháng -->
      <div id="filter-month" style="display:flex; gap:10px; align-items:end; flex-wrap:wrap;">
        <div class="field" style="margin:0"><label>Tháng</label>
          <input id="r-month" type="number" min="1" max="12" value="${now.getMonth() + 1}"></div>
        <div class="field" style="margin:0"><label>Năm</label>
          <input id="r-year" type="number" value="${now.getFullYear()}"></div>
        <button class="btn" onclick="fetchMonthlyReport()">Xem báo cáo tháng</button>
      </div>

      <!-- Form lọc theo Ngày -->
      <div id="filter-day" style="display:none; gap:10px; align-items:end; flex-wrap:wrap;">
        <div class="field" style="margin:0"><label>Ngày</label>
          <input id="r-day" type="number" min="1" max="31" value="${now.getDate()}"></div>
        <div class="field" style="margin:0"><label>Tháng</label>
          <input id="r-day-month" type="number" min="1" max="12" value="${now.getMonth() + 1}"></div>
        <div class="field" style="margin:0"><label>Năm</label>
          <input id="r-day-year" type="number" value="${now.getFullYear()}"></div>
        <button class="btn" onclick="fetchDailyReport()">Xem báo cáo ngày</button>
      </div>

      <div id="revenue-result" style="margin-top:16px"></div>
    </div>

    <!-- Sản phẩm bán chạy -->
    <div class="card" style="margin-bottom:18px">
      <h3 style="margin-bottom:12px">🏆 Top 10 sản phẩm bán chạy</h3>
      <table><thead><tr><th>Mã SP</th><th>Tên sản phẩm</th><th class="num">Số lượng bán</th><th class="num">Doanh thu</th></tr></thead>
      <tbody id="best-rows"></tbody></table>
    </div>

    <!-- Khách hàng chi tiêu cao nhất -->
    <div class="card" style="margin-bottom:18px">
      <h3 style="margin-bottom:12px">💎 Top khách hàng chi tiêu cao nhất</h3>
      <table><thead><tr><th>Mã KH</th><th>Tên khách hàng</th><th>Loại TV</th><th class="num">Tổng chi tiêu</th></tr></thead>
      <tbody id="top-rows"></tbody></table>
    </div>

    <!-- Low stock -->
    <div class="card">
      <h3 style="margin-bottom:12px">⚠️ Báo cáo sản phẩm tồn kho thấp (≤ 5)</h3>
      <table><thead><tr><th>Mã SP</th><th>Tên sản phẩm</th><th class="num">Tồn kho</th></tr></thead>
      <tbody id="lowstock-rows"></tbody></table>
    </div>`;

  await Promise.all([loadBest(), loadTop(), loadLowStock()]);
  fetchMonthlyReport();
}

function switchReportMode(mode) {
  const isMonth = mode === "month";
  document.getElementById("filter-month").style.display = isMonth ? "flex" : "none";
  document.getElementById("filter-day").style.display = isMonth ? "none" : "flex";
  document.getElementById("btn-mode-month").className = isMonth ? "btn small" : "btn small secondary";
  document.getElementById("btn-mode-day").className = isMonth ? "btn small secondary" : "btn small";

  if (isMonth) fetchMonthlyReport();
  else fetchDailyReport();
}

/* Báo cáo doanh thu tháng */
async function fetchMonthlyReport() {
  const month = document.getElementById("r-month").value;
  const year  = document.getElementById("r-year").value;
  try {
    const r = await api(`/reports/monthly?month=${month}&year=${year}`);
    document.getElementById("revenue-result").innerHTML = `
      <div class="stats" style="margin:0">
        <div class="stat"><div class="label">Kỳ báo cáo</div><div class="value" style="font-size:1.15rem">${month}/${year}</div></div>
        <div class="stat"><div class="label">Số giao dịch</div><div class="value">${r.transactions}</div></div>
        <div class="stat"><div class="label">Sản phẩm bán ra</div><div class="value">${r.productsSold}</div></div>
        <div class="stat"><div class="label">Tổng doanh thu</div><div class="value" style="color:var(--green)">${money(r.revenue)}</div></div>
      </div>`;
  } catch (e) { toast(e.message, false); }
}

/* Báo cáo doanh thu ngày */
async function fetchDailyReport() {
  const day   = document.getElementById("r-day").value;
  const month = document.getElementById("r-day-month").value;
  const year  = document.getElementById("r-day-year").value;
  try {
    const r = await api(`/reports/daily?day=${day}&month=${month}&year=${year}`);
    document.getElementById("revenue-result").innerHTML = `
      <div class="stats" style="margin:0">
        <div class="stat"><div class="label">Ngày báo cáo</div><div class="value" style="font-size:1.15rem">${day}/${month}/${year}</div></div>
        <div class="stat"><div class="label">Số giao dịch</div><div class="value">${r.transactions}</div></div>
        <div class="stat"><div class="label">Sản phẩm bán ra</div><div class="value">${r.productsSold}</div></div>
        <div class="stat"><div class="label">Tổng doanh thu</div><div class="value" style="color:var(--green)">${money(r.revenue)}</div></div>
      </div>`;
  } catch (e) { toast(e.message, false); }
}

/* Sản phẩm bán chạy nhất */
async function loadBest() {
  const rows = await api("/reports/best?n=10");
  document.getElementById("best-rows").innerHTML = rows.length
    ? rows.map((r) => `
        <tr>
          <td><b>${esc(r.productId)}</b></td>
          <td>${esc(r.productName)}</td>
          <td class="num">${r.quantitySold}</td>
          <td class="num">${money(r.revenue)}</td>
        </tr>`).join("")
    : `<tr><td colspan="4" class="empty">Chưa có dữ liệu bán hàng</td></tr>`;
}

/* Khách hàng chi tiêu cao nhất */
async function loadTop() {
  const rows = await api("/reports/topcustomers?n=10");
  document.getElementById("top-rows").innerHTML = rows.length
    ? rows.map((r) => `
        <tr>
          <td><b>${esc(r.customerId)}</b></td>
          <td>${esc(r.customerName)}</td>
          <td><span class="badge ${r.type === "VIP" ? "vip" : "regular"}">${esc(r.type)}</span></td>
          <td class="num">${money(r.totalPurchase)}</td>
        </tr>`).join("")
    : `<tr><td colspan="4" class="empty">Chưa có dữ liệu bán hàng</td></tr>`;
}

/* Sản phẩm tồn kho thấp */
async function loadLowStock() {
  const rows = await api("/reports/lowstock");
  document.getElementById("lowstock-rows").innerHTML = rows.length
    ? rows.map((p) => `
        <tr>
          <td><b>${esc(p.id)}</b></td>
          <td>${esc(p.name)}</td>
          <td class="num"><span class="badge low">${p.quantity}</span></td>
        </tr>`).join("")
    : `<tr><td colspan="3" class="empty">Không có sản phẩm nào sắp hết hàng</td></tr>`;
}
