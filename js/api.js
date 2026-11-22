/**
 * Klien API GoFile.
 *
 * Endpoint yang dipakai (sama seperti yang dipakai situs resmi gofile.io):
 *   POST /accounts                 -> kirim login link ke email
 *   GET  /accounts/website         -> tukar API token menjadi data akun
 *   GET  /contents/{id}            -> isi folder (butuh X-Website-Token)
 *   POST /contents/createFolder    -> folder baru
 *   DELETE /contents               -> hapus konten
 *   PUT  /contents/{id}/update     -> ubah satu atribut (nama, dll.)
 *   PUT  /contents/move            -> pindahkan konten
 *   POST /contents/copy            -> salin konten
 *   POST upload.gofile.io/uploadfile -> unggah satu file (multipart)
 */

export const API_BASE = "https://api.gofile.io";
export const UPLOAD_URL = "https://upload.gofile.io/uploadfile";

const STORAGE_KEY = "gofile-expressive-session";
const WT_SECRET = "12af056dacea0b";
const WT_PERIOD_MS = 4 * 60 * 60 * 1000;
const REQUEST_TIMEOUT_MS = 20_000;

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

  // Tanpa timeout, fetch bisa menggantung selamanya dan overlay "busy" tidak
  // pernah hilang (terlihat seperti aplikasi stuck).
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
      "error-unauthorized": "Sesi tidak diizinkan untuk aksi ini.",
      "error-notFound": "Item tidak ditemukan. Mungkin sudah dihapus.",
      "error-premium": "Fitur ini membutuhkan akun GoFile premium.",
      "error-emptyName": "Nama tidak boleh kosong.",
      "error-folderName": "Nama folder tidak valid.",
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

/* ------------------------- website token ------------------------- */

