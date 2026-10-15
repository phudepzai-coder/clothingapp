/* ============================================================
 * firebase.js — Cấu hình Firebase & các hàm CRUD cơ bản
 * ============================================================ */

const FB_URL = "https://clothingapp-59ca7-default-rtdb.asia-southeast1.firebasedatabase.app";

const fbId = (p) => encodeURIComponent(p);

async function fbGet(path) {
  const res = await fetch(FB_URL + path + ".json");
  if (!res.ok) throw new Error("Firebase HTTP " + res.status);
  return res.json();
}

async function fbPut(path, value) {
  const res = await fetch(FB_URL + path + ".json", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(value),
  });
  if (!res.ok) throw new Error("Firebase HTTP " + res.status);
  return res.json();
}

async function fbDelete(path) {
  const res = await fetch(FB_URL + path + ".json", { method: "DELETE" });
  if (!res.ok) throw new Error("Firebase HTTP " + res.status);
}

/** Chuyển object Firebase (key→value) thành mảng */
function fbList(obj) {
  return obj ? Object.keys(obj).map((k) => obj[k]) : [];
}
