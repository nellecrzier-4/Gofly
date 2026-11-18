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

const REQUEST_TIMEOUT_MS = 20_000;

export async function apiFetch(path, { method = "GET", body, token, headers = {} } = {}) {
  const finalHeaders = { ...headers };
  if (body !== undefined) finalHeaders["Content-Type"] = "application/json";
  if (token) finalHeaders["Authorization"] = `Bearer ${token}`;

  // Tanpa timeout, fetch bisa menggantung selamanya di WebView/HP dan overlay
  // "busy" tidak pernah hilang (terlihat seperti aplikasi stuck).
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
  } catch (error) {
    const aborted = error?.name === "AbortError";
    throw new GoFileError(
      aborted
        ? "Waktu tunggu habis. Periksa koneksi internet lalu coba lagi."
        : "Tidak dapat terhubung ke server GoFile. Periksa koneksi lalu coba lagi.",
      { kind: aborted ? "timeout" : "network", cause: error },
    );
  } finally {
    clearTimeout(timer);
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
  // Di WebView/HP, skrip eksternal kadang masih termuat saat halaman siap.
  // Tunggu sebentar sebelum menyerah, daripada langsung menggagalkan login.
  if (typeof window.generateWT !== "function") {
    await new Promise((resolve) => {
      const started = Date.now();
      const check = () => {
        if (typeof window.generateWT === "function" || Date.now() - started > 8000) resolve();
        else setTimeout(check, 120);
      };
      check();
    });
  }
  if (typeof window.generateWT !== "function") {
    throw new GoFileError("Generator token website GoFile belum termuat. Periksa koneksi lalu muat ulang halaman.", {
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

/**
 * Terima URL penuh (`https://gofile.io/login/<token>`), URL dengan query
 * (`?token=...`), atau token mentah 32 karakter. Toleran terhadap slash
 * tambahan, spasi, tanda kutip hasil copy, dan parameter pelacakan email.
 */
export function extractToken(input) {
  const clean = String(input ?? "")
    .trim()
    .replace(/[\s"'`]/g, "");
  if (!clean) return null;

  const tokenPattern = "[a-zA-Z0-9_-]{16,64}";
  const fromPath = clean.match(new RegExp(`login/(${tokenPattern})`, "i"));
  if (fromPath) return fromPath[1];

  const fromQuery = clean.match(new RegExp(`[?&](?:token|apiToken)=(${tokenPattern})`, "i"));
  if (fromQuery) return fromQuery[1];

  if (new RegExp(`^${tokenPattern}$`).test(clean)) return clean;
  return null;
}
