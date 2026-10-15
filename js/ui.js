/* ============================================================
 * ui.js — UI helpers dùng chung: toast, modal, navigation
 * ============================================================ */

const $main = document.getElementById("main");

/* ---------------- Format tiền ---------------- */
function money(v) {
  return Number(v).toLocaleString("vi-VN") + " VND";
}

/* ---------------- Escape HTML (chống XSS) ---------------- */
function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

/* ---------------- Toast notification ---------------- */
function toast(msg, ok = true) {
  const el = document.createElement("div");
  el.className = "toast-msg " + (ok ? "ok" : "err");
  el.textContent = msg;
  document.getElementById("toast").appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

/* ---------------- Modal ---------------- */
function openModal(title, bodyHTML, footerHTML) {
  document.getElementById("modal-title").textContent = title;
  document.getElementById("modal-body").innerHTML = bodyHTML;
  document.getElementById("modal-footer").innerHTML = footerHTML || "";
  document.getElementById("modal-backdrop").classList.remove("hidden");
}

function closeModal() {
  document.getElementById("modal-backdrop").classList.add("hidden");
}

// Đóng modal khi click ra ngoài
document.getElementById("modal-backdrop").addEventListener("click", (e) => {
  if (e.target.id === "modal-backdrop") closeModal();
});

/* ---------------- Navigation ---------------- */
document.getElementById("nav").addEventListener("click", (e) => {
  const btn = e.target.closest(".nav-btn");
  if (!btn) return;
  document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  renderTab(btn.dataset.tab);
});

function renderTab(tab) {
  const user = typeof getCurrentUser === "function" ? getCurrentUser() : null;
  if (user && user.role === "customer" && !["products", "sales"].includes(tab)) {
    toast("Khu vực này chỉ dành cho Quản trị viên (Admin)!", false);
    renderProducts();
    return;
  }
  if (tab === "products")  renderProducts();
  else if (tab === "customers") renderCustomers();
  else if (tab === "sales")     renderSales();
  else if (tab === "inventory") renderInventory();
  else if (tab === "reports")   renderReports();
}
