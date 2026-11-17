/**
 * Klien API GoFile.
 *
 * Endpoint yang dipakai (sama seperti yang dipakai situs resmi gofile.io):
 *   POST /accounts                 -> kirim login link ke email
 *   GET  /accounts/website         -> tukar API token menjadi data akun
 *   GET  /contents/{id}            -> isi folder (butuh X-Website-Token)
 */

export const API_BASE = "https://api.gofile.io";
const STORAGE_KEY = "gofile-expressive-session";

export class GoFileError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = "GoFileError";
    Object.assign(this, details);
  }
}

export async function apiFetch(path, { method = "GET", body, token, headers = {} } = {}) {
  const finalHeaders = { ...headers };
  if (body !== undefined) finalHeaders["Content-Type"] = "application/json";
  if (token) finalHeaders["Authorization"] = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    throw new GoFileError("Tidak dapat terhubung ke server GoFile. Periksa koneksi lalu coba lagi.", {
      kind: "network",
      cause: error,
    });
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok || payload?.status !== "ok") {
    const statusMessages = {
      "error-wrongToken": "Token tidak valid atau sudah kedaluwarsa.",
      "error-token": "Token tidak valid atau sudah kedaluwarsa.",
      "error-email": "Alamat email tidak valid.",
      "error-rateLimit": "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi.",
      "error-accountId": "Akun tidak ditemukan.",
      "error-noResponse": "GoFile belum merespons. Coba lagi sebentar.",
    };
    const message =
      payload?.data?.message ||
      payload?.message ||
      statusMessages[payload?.status] ||
      (response.status === 429
        ? "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
        : `GoFile menjawab ${response.status}.`);
    throw new GoFileError(message, {
      kind: response.status === 429 ? "rateLimit" : response.status >= 500 ? "server" : "input",
      httpStatus: response.status,
      apiStatus: payload?.status,
    });
  }

  return payload.data;
}

/** Kirim email berisi login link. GoFile akan membuat akun jika belum ada. */
export function sendLoginLink(email) {
  return apiFetch("/accounts", { method: "POST", body: { email } });
}

/** Verifikasi token & ambil profil (termasuk rootFolder). */
export function getAccount(token) {
  return apiFetch("/accounts/website", { token });
}

/**
 * GoFile melindungi endpoint /contents dengan "website token" yang dihasilkan
 * skrip resmi https://gofile.io/js/wt.obf.js (dimuat di index.html).
 */
export async function websiteToken(token) {
  if (typeof window.generateWT !== "function") {
    throw new GoFileError("Generator token website GoFile belum termuat. Muat ulang halaman.", {
      kind: "setup",
    });
  }
  return window.generateWT(token);
}

/** Ambil isi folder. Mengembalikan objek `children` (map id -> item). */
export async function getFolder(token, folderId, { sortField = "name", sortDirection = 1 } = {}) {
  const wt = await websiteToken(token);
  const query = new URLSearchParams({
    page: "1",
    pageSize: "100",
    sortField,
    sortDirection: String(sortDirection),
  });
  return apiFetch(`/contents/${encodeURIComponent(folderId)}?${query}`, {
    token,
    headers: {
      "X-Website-Token": wt,
      "X-BL": navigator.language || "en",
    },
  });
}

/* --------------------------- sesi lokal --------------------------- */

export function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    return session?.token && session?.account?.rootFolder ? session : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
}

/** Terima URL penuh (`https://gofile.io/login/xxxx`) atau token mentah 32 char. */
export function extractToken(input) {
  const clean = String(input ?? "").trim().replace(/[\s"'`]/g, "");
  const fromUrl = clean.match(/(?:^|\/)login\/([a-zA-Z0-9]{32})(?:[?#].*)?$/i);
  if (fromUrl) return fromUrl[1];
  if (/^[a-zA-Z0-9]{32}$/.test(clean)) return clean;
  return null;
}