function localWebsiteToken(token) {
  if (!globalThis.crypto?.subtle) {
    return Promise.reject(
      new GoFileError("Browser ini tidak mendukung Web Crypto untuk token website GoFile.", { kind: "setup" }),
    );
  }
  const language = navigator.language || "en";
  const period = Math.floor(Date.now() / WT_PERIOD_MS);
  const message = `${navigator.userAgent}::${language}::${token}::${period}::${WT_SECRET}`;
  return crypto.subtle.digest("SHA-256", new TextEncoder().encode(message)).then((digest) =>
    [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join(""),
  );
}

/**
 * GoFile melindungi endpoint /contents dengan "website token".
 * Token dihitung lokal dengan SHA-256 (Web Crypto) memakai rumus resmi GoFile.
 * Kalau Web Crypto tidak tersedia (mis. halaman dibuka lewat file://), skrip
 * resmi `wt.obf.js` dipakai sebagai cadangan. Token tidak pernah dicatat ke console.
 */
async function officialWebsiteToken(token) {
  if (typeof window.generateWT !== "function") {
    await new Promise((resolve) => {
      const started = Date.now();
      const check = () => {
        if (typeof window.generateWT === "function" || Date.now() - started > 2500) resolve();
        else setTimeout(check, 100);
      };
      check();
    });
  }
  if (typeof window.generateWT !== "function") return null;
  try {
    const generated = await window.generateWT(token);
    return generated || null;
  } catch {
    return null;
  }
}

export async function websiteToken(token) {
  try {
    return await localWebsiteToken(token);
  } catch {
    const official = await officialWebsiteToken(token);
    if (official) return official;
    throw new GoFileError("Generator token website GoFile tidak tersedia di browser ini.", { kind: "setup" });
  }
}

async function contentHeaders(token, extra = {}) {
  return {
    "X-Website-Token": await websiteToken(token),
    "X-BL": navigator.language || "en",
    ...extra,
  };
}

/** Ambil isi folder. Mengembalikan objek `children` (map id -> item). */
export async function getFolder(token, folderId, { sortField = "name", sortDirection = 1 } = {}) {
  const query = new URLSearchParams({
    page: "1",
    pageSize: "1000",
    sortField,
    sortDirection: String(sortDirection),
  });
  return apiFetch(`/contents/${encodeURIComponent(folderId)}?${query}`, {
    token,
    headers: await contentHeaders(token),
  });
}

/* ------------------------- mutasi konten ------------------------- */

export async function createFolder(token, parentFolderId, folderName) {
  return apiFetch("/contents/createFolder", {
    method: "POST",
    body: { token, parentFolderId, folderName },
    token,
    headers: await contentHeaders(token),
  });
}

export async function updateContentAttribute(token, contentId, attribute, value) {
  return apiFetch(`/contents/${encodeURIComponent(contentId)}/update`, {
    method: "PUT",
    body: { attribute, attributeValue: value },
    token,
    headers: await contentHeaders(token),
  });
}

export function renameContent(token, contentId, name) {
  return updateContentAttribute(token, contentId, "name", name);
}

export async function deleteContents(token, contentIds) {
  const ids = [...contentIds].filter(Boolean);
  if (!ids.length) return null;
  return apiFetch("/contents", {
    method: "DELETE",
    body: { token, contentsId: ids.join(",") },
    token,
    headers: await contentHeaders(token),
  });
}

export async function moveContents(token, contentIds, destinationFolderId) {
  return apiFetch("/contents/move", {
    method: "PUT",
    body: { contentsId: [...contentIds].join(","), folderId: destinationFolderId },
    token,
    headers: await contentHeaders(token),
  });
}

export async function copyContents(token, contentIds, destinationFolderId) {
  return apiFetch("/contents/copy", {
    method: "POST",
    body: { contentsId: [...contentIds].join(","), folderId: destinationFolderId },
    token,
    headers: await contentHeaders(token),
  });
}

/* ------------------------------ unggah ------------------------------ */

const UPLOAD_MESSAGES = {
  "error-notEnoughSpace": "Ruang penyimpanan GoFile tidak cukup.",
  "error-folderNotFound": "Folder tujuan tidak ditemukan.",
  "error-wrongToken": "Token tidak valid atau sudah kedaluwarsa.",
  "error-rateLimit": "Terlalu banyak unggahan. Tunggu sebentar lalu coba lagi.",
};

/**
 * Unggah satu file memakai XMLHttpRequest supaya progres per file tersedia.
 * `onStart(abort)` dipanggil setelah permintaan dimulai.
 */
export function uploadFile({ file, token, folderId, onProgress = () => {}, onStart = () => {} }) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    let settled = false;

    const fail = (message, kind) => {
      if (settled) return;
      settled = true;
      reject(new GoFileError(message, { kind }));
    };

    xhr.open("POST", UPLOAD_URL);
    xhr.timeout = 30 * 60 * 1000;
    xhr.upload.addEventListener("progress", (event) => {
      if (!event.lengthComputable || !event.total) return;
      onProgress(Math.min(99, Math.round((event.loaded / event.total) * 100)));
    });

    xhr.addEventListener("load", () => {
      let payload = null;
      try {
        payload = JSON.parse(xhr.responseText);
      } catch {
        payload = null;
      }
      const apiStatus = payload?.status;
      if (xhr.status >= 200 && xhr.status < 300 && (!apiStatus || apiStatus === "ok")) {
        settled = true;
        onProgress(100);
        resolve(payload?.data ?? {});
        return;
      }
      const message =
        payload?.data?.message ||
        payload?.message ||
        UPLOAD_MESSAGES[apiStatus] ||
        `Unggahan gagal (${xhr.status || "jaringan"}).`;
      fail(message, "upload");
    });

    xhr.addEventListener("error", () => fail("Koneksi terputus saat mengunggah.", "network"));
    xhr.addEventListener("timeout", () => fail("Unggahan melewati batas waktu.", "timeout"));
    xhr.addEventListener("abort", () => fail("Unggahan dibatalkan.", "abort"));

    const form = new FormData();
    form.append("file", file, file.name);
    form.append("token", token);
    form.append("folderId", folderId);
    xhr.send(form);
    onStart(() => xhr.abort());
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
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    /* penyimpanan diblokir/penuh — sesi tetap hidup di memori tab ini */
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    /* abaikan */
  }
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
