import {
  sendLoginLink,
  getAccount,
  getFolder,
  loadSession,
  saveSession,
  clearSession,
  extractToken,
  createFolder,
  deleteContents,
  renameContent,
  moveContents,
  copyContents,
  uploadFile,
  GoFileError,
} from "./api.js";
import { qrSvg } from "./qr.js";

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const els = {
  auth: $("#auth"),
  manager: $("#manager"),
  main: $("#main-content"),
  topbar: $("#topbar"),
  managerTitle: $("#manager-title"),
  formEmail: $("#form-email"),
  formLink: $("#form-link"),
  email: $("#email"),
  loginLink: $("#login-link"),
  sentTo: $("#sent-to"),
  authError: $("#auth-error"),
  breadcrumbs: $("#breadcrumbs"),
  folderTitle: $("#folder-title"),
  folderSub: $("#folder-sub"),
  stats: $("#stats"),
  fileGrid: $("#file-grid"),
  loading: $("#loading"),
  empty: $("#empty"),
  emptyTitle: $("#empty-title"),
  emptyText: $("#empty-text"),
  emptyRetry: $("#empty-retry"),
  search: $("#search"),
  syncBtn: $("#sync-btn"),
  logoutBtn: $("#logout-btn"),
  sortBtn: $("#sort-btn"),
  sortLabel: $("#sort-label"),
  filterBtn: $("#filter-btn"),
  filterLabel: $("#filter-label"),
  filterDot: $("#filter-dot"),
  snackbar: $("#snackbar"),
  busy: $("#busy"),
  accountName: $("#account-name"),
  avatar: $("#avatar"),
  uploadBtn: $("#upload-btn"),
  newFolderBtn: $("#newfolder-btn"),
  fileInput: $("#file-input"),
  transferPanel: $("#transfer-panel"),
  selectionBar: $("#selection-bar"),
  selectionCount: $("#selection-count"),
  selDownload: $("#sel-download"),
  selMove: $("#sel-move"),
  selCopy: $("#sel-copy"),
  selDelete: $("#sel-delete"),
  selClear: $("#sel-clear"),
  menu: $("#menu"),
  details: $("#details"),
  detailsTitle: $("#details-title"),
  detailsBody: $("#details-body"),
  detailsClose: $("#details-close"),
  preview: $("#preview"),
  previewTitle: $("#preview-title"),
  previewSub: $("#preview-sub"),
  previewBody: $("#preview-body"),
  previewActions: $("#preview-actions"),
  previewClose: $("#preview-close"),
  modal: $("#modal"),
  dropOverlay: $("#drop-overlay"),
  dropFolder: $("#drop-folder"),
  dropHint: $("#drop-hint"),
  netStatus: $("#net-status"),
  syncStatus: $("#sync-status"),
  installBtn: $("#install-btn"),
};

const SORTS = [
  { field: "name", direction: 1, label: "Nama A–Z", icon: "sort_by_alpha" },
  { field: "name", direction: -1, label: "Nama Z–A", icon: "sort_by_alpha" },
  { field: "createTime", direction: -1, label: "Terbaru", icon: "schedule" },
  { field: "size", direction: -1, label: "Terbesar", icon: "data_usage" },
];

const FILTERS = [
  { id: "all", label: "Semua", icon: "select_all" },
  { id: "folder", label: "Folder", icon: "folder" },
  { id: "image", label: "Gambar", icon: "image" },
  { id: "video", label: "Video", icon: "movie" },
  { id: "audio", label: "Audio", icon: "audio_file" },
  { id: "doc", label: "Dokumen", icon: "description" },
  { id: "archive", label: "Arsip", icon: "folder_zip" },
  { id: "code", label: "Kode", icon: "code" },
];

const FOLDER_CACHE_KEY = "gofly-folders-v1";
const SHARED_KEY = "gofly-shared-ack";
const MAX_CACHED_FOLDERS = 8;

const state = {
  session: null,
  pendingEmail: "",
  path: [],
  items: [],
  sortIndex: 0,
  view: "grid",
  search: "",
  filter: "all",
  selected: new Set(),
  anchorId: null,
  transfers: {},
  uploads: {},
  dropTargetId: null,
  detailsId: null,
  previewId: null,
  menu: null,
  menuItems: [],
  picker: null,
  offline: false,
  loadError: "",
  lastFocus: null,
};

/* ============================ helpers ============================ */

function icon(name, { cls = "", fill = false, size } = {}) {
  const style = [
    size ? `font-size:${size}px` : "",
    fill ? "font-variation-settings:'FILL' 1,'wght' 560,'opsz' 32" : "",
  ]
    .filter(Boolean)
    .join(";");
  return `<span class="ms ${cls}" ${style ? `style="${style}"` : ""} aria-hidden="true">${name}</span>`;
}

function escapeHtml(value) {
  return String(value ?? "").replace(
    /[&<>"']/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" })[c],
  );
}

function toast(message, { error = false, action = null, duration = 3800 } = {}) {
  const iconName = error ? "error" : "check_circle";
  els.snackbar.innerHTML = `${icon(iconName)}<span class="snackbar-text">${escapeHtml(message)}</span>${
    action ? `<button type="button" class="snackbar-action" data-toast-action>${escapeHtml(action.label)}</button>` : ""
  }`;
  els.snackbar.classList.toggle("is-error", error);
  els.snackbar.classList.add("is-visible");
  const actionButton = $("[data-toast-action]", els.snackbar);
  if (actionButton) {
    actionButton.addEventListener("click", () => {
      els.snackbar.classList.remove("is-visible");
      action.onClick?.();
    });
  }
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => els.snackbar.classList.remove("is-visible"), duration);
}

function setBusy(on) {
  els.busy.hidden = !on;
}

function authError(message) {
  els.authError.textContent = message;
  els.authError.hidden = !message;
}

function setStep(step) {
  $$(".step").forEach((node) => {
    const n = Number(node.dataset.step);
    node.classList.toggle("is-active", n === step);
    node.classList.toggle("is-done", n < step);
    node.setAttribute("aria-current", n === step ? "step" : "false");
    const bullet = node.querySelector("span");
    bullet.textContent = n < step ? "check" : String(n);
    bullet.classList.toggle("ms", n < step);
  });
  const showEmail = step === 1;
  els.formEmail.classList.toggle("is-visible", showEmail);
  els.formEmail.hidden = !showEmail;
  els.formLink.classList.toggle("is-visible", !showEmail);
  els.formLink.hidden = showEmail;
  authError("");
}

function formatBytes(bytes) {
  const n = Number(bytes) || 0;
  if (!n) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.min(Math.floor(Math.log(n) / Math.log(1024)), units.length - 1);
  return `${(n / 1024 ** i).toFixed(i ? 1 : 0)} ${units[i]}`;
}

function formatDate(value) {
  if (!Number(value)) return "—";
  const d = new Date(Number(value) * 1000 || value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("id-ID", { day: "numeric", month: "short", year: "numeric" });
}

function formatDateTime(value) {
  if (!Number(value)) return "—";
  const d = new Date(Number(value) * 1000 || value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("id-ID", { dateStyle: "medium", timeStyle: "short" });
}

function humanError(error) {
  if (error instanceof GoFileError || error?.message) return error.message;
  return "Terjadi kesalahan. Coba lagi.";
}

function downloadCount(item) {
  return Number(item?.downloads ?? item?.downloadCount ?? 0) || 0;
}

function iconFor(item) {
  if (item.type === "folder") return { name: "folder", cls: "folder" };
  const mime = String(item.mimetype || "").toLowerCase();
  const name = String(item.name || "").toLowerCase();
  if (mime.startsWith("image/")) return { name: "image", cls: "image" };
  if (mime.startsWith("video/")) return { name: "movie", cls: "video" };
  if (mime.startsWith("audio/")) return { name: "audio_file", cls: "audio" };
  if (mime.includes("pdf")) return { name: "picture_as_pdf", cls: "doc" };
  if (/\.(zip|rar|7z|tar|gz|bz2|xz)$/.test(name)) return { name: "folder_zip", cls: "archive" };
  if (/\.(js|mjs|ts|jsx|tsx|py|go|rs|java|css|html|json|md|sh|yml|yaml)$/.test(name)) {
    return { name: "code", cls: "code" };
  }
  if (mime.includes("spreadsheet") || /\.(csv|xlsx|xls|ods)$/.test(name)) {
    return { name: "table_chart", cls: "doc" };
  }
  if (mime.startsWith("text/") || /\.(txt|doc|docx|odt|rtf)$/.test(name)) {
    return { name: "description", cls: "doc" };
  }
  return { name: "draft", cls: "" };
}

function prefersReducedMotion() {
  return window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
}

function withTransition(update) {
  if (!document.startViewTransition || prefersReducedMotion()) {
    update();
    return;
  }
  try {
    document.startViewTransition(update);
  } catch {
    update();
  }
}

function attachRipples() {
  document.addEventListener("pointerdown", (event) => {
    const target = event.target.closest(".btn, .icon-btn, .seg, .crumb");
    if (!target || target.hasAttribute("disabled")) return;
    const rect = target.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const ripple = document.createElement("span");
    ripple.className = "ripple";
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
    ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
    target.appendChild(ripple);
    ripple.addEventListener("animationend", () => ripple.remove());
  });
}

function openExternal(url) {
  if (!url) return;
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    try {
      const area = document.createElement("textarea");
      area.value = text;
      area.setAttribute("readonly", "");
      area.style.position = "fixed";
      area.style.opacity = "0";
      document.body.append(area);
      area.select();
      const ok = document.execCommand("copy");
      area.remove();
      return ok;
    } catch {
      return false;
    }
  }
}

function isTypingTarget(target) {
  if (!target) return false;
  if (target.isContentEditable) return true;
  return /^(input|textarea|select)$/i.test(target.tagName);
}

function currentFolder() {
  return state.path[state.path.length - 1] || null;
}

function itemById(id) {
  return state.items.find((item) => item.id === id) || null;
}

/* ============================ auth ============================ */

async function requestLink() {
  const email = els.email.value.trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    authError("Masukkan alamat email yang valid.");
    return;
  }
  authError("");
  setBusy(true);
  try {
    await sendLoginLink(email);
    state.pendingEmail = email;
    els.sentTo.textContent = `Link dikirim ke ${email}`;
    setStep(2);
    setTimeout(() => els.loginLink.focus(), 250);
    toast("Login link terkirim. Cek email kamu!");
  } catch (error) {
    authError(humanError(error));
  } finally {
    setBusy(false);
  }
}

async function loginWithLink(event) {
  event?.preventDefault();
  const token = extractToken(els.loginLink.value);
  if (!token) {
    authError("Link tidak dikenali. Tempel URL https://gofile.io/login/... atau token 32 karakter.");
    return;
  }
  authError("");
  setBusy(true);
  try {
    const account = await getAccount(token);
    if (!account?.rootFolder) throw new GoFileError("Akun tidak memiliki root folder.", { kind: "input" });
    state.session = { token, account };
    saveSession(state.session);
    showManager();
    await loadFolder(account.rootFolder, "Root", { reset: true });
    maybeWarnShared();
    toast("Berhasil masuk. Selamat datang!");
  } catch (error) {
    const actionable = error instanceof GoFileError && ["network", "timeout", "rateLimit", "server", "input"].includes(error.kind);
    authError(actionable ? error.message : "Login link tidak valid atau sudah kedaluwarsa. Minta link baru.");
  } finally {
    setBusy(false);
  }
}

/* Transisi halus antar-tampilan: View Transitions API bila ada, langsung bila gerak dikurangi. */
function smoothUpdate(update) {
  const reduce = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
  if (reduce || typeof document.startViewTransition !== "function") {
    update();
    return;
  }
  try {
    document.startViewTransition(update);
  } catch {
    update();
  }
}

function scrollToTopSmooth() {
  const reduce = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
  window.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" });
}

function showAuth() {
  els.manager.hidden = true;
  els.auth.hidden = false;
  setStep(1);
  setTimeout(() => els.email.focus(), 200);
}

function showManager() {
  const { account } = state.session;
  els.auth.hidden = true;
  els.manager.hidden = false;
  const display = account.username || account.email || "Akun";
  els.accountName.textContent = display;
  els.avatar.textContent = display.slice(0, 1);
  els.accountName.title = display;
}

function sharedWarningAcknowledged() {
  try {
    return localStorage.getItem(SHARED_KEY) === "1";
  } catch {
    return false;
  }
}

function acknowledgeSharedWarning() {
  try {
    localStorage.setItem(SHARED_KEY, "1");
  } catch {
    /* preferensi tidak bisa disimpan; dialog muncul lagi lain kali */
  }
}

function maybeWarnShared() {
  if (sharedWarningAcknowledged()) return;
  setTimeout(() => {
    if (!state.session || els.modal.open) return;
    openModal({
      title: "Komputer bersama?",
      body: `<p class="confirm-text">Sesi ini disimpan di browser ini supaya kamu tidak perlu login ulang. Di komputer bersama, tekan <b>Keluar</b> setelah selesai — token dan semua salinan folder akan dihapus dari browser.</p>
        <p class="modal-note">${icon("shield")} Token hanya dipakai untuk api.gofile.io dan upload.gofile.io, dan tidak pernah ditulis ke log.</p>`,
      actions: [
        {
          label: "Mengerti",
          variant: "primary",
          onClick: acknowledgeSharedWarning,
        },
      ],
    });
  }, 500);
}

/* ============================ folder cache ============================ */

function readFolderCache(id) {
  try {
    const map = JSON.parse(localStorage.getItem(FOLDER_CACHE_KEY) || "{}");
    return map[id] || null;
  } catch {
    return null;
  }
}

function writeFolderCache(id, name, items) {
  try {
    const map = JSON.parse(localStorage.getItem(FOLDER_CACHE_KEY) || "{}");
    map[id] = { id, name, at: Date.now(), items };
    const keys = Object.keys(map).sort((a, b) => (map[b].at || 0) - (map[a].at || 0));
    keys.slice(MAX_CACHED_FOLDERS).forEach((key) => delete map[key]);
    localStorage.setItem(FOLDER_CACHE_KEY, JSON.stringify(map));
  } catch {
    /* penyimpanan penuh atau diblokir — cache offline dilewati */
  }
}

function deleteFolderCacheEntry(id) {
  try {
    const map = JSON.parse(localStorage.getItem(FOLDER_CACHE_KEY) || "{}");
    delete map[id];
    localStorage.setItem(FOLDER_CACHE_KEY, JSON.stringify(map));
  } catch {
    /* abaikan */
  }
}

function clearFolderCaches() {
  try {
    localStorage.removeItem(FOLDER_CACHE_KEY);
  } catch {
    /* abaikan */
  }
  if (typeof caches !== "undefined") {
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key.startsWith("gofile-api-")).map((key) => caches.delete(key))))
      .catch(() => {});
  }
}

/* ============================ file manager ============================ */

function updateNetStatus() {
  const offline = !navigator.onLine || state.offline;
  els.netStatus.hidden = !offline;
  els.netStatus.title = offline ? "Offline — menampilkan salinan folder tersimpan" : "";
  if (els.syncStatus) els.syncStatus.textContent = offline ? "GoFile · offline" : "GoFile · tersinkron";
}

async function loadFolder(id, name, { reset = false, silent = false } = {}) {
  if (!state.session) return;
  const token = state.session.token;

  if (reset) {
    state.path = [{ id, name }];
  } else {
    const existing = state.path.findIndex((part) => part.id === id);
    if (existing >= 0) state.path = state.path.slice(0, existing + 1);
    else state.path.push({ id, name });
  }

  closeMenu(false);
  closeDetails();
  closePreview();
  clearSelection();
  state.loadError = "";
  if (!silent) scrollToTopSmooth();

  renderBreadcrumbs();
  els.managerTitle.textContent = name;
  els.managerTitle.setAttribute("aria-label", `Folder ${name}`);
  els.folderTitle.textContent = name;
  els.folderSub.textContent = "Menyinkronkan isi folder…";
  els.fileGrid.hidden = true;
  els.empty.hidden = true;
  els.loading.hidden = silent;
  if (!silent) {
    els.loading.innerHTML = Array.from({ length: 8 }, () => '<div class="skeleton"></div>').join("");
  }

  try {
    const sort = SORTS[state.sortIndex];
    const data = await getFolder(token, id, { sortField: sort.field, sortDirection: sort.direction });
    if (data?.canAccess === false) {
      throw new GoFileError("Folder ini tidak dapat diakses dengan akun ini.", { kind: "input" });
    }
    const children = data?.children || {};
    state.items = Object.entries(children).map(([key, item]) => ({
      ...item,
      id: key,
      type: String(item?.type || "file").toLowerCase(),
    }));
    state.offline = false;
    writeFolderCache(id, name, state.items);
    updateNetStatus();
    renderFiles();
  } catch (error) {
    if (error?.apiStatus === "error-wrongToken" || error?.httpStatus === 401) {
      els.loading.hidden = true;
      signOut({ message: "Sesi berakhir. Minta login link baru.", error: true });
      return;
    }
    const cached = readFolderCache(id);
    if (cached) {
      state.items = Array.isArray(cached.items) ? cached.items : [];
      state.offline = true;
      updateNetStatus();
      renderFiles();
      toast("Offline — menampilkan salinan folder terakhir.", { error: false });
    } else {
      state.items = [];
      state.offline = !navigator.onLine;
      state.loadError = humanError(error);
      updateNetStatus();
      renderFiles();
      toast(state.loadError, { error: true });
    }
  } finally {
    els.loading.hidden = true;
  }
}

async function reloadCurrent() {
  const folder = currentFolder();
  if (!folder) return;
  await loadFolder(folder.id, folder.name);
}

function renderBreadcrumbs() {
  els.breadcrumbs.innerHTML = state.path
    .map(
      (part, index) => `
      <button class="crumb ${index === state.path.length - 1 ? "is-current" : ""}" data-index="${index}" type="button"
        aria-current="${index === state.path.length - 1 ? "page" : "false"}">
        ${icon(index === 0 ? "home" : "folder", { fill: index === 0 })}
        <span>${escapeHtml(part.name)}</span>
        ${index < state.path.length - 1 ? icon("chevron_right") : ""}
      </button>`,
    )
    .join("");
}

function matchesFilter(item) {
  if (state.filter === "all") return true;
  if (state.filter === "folder") return item.type === "folder";
  if (item.type === "folder") return false;
  const mime = String(item.mimetype || "").toLowerCase();
  const name = String(item.name || "").toLowerCase();
  switch (state.filter) {
    case "image":
      return mime.startsWith("image/");
    case "video":
      return mime.startsWith("video/");
    case "audio":
      return mime.startsWith("audio/");
    case "doc":
      return (
        mime.includes("pdf") ||
        mime.startsWith("text/") ||
        mime.includes("word") ||
        mime.includes("document") ||
        mime.includes("spreadsheet") ||
        mime.includes("presentation") ||
        /\.(pdf|docx?|odt|rtf|xlsx?|ods|csv|pptx?|odp)$/.test(name)
      );
    case "archive":
      return /\.(zip|rar|7z|tar|gz|bz2|xz)$/.test(name);
    case "code":
      return /\.(js|mjs|ts|jsx|tsx|py|go|rs|java|css|html|json|md|sh|yml|yaml|c|cpp|h)$/.test(name);
    default:
      return true;
  }
}

function visibleItems() {
  const term = state.search.trim().toLowerCase();
  const list = state.items.filter(
    (item) => matchesFilter(item) && (!term || String(item.name || "").toLowerCase().includes(term)),
  );
  const sort = SORTS[state.sortIndex];
  return [...list].sort((a, b) => {
    if (a.type !== b.type) return a.type === "folder" ? -1 : 1;
    let av;
    let bv;
    if (sort.field === "name") {
      av = String(a.name || "").toLowerCase();
      bv = String(b.name || "").toLowerCase();
    } else {
      av = Number(a[sort.field]) || 0;
      bv = Number(b[sort.field]) || 0;
    }
    if (av < bv) return -1 * sort.direction;
    if (av > bv) return 1 * sort.direction;
    return 0;
  });
}

function highlightName(name, term) {
  const raw = String(name || "Tanpa nama");
  const query = term.trim();
  if (!query) return escapeHtml(raw);
  const index = raw.toLowerCase().indexOf(query.toLowerCase());
  if (index < 0) return escapeHtml(raw);
  return `${escapeHtml(raw.slice(0, index))}<mark>${escapeHtml(raw.slice(index, index + query.length))}</mark>${escapeHtml(
    raw.slice(index + query.length),
  )}`;
}

function renderFiles() {
  const items = visibleItems();
  const folders = state.items.filter((item) => item.type === "folder").length;
  const files = state.items.length - folders;
  const totalSize = state.items.reduce((sum, item) => sum + (Number(item.size) || 0), 0);

  els.stats.innerHTML = `
    <div class="stat" style="animation-delay:0ms">${icon("data_object")}<div><b>${state.items.length}</b><small>semua</small></div></div>
    <div class="stat folders" style="animation-delay:60ms">${icon("folder")}<div><b>${folders}</b><small>folder</small></div></div>
    <div class="stat files" style="animation-delay:120ms">${icon("description")}<div><b>${files}</b><small>file</small></div></div>
    <div class="stat" style="animation-delay:180ms">${icon("database")}<div><b>${formatBytes(totalSize)}</b><small>total</small></div></div>`;

  const filtered = state.search || state.filter !== "all";
  let sub = filtered
    ? `${items.length} dari ${state.items.length} item di folder ini`
    : `${state.items.length} item di folder ini`;
  if (state.offline) sub += " · salinan offline";
  else sub += " · tersinkron dengan GoFile";
  els.folderSub.textContent = sub;

  const activeFilter = FILTERS.find((filter) => filter.id === state.filter)?.label || "Semua";
  els.filterLabel.textContent = activeFilter;
  els.filterBtn.setAttribute("aria-label", `Saring jenis file, saat ini ${activeFilter}`);
  els.filterDot.hidden = state.filter === "all";

  if (!items.length) {
    els.fileGrid.hidden = true;
    els.empty.hidden = false;
    els.emptyRetry.hidden = true;
    if (state.loadError) {
      els.emptyTitle.textContent = "Gagal memuat folder";
      els.emptyText.textContent = state.loadError;
      els.empty.querySelector(".empty-icon").textContent = "cloud_off";
      els.emptyRetry.hidden = false;
    } else if (state.search) {
      els.emptyTitle.textContent = "Tidak ada hasil";
      els.emptyText.textContent = `Tidak ada item yang cocok dengan “${state.search}”.`;
      els.empty.querySelector(".empty-icon").textContent = "search_off";
    } else if (state.filter !== "all") {
      els.emptyTitle.textContent = "Tidak ada item sejenis";
      els.emptyText.textContent = `Folder ini tidak berisi ${(
        FILTERS.find((filter) => filter.id === state.filter)?.label || "item"
      ).toLowerCase()}.`;
      els.empty.querySelector(".empty-icon").textContent = "filter_list_off";
    } else {
      els.emptyTitle.textContent = "Folder ini kosong";
      els.emptyText.textContent = "Belum ada file atau folder di sini. Unggah file atau buat folder baru.";
      els.empty.querySelector(".empty-icon").textContent = "folder_off";
    }
    return;
  }

  els.empty.hidden = true;
  els.fileGrid.hidden = false;
  els.fileGrid.classList.toggle("list-view", state.view === "list");
  els.fileGrid.classList.toggle("is-long", items.length > 60);
  els.fileGrid.innerHTML = items
    .map((item, index) => {
      const glyph = iconFor(item);
      const isFolder = item.type === "folder";
      const detail = isFolder
        ? `Folder${item.childrenCount ? ` · ${item.childrenCount} item` : ""}`
        : `${formatBytes(item.size)} · ${formatDate(item.createTime)}`;
      const label = `${item.name || "Tanpa nama"}, ${isFolder ? "folder" : formatBytes(item.size)}`;
      return `
        <div class="file-card" data-id="${escapeHtml(item.id)}" data-type="${escapeHtml(item.type)}"
          style="--i:${Math.min(index, 20)}" role="button" tabindex="0" aria-label="${escapeHtml(label)}" aria-pressed="false">
          <span class="icon-tile ${glyph.cls}">${icon(glyph.name)}</span>
          <span class="file-meta">
            <span class="file-name">${highlightName(item.name, state.search)}</span>
            <span class="file-detail">${escapeHtml(detail)}</span>
          </span>
          <div class="card-actions">
            <button class="sel-check" type="button" tabindex="-1" data-select="${escapeHtml(item.id)}"
              aria-label="Pilih ${escapeHtml(item.name || "item")}" aria-pressed="false">${icon("check")}</button>
            <button class="more-btn" type="button" tabindex="-1" data-more="${escapeHtml(item.id)}"
              aria-label="Aksi untuk ${escapeHtml(item.name || "item")}" aria-haspopup="menu" aria-expanded="false">${icon("more_vert")}</button>
          </div>
        </div>`;
    })
    .join("");

  updateSelectionUI();
}

/* ============================ selection ============================ */

function setSelection(ids) {
  state.selected = new Set(ids);
  updateSelectionUI();
}

function clearSelection() {
  if (!state.selected.size) {
    updateSelectionUI();
    return;
  }
  state.selected.clear();
  state.anchorId = null;
  updateSelectionUI();
}

function toggleSelection(id, { range = false } = {}) {
  if (range && state.anchorId) {
    const ids = visibleItems().map((item) => item.id);
    const from = ids.indexOf(state.anchorId);
    const to = ids.indexOf(id);
    if (from >= 0 && to >= 0) {
      const [start, end] = from < to ? [from, to] : [to, from];
      ids.slice(start, end + 1).forEach((candidate) => state.selected.add(candidate));
      updateSelectionUI();
      return;
    }
  }
  if (state.selected.has(id)) {
    state.selected.delete(id);
    if (state.anchorId === id) state.anchorId = null;
  } else {
    state.selected.add(id);
    state.anchorId = id;
  }
  updateSelectionUI();
}

function updateSelectionUI() {
  $$(".file-card", els.fileGrid).forEach((card) => {
    const selected = state.selected.has(card.dataset.id);
    card.classList.toggle("is-selected", selected);
    card.setAttribute("aria-pressed", String(selected));
    const check = $(".sel-check", card);
    if (check) check.setAttribute("aria-pressed", String(selected));
  });

  const count = state.selected.size;
  document.body.classList.toggle("has-selection", count > 0);
  els.selectionBar.hidden = count === 0;
  els.selectionCount.textContent = `${count} dipilih`;
  const hasFiles = [...state.selected].some((id) => itemById(id)?.type === "file");
  els.selDownload.disabled = !hasFiles;
  els.selDownload.title = hasFiles ? "Unduh file terpilih" : "Folder tidak bisa diunduh";
}

/* ============================ menu ============================ */

function menuMarkup(items) {
  return items
    .map((item, index) => {
      if (item.separator) return '<div class="menu-sep" role="separator"></div>';
      const shortcut = item.shortcut ? `<kbd>${escapeHtml(item.shortcut)}</kbd>` : "";
      const checked = item.checked ? icon("check", { cls: "menu-check" }) : "";
      const isRadio = typeof item.checked === "boolean";
      return `<button class="menu-item ${item.danger ? "is-danger" : ""}" type="button" role="${
        isRadio ? "menuitemradio" : "menuitem"
      }" ${isRadio ? `aria-checked="${item.checked}"` : ""}
        data-index="${index}" ${item.disabled ? 'disabled aria-disabled="true"' : ""}>
        ${icon(item.icon || "chevron_right", { cls: "menu-icon" })}
        <span class="menu-label">${escapeHtml(item.label)}</span>${shortcut}${checked}
      </button>`;
    })
    .join("");
}

function openMenu(anchor, items, { point = null, label = "Menu" } = {}) {
  closeMenu(false);
  state.menuItems = items;
  els.menu.innerHTML = menuMarkup(items);
  els.menu.setAttribute("aria-label", label);
  els.menu.hidden = false;

  const rect = els.menu.getBoundingClientRect();
  let x;
  let y;
  if (point) {
    x = point.x;
    y = point.y;
  } else if (anchor) {
    const box = anchor.getBoundingClientRect();
    x = box.left;
    y = box.bottom + 6;
  } else {
    x = 12;
    y = 12;
  }
  x = Math.max(8, Math.min(x, window.innerWidth - rect.width - 8));
  y = Math.max(8, Math.min(y, window.innerHeight - rect.height - 8));
  els.menu.style.left = `${x}px`;
  els.menu.style.top = `${y}px`;

  const expander = anchor?.closest("[aria-haspopup]") || null;
  if (expander) expander.setAttribute("aria-expanded", "true");
  state.menu = { opener: anchor, expander, point };
  ($('.menu-item[aria-checked="true"]:not(:disabled)', els.menu) || $(".menu-item:not(:disabled)", els.menu))?.focus();
}

function closeMenu(restoreFocus = true) {
  if (els.menu.hidden) return;
  els.menu.hidden = true;
  els.menu.innerHTML = "";
  const opener = state.menu?.opener;
  const expander = state.menu?.expander;
  if (expander) expander.setAttribute("aria-expanded", "false");
  state.menu = null;
  state.menuItems = [];
  if (restoreFocus && opener?.isConnected && typeof opener.focus === "function") opener.focus();
}

function handleMenuKeydown(event) {
  if (els.menu.hidden) return;
  const focusables = $$(".menu-item:not(:disabled)", els.menu);
  const index = focusables.indexOf(document.activeElement);
  if (event.key === "ArrowDown") {
    event.preventDefault();
    focusables[(index + 1) % focusables.length]?.focus();
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    focusables[(index - 1 + focusables.length) % focusables.length]?.focus();
  } else if (event.key === "Home") {
    event.preventDefault();
    focusables[0]?.focus();
  } else if (event.key === "End") {
    event.preventDefault();
    focusables[focusables.length - 1]?.focus();
  } else if (event.key === "Escape") {
    event.preventDefault();
    event.stopPropagation();
    closeMenu();
  } else if (event.key === "Tab") {
    closeMenu();
  }
}

/* ============================ open / preview / details ============================ */

function isPreviewable(item) {
  if (item.type === "folder" || !item.link) return false;
  const mime = String(item.mimetype || "").toLowerCase();
  const name = String(item.name || "").toLowerCase();
  if (mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")) return true;
  if (mime.includes("pdf") || mime.startsWith("text/")) return true;
  return /\.(txt|md|json|csv|log|xml|js|mjs|css|html|yml|yaml)$/.test(name);
}

function openItem(item) {
  if (!item) return;
  if (item.type === "folder") {
    withTransition(() => loadFolder(item.id, item.name || "Folder"));
    return;
  }
  if (isPreviewable(item)) openPreview(item);
  else openDetails(item);
}

function shareUrl(item) {
  // Tautan halaman GoFile: pendek, stabil, dan bisa dipindai sebagai QR.
  // Unduhan tetap memakai tautan CDN langsung dari `item.link`.
  return `https://gofile.io/d/${encodeURIComponent(item.id)}`;
}

function openPreview(item) {
  if (!item) return;
  if (!isPreviewable(item)) {
    openDetails(item);
    return;
  }
  closeDetails();
  state.previewId = item.id;
  state.lastFocus = document.activeElement;
  const mime = String(item.mimetype || "").toLowerCase();
  const link = item.link;
  els.previewTitle.textContent = item.name || "Pratinjau";
  els.previewSub.textContent = `${formatBytes(item.size)} · ${item.mimetype || "berkas"}`;
  els.previewBody.innerHTML = `<div class="preview-loading">${icon("progress_activity", {
    cls: "preview-spinner",
  })}<span>Menyiapkan pratinjau…</span></div>`;
  els.previewActions.innerHTML = `
    <button class="btn btn-tonal btn-sm" type="button" data-preview-action="details">${icon("info")}<span>Detail</span></button>
    <button class="btn btn-tonal btn-sm" type="button" data-preview-action="share">${icon("share")}<span>Bagikan</span></button>
    <button class="btn btn-primary btn-sm" type="button" data-preview-action="download">${icon("download")}<span>Unduh</span></button>`;
  els.preview.hidden = false;
  syncLock();
  els.previewClose.focus();

  if (mime.startsWith("image/")) {
    els.previewBody.innerHTML = `<img class="preview-media" src="${escapeHtml(link)}" alt="${escapeHtml(
      item.name || "Gambar",
    )}" />`;
    const image = $(".preview-media", els.previewBody);
    image?.addEventListener("error", () => {
      els.previewBody.innerHTML = previewFallback("Gambar tidak dapat ditampilkan.");
    });
  } else if (mime.startsWith("video/")) {
    els.previewBody.innerHTML = `<video class="preview-media" controls playsinline preload="metadata" src="${escapeHtml(
      link,
    )}"></video>`;
  } else if (mime.startsWith("audio/")) {
    els.previewBody.innerHTML = `<div class="preview-audio">${icon("graphic_eq", {
      size: 48,
    })}<audio controls preload="metadata" src="${escapeHtml(link)}"></audio></div>`;
  } else if (mime.includes("pdf")) {
    els.previewBody.innerHTML = `<iframe class="preview-frame" src="${escapeHtml(link)}" title="${escapeHtml(
      item.name || "PDF",
    )}"></iframe>`;
  } else {
    renderTextPreview(item);
  }
}

function previewFallback(message) {
  return `<div class="preview-fallback">${icon("visibility_off", { size: 40 })}<p>${escapeHtml(
    message,
  )}</p><p class="preview-note">Gunakan tombol Unduh atau buka tautan langsung.</p></div>`;
}

async function renderTextPreview(item) {
  if (Number(item.size) > 1_000_000) {
    els.previewBody.innerHTML = previewFallback("File teks terlalu besar untuk dipratinjau.");
    return;
  }
  try {
    const response = await fetch(item.link, { credentials: "omit" });
    if (!response.ok) throw new Error("gagal");
    const text = await response.text();
    if (state.previewId !== item.id) return;
    els.previewBody.innerHTML = `<pre class="preview-text">${escapeHtml(text.slice(0, 120_000))}</pre>`;
  } catch {
    if (state.previewId !== item.id) return;
    els.previewBody.innerHTML = previewFallback("Pratinjau teks diblokir oleh server file.");
  }
}

function closePreview() {
  if (els.preview.hidden) return;
  els.preview.hidden = true;
  els.previewBody.innerHTML = "";
  els.previewActions.innerHTML = "";
  state.previewId = null;
  syncLock();
  if (state.lastFocus?.isConnected) state.lastFocus.focus();
}

function detailValue(value, { mono = false, copyField = "" } = {}) {
  const copy = copyField
    ? `<button class="copy-btn" type="button" data-copy-field="${copyField}" aria-label="Salin ${copyField}">${icon(
        "content_copy",
      )}</button>`
    : "";
  return `<dd class="detail-value ${mono ? "is-mono" : ""}"><span>${escapeHtml(value)}</span>${copy}</dd>`;
}

function openDetails(item) {
  if (!item) return;
  closePreview();
  closeMenu(false);
  state.detailsId = item.id;
  state.lastFocus = document.activeElement;
  const glyph = iconFor(item);
  const isFolder = item.type === "folder";
  const link = shareUrl(item);
  const rows = [
    `<div class="detail-row">${icon("badge")}${detailValue(item.name || "Tanpa nama")}<dt>Nama</dt></div>`,
    `<div class="detail-row">${icon("category")}${detailValue(isFolder ? "Folder" : item.mimetype || "Berkas")}<dt>Jenis</dt></div>`,
    isFolder
      ? `<div class="detail-row">${icon("folder_open")}${detailValue(String(item.childrenCount ?? "—"))}<dt>Isi</dt></div>`
      : `<div class="detail-row">${icon("scale")}${detailValue(formatBytes(item.size))}<dt>Ukuran</dt></div>`,
    `<div class="detail-row">${icon("schedule")}${detailValue(formatDateTime(item.createTime))}<dt>Diunggah</dt></div>`,
    isFolder
      ? ""
      : `<div class="detail-row">${icon("download")}${detailValue(`${downloadCount(item)} unduhan`)}<dt>Unduhan</dt></div>`,
    `<div class="detail-row">${icon("fingerprint")}${detailValue(item.id, { mono: true, copyField: "id" })}<dt>ID</dt></div>`,
    item.md5
      ? `<div class="detail-row">${icon("tag")}${detailValue(item.md5, { mono: true, copyField: "md5" })}<dt>MD5</dt></div>`
      : "",
    `<div class="detail-row">${icon("link")}${detailValue(link, { mono: true, copyField: "link" })}<dt>Tautan</dt></div>`,
    item.type === "file" && item.link
      ? `<div class="detail-row">${icon("download")}${detailValue(item.link, {
          mono: true,
          copyField: "direct",
        })}<dt>Tautan langsung</dt></div>`
      : "",
  ].filter(Boolean);

  els.detailsTitle.textContent = item.name || "Detail";
  els.detailsBody.innerHTML = `
    <div class="detail-hero">
      <span class="icon-tile ${glyph.cls}">${icon(glyph.name)}</span>
      <div class="detail-hero-copy">
        <strong>${escapeHtml(item.name || "Tanpa nama")}</strong>
        <span>${escapeHtml(isFolder ? "Folder" : formatBytes(item.size))} · ${
          isFolder ? escapeHtml(String(item.childrenCount ?? 0)) + " item" : escapeHtml(formatDate(item.createTime))
        }</span>
      </div>
    </div>
    <dl class="detail-list">${rows.join("")}</dl>
    <div class="detail-actions">
      ${
        isFolder
          ? `<button class="btn btn-primary btn-sm" type="button" data-detail-action="open">${icon(
              "folder_open",
            )}<span>Buka folder</span></button>`
          : `<button class="btn btn-primary btn-sm" type="button" data-detail-action="download">${icon(
              "download",
            )}<span>Unduh</span></button>`
      }
      <button class="btn btn-tonal btn-sm" type="button" data-detail-action="share">${icon("share")}<span>Bagikan</span></button>
      <button class="btn btn-tonal btn-sm" type="button" data-detail-action="rename">${icon("edit")}<span>Ganti nama</span></button>
      <button class="btn btn-tonal btn-sm" type="button" data-detail-action="external">${icon(
        "open_in_new",
      )}<span>Buka di GoFile</span></button>
    </div>`;
  els.details.hidden = false;
  syncLock();
  els.detailsClose.focus();
}

function closeDetails() {
  if (els.details.hidden) return;
  els.details.hidden = true;
  els.detailsBody.innerHTML = "";
  state.detailsId = null;
  syncLock();
  if (state.lastFocus?.isConnected) state.lastFocus.focus();
}

function syncLock() {
  document.body.classList.toggle("is-locked", !els.details.hidden || !els.preview.hidden);
}

/* ============================ downloads & share ============================ */

function downloadItem(item) {
  if (!item) return;
  if (item.type === "folder") {
    toast("GoFile tidak menyediakan unduhan folder (zip). Buka folder lalu unduh isinya.", { error: true });
    return;
  }
  if (!item.link) {
    toast("Tautan unduhan belum tersedia untuk file ini.", { error: true });
    return;
  }
  const anchor = document.createElement("a");
  anchor.href = item.link;
  anchor.download = item.name || "berkas";
  anchor.rel = "noopener";
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  toast(`Mengunduh ${item.name || "berkas"}…`);
}

function downloadMany(items) {
  const files = items.filter((item) => item.type === "file" && item.link);
  if (!files.length) {
    toast("Tidak ada file yang bisa diunduh.", { error: true });
    return;
  }
  toast(`Mengunduh ${files.length} file…`);
  files.forEach((item, index) => setTimeout(() => downloadItem(item), index * 350));
}

function openShare(item) {
  if (!item) return;
  const url = shareUrl(item);
  const canShare = typeof navigator.share === "function";
  let qr = "";
  try {
    qr = qrSvg(url, { size: 212 });
  } catch {
    qr = "";
  }
  openModal({
    title: "Bagikan",
    subtitle: item.name || "Item",
    body: `
      <div class="share">
        ${
          qr
            ? `<div class="share-qr">${qr}</div>`
            : `<p class="share-note">QR tidak tersedia untuk tautan sepanjang ini.</p>`
        }
        <p class="share-url">${escapeHtml(url)}</p>
        <div class="share-actions">
          <button class="btn btn-tonal btn-sm" type="button" data-share="copy">${icon("content_copy")}<span>Salin tautan</span></button>
          ${
            canShare
              ? `<button class="btn btn-primary btn-sm" type="button" data-share="native">${icon("share")}<span>Bagikan…</span></button>`
              : `<button class="btn btn-primary btn-sm" type="button" data-share="open">${icon("open_in_new")}<span>Buka tautan</span></button>`
          }
        </div>
        <p class="share-hint">${icon("lock")} Siapa pun yang punya tautan ini dapat membuka item.</p>
      </div>`,
    actions: [{ label: "Tutup" }],
    onMount: (dialog) => {
      $$("[data-share]", dialog).forEach((button) => {
        button.addEventListener("click", async () => {
          const mode = button.dataset.share;
          if (mode === "copy") {
            const ok = await copyText(url);
            toast(ok ? "Tautan disalin." : "Gagal menyalin tautan.", { error: !ok });
          } else if (mode === "native") {
            try {
              await navigator.share({ title: item.name || "GoFile", text: item.name || "", url });
            } catch {
              /* pengguna membatalkan — bukan error */
            }
          } else {
            openExternal(url);
          }
        });
      });
    },
  });
}

/* ============================ modal ============================ */

function openModal({ title, subtitle = "", body = "", actions = [], onMount = null, onClose = null }) {
  const dialog = els.modal;
  if (dialog.open) dialog.close();
  dialog.innerHTML = `
    <div class="modal-card">
      <header class="modal-head">
        <div>
          <h2 id="modal-title">${escapeHtml(title)}</h2>
          ${subtitle ? `<p class="modal-sub">${escapeHtml(subtitle)}</p>` : ""}
        </div>
        <button class="icon-btn modal-close" type="button" data-modal-close aria-label="Tutup">${icon("close")}</button>
      </header>
      <div class="modal-body">${body}</div>
      ${
        actions.length
          ? `<footer class="modal-actions">${actions
              .map(
                (action, index) =>
                  `<button class="btn ${
                    action.variant === "danger" ? "btn-danger" : action.variant === "primary" ? "btn-primary" : "btn-tonal"
                  }" type="button" data-modal-action="${index}" ${action.disabled ? "disabled" : ""}>${
                    action.icon ? icon(action.icon) : ""
                  }<span>${escapeHtml(action.label)}</span></button>`,
              )
              .join("")}</footer>`
          : ""
      }
    </div>`;
  dialog.onclose = () => onClose?.();
  dialog.querySelector("[data-modal-close]")?.addEventListener("click", () => dialog.close());
  $$("[data-modal-action]", dialog).forEach((button) => {
    button.addEventListener("click", async () => {
      const action = actions[Number(button.dataset.modalAction)];
      if (!action) return;
      let keepOpen = false;
      if (action.onClick) {
        try {
          keepOpen = (await action.onClick(dialog, button)) === false;
        } catch (error) {
          toast(humanError(error), { error: true });
          keepOpen = true;
        }
      }
      if (!keepOpen) dialog.close();
    });
  });
  dialog.showModal();
  onMount?.(dialog);
  return dialog;
}

function showFieldError(dialog, message) {
  const error = dialog.querySelector("#modal-error");
  if (!error) return;
  error.textContent = message;
  error.hidden = !message;
}

function openPrompt({
  title,
  subtitle = "",
  label,
  value = "",
  placeholder = "",
  confirmLabel = "Simpan",
  description = "",
  iconName = "edit",
  onSubmit,
}) {
  openModal({
    title,
    subtitle,
    body: `
      <form class="modal-form" id="modal-form" novalidate>
        <label class="field-label" for="modal-input">${escapeHtml(label)}</label>
        <div class="field">
          <span class="ms field-icon" aria-hidden="true">${iconName}</span>
          <input id="modal-input" type="text" autocomplete="off" spellcheck="false"
            value="${escapeHtml(value)}" placeholder="${escapeHtml(placeholder)}" />
        </div>
        <p class="field-error" id="modal-error" role="alert" hidden></p>
        ${description ? `<p class="modal-note">${description}</p>` : ""}
      </form>`,
    actions: [
      { label: "Batal" },
      {
        label: confirmLabel,
        variant: "primary",
        onClick: async (dialog, button) => {
          const input = dialog.querySelector("#modal-input");
          const next = input.value.trim();
          if (!next) {
            showFieldError(dialog, "Nama tidak boleh kosong.");
            input.focus();
            return false;
          }
          showFieldError(dialog, "");
          const original = button.innerHTML;
          button.disabled = true;
          button.innerHTML = `${icon("progress_activity")}<span>Menyimpan…</span>`;
          try {
            await onSubmit(next, dialog);
            return true;
          } catch (error) {
            showFieldError(dialog, humanError(error));
            return false;
          } finally {
            button.disabled = false;
            button.innerHTML = original;
          }
        },
      },
    ],
    onMount: (dialog) => {
      const input = dialog.querySelector("#modal-input");
      input.addEventListener("input", () => showFieldError(dialog, ""));
      input.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
          event.preventDefault();
          dialog.querySelector('[data-modal-action="1"]')?.click();
        }
      });
      input.focus();
      input.select();
    },
  });
}

function openConfirm({ title, subtitle = "", message, confirmLabel = "Hapus permanen", danger = true, onConfirm }) {
  openModal({
    title,
    subtitle,
    body: `<p class="confirm-text">${message}</p>`,
    actions: [
      { label: "Batal" },
      {
        label: confirmLabel,
        variant: danger ? "danger" : "primary",
        onClick: async (dialog, button) => {
          const original = button.innerHTML;
          button.disabled = true;
          button.innerHTML = `${icon("progress_activity")}<span>Memproses…</span>`;
          try {
            await onConfirm();
            return true;
          } catch (error) {
            toast(humanError(error), { error: true });
            return false;
          } finally {
            button.disabled = false;
            button.innerHTML = original;
          }
        },
      },
    ],
  });
}

/* ============================ mutations ============================ */

function currentToken() {
  return state.session?.token || "";
}

function newFolder() {
  const folder = currentFolder();
  if (!folder) return;
  openPrompt({
    title: "Folder baru",
    subtitle: `Di dalam ${folder.name}`,
    label: "Nama folder",
    placeholder: "mis. Proyek 2026",
    confirmLabel: "Buat folder",
    iconName: "create_new_folder",
    onSubmit: async (name) => {
      await createFolder(currentToken(), folder.id, name);
      deleteFolderCacheEntry(folder.id);
      await reloadCurrent();
      toast(`Folder “${name}” dibuat.`);
    },
  });
}

function openRename(item) {
  if (!item) return;
  openPrompt({
    title: "Ganti nama",
    subtitle: item.type === "folder" ? "Folder" : "File",
    label: "Nama baru",
    value: item.name || "",
    confirmLabel: "Simpan",
    onSubmit: async (name) => {
      if (name === item.name) return;
      await renameContent(currentToken(), item.id, name);
      deleteFolderCacheEntry(currentFolder()?.id);
      await reloadCurrent();
      toast("Nama diperbarui.");
    },
  });
}

function confirmDelete(ids = [...state.selected]) {
  const items = ids.map(itemById).filter(Boolean);
  if (!items.length) return;
  const names = items
    .slice(0, 5)
    .map((item) => `<li>${escapeHtml(item.name || "Tanpa nama")}</li>`)
    .join("");
  const more = items.length > 5 ? `<li class="muted">+ ${items.length - 5} item lain</li>` : "";
  openConfirm({
    title: items.length === 1 ? "Hapus item ini?" : `Hapus ${items.length} item?`,
    subtitle: items.length === 1 ? items[0].name : "",
    message: `<span class="confirm-lead">Item berikut akan dihapus permanen:</span><ul class="confirm-list">${names}${more}</ul>
      <p class="modal-note">${icon("warning")} GoFile tidak punya recycle bin. Folder akan terhapus beserta seluruh isinya.</p>`,
    confirmLabel: "Hapus permanen",
    onConfirm: async () => {
      await deleteContents(currentToken(), items.map((item) => item.id));
      items.forEach((item) => deleteFolderCacheEntry(item.id));
      clearSelection();
      await reloadCurrent();
      toast(items.length === 1 ? "Item dihapus." : `${items.length} item dihapus.`);
    },
  });
}

function openTransferDialog(mode, ids) {
  const items = ids.map(itemById).filter(Boolean);
  if (!items.length) return;
  const folder = currentFolder();
  if (!folder) return;
  state.picker = {
    mode,
    ids: items.map((item) => item.id),
    items,
    blocked: new Set(items.filter((item) => item.type === "folder").map((item) => item.id)),
    path: [{ id: state.session.account.rootFolder, name: "Root" }],
    current: state.session.account.rootFolder,
  };

  const verb = mode === "move" ? "Pindahkan" : "Salin";
  openModal({
    title: mode === "move" ? "Pindahkan ke…" : "Salin ke…",
    subtitle: items.length === 1 ? items[0].name : `${items.length} item`,
    body: `<div class="picker">
      <nav class="picker-crumbs" id="picker-crumbs" aria-label="Lokasi tujuan"></nav>
      <div class="picker-list" id="picker-list"></div>
      <p class="picker-status" id="picker-status" role="status">Memuat…</p>
    </div>`,
    actions: [
      { label: "Batal" },
      {
        label: verb,
        variant: "primary",
        onClick: async (dialog, button) => {
          const picker = state.picker;
          if (!picker) return false;
          if (picker.blocked.has(picker.current)) {
            toast("Folder tidak bisa dipindahkan ke dalam dirinya sendiri.", { error: true });
            return false;
          }
          if (mode === "move" && picker.items.every((item) => item.parentFolder === picker.current)) {
            toast("Item sudah ada di folder ini.");
            return false;
          }
          const original = button.innerHTML;
          button.disabled = true;
          button.innerHTML = `${icon("progress_activity")}<span>Memproses…</span>`;
          try {
            if (mode === "move") await moveContents(currentToken(), picker.ids, picker.current);
            else await copyContents(currentToken(), picker.ids, picker.current);
            deleteFolderCacheEntry(picker.current);
            deleteFolderCacheEntry(folder.id);
            await reloadCurrent();
            toast(mode === "move" ? "Item dipindahkan." : "Item disalin.");
            return true;
          } catch (error) {
            toast(humanError(error), { error: true });
            return false;
          } finally {
            button.disabled = false;
            button.innerHTML = original;
          }
        },
      },
    ],
    onMount: (dialog) => {
      const list = dialog.querySelector("#picker-list");
      list.addEventListener("click", (event) => {
        const row = event.target.closest("[data-folder-id]");
        if (!row) return;
        loadPickerFolder(row.dataset.folderId, row.dataset.folderName, true);
      });
      dialog.querySelector("#picker-crumbs").addEventListener("click", (event) => {
        const crumb = event.target.closest("[data-picker-index]");
        if (!crumb) return;
        const picker = state.picker;
        if (!picker) return;
        const index = Number(crumb.dataset.pickerIndex);
        const target = picker.path[index];
        if (target) loadPickerFolder(target.id, target.name, false, index);
      });
      loadPickerFolder(state.session.account.rootFolder, "Root", false);
    },
    onClose: () => {
      state.picker = null;
    },
  });
}

async function loadPickerFolder(id, name, push, sliceAt = null) {
  const picker = state.picker;
  if (!picker) return;
  if (sliceAt !== null) picker.path = picker.path.slice(0, sliceAt + 1);
  else if (push) picker.path.push({ id, name });
  picker.current = id;
  renderPickerCrumbs();
  const list = $("#picker-list");
  const status = $("#picker-status");
  if (!list || !status) return;
  list.innerHTML = `<div class="picker-skeleton"><span></span><span></span><span></span></div>`;
  status.textContent = "Memuat…";
  const confirm = $("#modal [data-modal-action='1']");
  if (confirm) confirm.disabled = picker.blocked.has(id);
  try {
    const data = await getFolder(currentToken(), id, { sortField: "name", sortDirection: 1 });
    if (state.picker !== picker) return;
    const folders = Object.entries(data?.children || {})
      .map(([id, child]) => ({ ...child, id, type: String(child?.type || "").toLowerCase() }))
      .filter((child) => child.type === "folder")
      .sort((a, b) => String(a.name).localeCompare(String(b.name), "id", { sensitivity: "base" }));
    if (!folders.length) {
      list.innerHTML = `<p class="picker-empty">${icon("folder_off")} Tidak ada subfolder di sini.</p>`;
    } else {
      list.innerHTML = folders
        .map(
          (child) => `<button class="picker-row" type="button" data-folder-id="${escapeHtml(
            child.id,
          )}" data-folder-name="${escapeHtml(child.name || "Folder")}" ${
            picker.blocked.has(child.id) ? 'disabled aria-disabled="true"' : ""
          }>
            ${icon("folder", { cls: "picker-folder" })}
            <span>${escapeHtml(child.name || "Folder")}</span>
            ${picker.blocked.has(child.id) ? `<em>tujuan tidak valid</em>` : icon("chevron_right")}
          </button>`,
        )
        .join("");
    }
    status.textContent = `Tujuan: ${picker.path.map((part) => part.name).join(" / ")}`;
  } catch (error) {
    if (state.picker !== picker) return;
    list.innerHTML = "";
    status.textContent = humanError(error);
  }
  if (confirm) confirm.disabled = picker.blocked.has(id);
}

function renderPickerCrumbs() {
  const picker = state.picker;
  const crumbs = $("#picker-crumbs");
  if (!picker || !crumbs) return;
  crumbs.innerHTML = picker.path
    .map(
      (part, index) => `<button class="picker-crumb ${
        index === picker.path.length - 1 ? "is-current" : ""
      }" type="button" data-picker-index="${index}">${escapeHtml(part.name)}</button>`,
    )
    .join(`<span class="picker-sep">/</span>`);
}

/* ============================ transfers & upload ============================ */

function renderTransfers() {
  const list = Object.values(state.transfers).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
  els.transferPanel.hidden = list.length === 0;
  if (!list.length) {
    els.transferPanel.innerHTML = "";
    return;
  }
  els.transferPanel.innerHTML = `
    <div class="transfer-head">${icon("swap_vert")} Transfer<span>${list.length}</span></div>
    ${list
      .map((item) => {
        const cls = item.state === "error" ? "is-error" : item.state === "success" ? "is-success" : "";
        const progress = Math.max(0, Math.min(100, Number(item.progress) || 0));
        const caption =
          item.state === "success"
            ? "Selesai"
            : item.state === "error"
              ? item.message || "Gagal"
              : `${progress}% · ${item.message || "Berjalan"}`;
        return `
          <div class="transfer-row ${cls}">
            ${icon(item.state === "success" ? "check_circle" : item.state === "error" ? "error" : "upload", {
              fill: true,
            })}
            <div class="transfer-body">
              <div class="transfer-name">${escapeHtml(item.fileName || "berkas")}</div>
              <div class="progress"><i style="width:${progress}%"></i></div>
              <small>${escapeHtml(caption)}</small>
            </div>
            ${
              item.state === "running"
                ? `<button class="transfer-cancel" type="button" data-cancel="${escapeHtml(
                    item.id,
                  )}" aria-label="Batalkan unggahan ${escapeHtml(item.fileName || "")}">${icon("close")}</button>`
                : ""
            }
          </div>`;
      })
      .join("")}`;
}

function scheduleTransferCleanup(id) {
  setTimeout(() => {
    delete state.transfers[id];
    renderTransfers();
  }, 6000);
}

function uploadOne(file, folderId) {
  const id = `up-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  state.transfers[id] = {
    id,
    type: "upload",
    fileName: file.name,
    progress: 0,
    state: "running",
    message: "Menyiapkan…",
    createdAt: Date.now(),
  };
  renderTransfers();

  return new Promise((resolve) => {
    uploadFile({
      file,
      token: currentToken(),
      folderId,
      onProgress: (progress) => {
        const transfer = state.transfers[id];
        if (!transfer) return;
        transfer.progress = progress;
        transfer.message = progress >= 100 ? "Menyelesaikan…" : "Mengunggah";
        renderTransfers();
      },
      onStart: (abort) => {
        state.uploads[id] = { abort };
      },
    })
      .then(() => {
        const transfer = state.transfers[id];
        if (transfer) {
          transfer.state = "success";
          transfer.progress = 100;
          transfer.message = "Selesai";
        }
        resolve(true);
      })
      .catch((error) => {
        const transfer = state.transfers[id];
        if (transfer) {
          transfer.state = "error";
          transfer.message = humanError(error);
        }
        resolve(false);
      })
      .finally(() => {
        delete state.uploads[id];
        renderTransfers();
        scheduleTransferCleanup(id);
      });
  });
}

async function handleFiles(fileList, folderId, folderName) {
  const files = [...fileList].filter((file) => file && file.name);
  if (!files.length || !state.session) return;
  const destination = folderId || currentFolder()?.id;
  if (!destination) return;
  deleteFolderCacheEntry(destination);
  toast(`Mengunggah ${files.length} file ke ${folderName || "folder ini"}…`);

  let cursor = 0;
  let success = 0;
  let failed = 0;
  const worker = async () => {
    while (cursor < files.length) {
      const file = files[cursor];
      cursor += 1;
      const ok = await uploadOne(file, destination);
      if (ok) success += 1;
      else failed += 1;
    }
  };
  await Promise.all(Array.from({ length: Math.min(3, files.length) }, worker));

  if (currentFolder()?.id === destination) await reloadCurrent();
  if (failed) toast(`${success} file terunggah, ${failed} gagal.`, { error: true });
  else toast(success === 1 ? "File berhasil diunggah." : `${success} file berhasil diunggah.`);
}

/* ============================ drag & drop ============================ */

let dragDepth = 0;
let longPressTimer = null;
let longPressFired = false;
let longPressStart = null;

function hasFiles(event) {
  return [...(event.dataTransfer?.types || [])].includes("Files");
}

function dropDestinationFromPoint(x, y) {
  const node = document.elementFromPoint(x, y);
  const card = node?.closest?.(".file-card[data-type='folder']");
  if (!card) return null;
  return itemById(card.dataset.id);
}

function setDropTarget(item) {
  state.dropTargetId = item?.id || null;
  $$(".file-card", els.fileGrid).forEach((card) => {
    card.classList.toggle("is-drop-target", card.dataset.id === state.dropTargetId);
  });
  els.dropFolder.textContent = item?.name || currentFolder()?.name || "folder ini";
  els.dropHint.textContent = item ? `Folder tujuan: ${item.name}` : "Lepas untuk mengunggah ke folder ini";
}

function showDropOverlay() {
  els.dropOverlay.hidden = false;
  setDropTarget(null);
}

function hideDropOverlay() {
  els.dropOverlay.hidden = true;
  state.dropTargetId = null;
  $$(".file-card.is-drop-target", els.fileGrid).forEach((card) => card.classList.remove("is-drop-target"));
}

function handleDrop(event) {
  if (!hasFiles(event)) return;
  event.preventDefault();
  dragDepth = 0;
  const item = dropDestinationFromPoint(event.clientX, event.clientY);
  hideDropOverlay();
  const folder = item || currentFolder();
  if (!folder) return;
  handleFiles(event.dataTransfer.files, folder.id, folder.name);
}

/* ============================ wiring ============================ */

function openItemMenu(item, ids, point, anchor) {
  const multi = ids.length > 1;
  const files = ids.map(itemById).filter((entry) => entry?.type === "file");
  const canPreview = isPreviewable(item);
  const openLabel = multi
    ? `Buka ${item.name}`
    : item.type === "folder"
      ? "Buka folder"
      : canPreview
        ? "Pratinjau"
        : "Buka di GoFile";
  openMenu(
    anchor,
    [
      {
        label: openLabel,
        icon: item.type === "folder" ? "folder_open" : canPreview ? "visibility" : "open_in_new",
        shortcut: item.type === "folder" || canPreview ? "Enter" : "",
        onSelect: () => {
          if (!multi && item.type === "file" && !canPreview) {
            openExternal(`https://gofile.io/d/${encodeURIComponent(item.id)}`);
          } else {
            openItem(item);
          }
        },
      },
      {
        label: multi ? `Unduh ${files.length} file` : "Unduh",
        icon: "download",
        disabled: !files.length,
        onSelect: () => downloadMany(files),
      },
      {
        label: state.selected.has(item.id) ? "Batalkan pilihan" : "Pilih item",
        icon: "check_circle",
        disabled: multi,
        onSelect: () => toggleSelection(item.id),
      },
      { separator: true },
      {
        label: "Ganti nama",
        icon: "edit",
        shortcut: "F2",
        disabled: multi,
        onSelect: () => openRename(item),
      },
      { label: "Pindahkan…", icon: "drive_file_move", onSelect: () => openTransferDialog("move", ids) },
      { label: "Salin…", icon: "content_copy", onSelect: () => openTransferDialog("copy", ids) },
      { label: "Bagikan…", icon: "share", disabled: multi, onSelect: () => openShare(item) },
      { label: "Detail", icon: "info", disabled: multi, onSelect: () => openDetails(item) },
      { separator: true },
      { label: multi ? `Hapus ${ids.length} item` : "Hapus", icon: "delete", shortcut: "Del", danger: true, onSelect: () => confirmDelete(ids) },
    ],
    { point, label: "Aksi item" },
  );
}

function openSortMenu() {
  openMenu(
    els.sortBtn,
    SORTS.map((sort, index) => ({
      label: sort.label,
      icon: sort.icon,
      checked: index === state.sortIndex,
      onSelect: () => {
        state.sortIndex = index;
        els.sortLabel.textContent = sort.label;
        els.sortBtn.setAttribute("aria-label", `Urutkan, saat ini ${sort.label}`);
        const folder = currentFolder();
        if (folder) loadFolder(folder.id, folder.name);
      },
    })),
    { label: "Urutkan" },
  );
}

function openFilterMenu() {
  openMenu(
    els.filterBtn,
    FILTERS.map((filter) => ({
      label: filter.label,
      icon: filter.icon,
      checked: filter.id === state.filter,
      onSelect: () => {
        state.filter = filter.id;
        clearSelection();
        renderFiles();
      },
    })),
    { label: "Saring jenis file" },
  );
}

function focusCard(offset) {
  const cards = $$(".file-card", els.fileGrid);
  if (!cards.length) return;
  const index = cards.indexOf(document.activeElement);
  const next = cards[Math.max(0, Math.min(cards.length - 1, (index < 0 ? 0 : index) + offset))];
  next?.focus();
  next?.scrollIntoView({ block: "nearest", behavior: "smooth" });
}

function gridColumns() {
  const columns = getComputedStyle(els.fileGrid).gridTemplateColumns;
  return Math.max(1, columns.split(" ").filter(Boolean).length);
}

function handleGlobalKeydown(event) {
  if (els.modal.open) return;

  if (event.key === "Escape") {
    if (!els.dropOverlay.hidden) {
      event.preventDefault();
      hideDropOverlay();
      return;
    }
    if (!els.menu.hidden) {
      event.preventDefault();
      closeMenu();
      return;
    }
    if (!els.preview.hidden) {
      event.preventDefault();
      closePreview();
      return;
    }
    if (!els.details.hidden) {
      event.preventDefault();
      closeDetails();
      return;
    }
    if (state.selected.size) {
      event.preventDefault();
      clearSelection();
      return;
    }
    if (state.search) {
      event.preventDefault();
      state.search = "";
      els.search.value = "";
      renderFiles();
      return;
    }
    if (document.activeElement === els.search) els.search.blur();
    return;
  }

  if (isTypingTarget(document.activeElement)) return;
  if (els.manager.hidden) return;
  if (!els.menu.hidden) return;
  if (!els.preview.hidden || !els.details.hidden) return;

  if (event.key === "/") {
    event.preventDefault();
    els.search.focus();
    els.search.select();
    return;
  }

  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "a") {
    event.preventDefault();
    setSelection(visibleItems().map((item) => item.id));
    return;
  }

  if (event.key === "Delete" || event.key === "Backspace") {
    const card = document.activeElement?.closest?.(".file-card");
    const ids = state.selected.size ? [...state.selected] : card ? [card.dataset.id] : [];
    if (ids.length) {
      event.preventDefault();
      confirmDelete(ids);
    }
    return;
  }

  if (event.key === "F2") {
    const item =
      state.selected.size === 1 ? itemById([...state.selected][0]) : itemById(document.activeElement?.closest?.(".file-card")?.dataset.id);
    if (item) {
      event.preventDefault();
      openRename(item);
    }
  }
}

function wireGrid() {
  els.fileGrid.addEventListener("click", (event) => {
    if (longPressFired) {
      longPressFired = false;
      return;
    }
    const card = event.target.closest(".file-card");
    if (!card) return;
    const item = itemById(card.dataset.id);
    if (!item) return;

    const selectButton = event.target.closest(".sel-check");
    if (selectButton) {
      event.stopPropagation();
      toggleSelection(item.id, { range: event.shiftKey });
      return;
    }

    const moreButton = event.target.closest(".more-btn");
    if (moreButton) {
      event.stopPropagation();
      if (!state.selected.has(item.id)) setSelection([item.id]);
      openItemMenu(item, [...state.selected], null, moreButton);
      return;
    }

    if (event.metaKey || event.ctrlKey || event.shiftKey) {
      event.preventDefault();
      toggleSelection(item.id, { range: event.shiftKey });
      return;
    }

    clearSelection();
    openItem(item);
  });

  els.fileGrid.addEventListener("contextmenu", (event) => {
    const card = event.target.closest(".file-card");
    if (!card) return;
    event.preventDefault();
    const item = itemById(card.dataset.id);
    if (!item) return;
    if (!state.selected.has(item.id)) setSelection([item.id]);
    else state.anchorId = item.id;
    openItemMenu(item, [...state.selected], { x: event.clientX, y: event.clientY }, card);
  });

  els.fileGrid.addEventListener("keydown", (event) => {
    const card = event.target.closest(".file-card");
    if (!card) return;
    const item = itemById(card.dataset.id);
    if (!item) return;
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      openItem(item);
    } else if (event.key === "ContextMenu" || (event.shiftKey && event.key === "F10")) {
      event.preventDefault();
      if (!state.selected.has(item.id)) setSelection([item.id]);
      const box = card.getBoundingClientRect();
      openItemMenu(item, [...state.selected], { x: box.left + 24, y: box.top + 24 }, card);
    } else if (event.key === "ArrowRight") {
      event.preventDefault();
      focusCard(1);
    } else if (event.key === "ArrowLeft") {
      event.preventDefault();
      focusCard(-1);
    } else if (event.key === "ArrowDown") {
      event.preventDefault();
      focusCard(gridColumns());
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      focusCard(-gridColumns());
    }
  });

  els.fileGrid.addEventListener("pointerdown", (event) => {
    if (event.pointerType !== "touch") return;
    const card = event.target.closest(".file-card");
    if (!card) return;
    if (event.target.closest(".sel-check, .more-btn")) return;
    longPressFired = false;
    longPressStart = { x: event.clientX, y: event.clientY };
    clearTimeout(longPressTimer);
    longPressTimer = setTimeout(() => {
      const item = itemById(card.dataset.id);
      if (!item) return;
      longPressFired = true;
      if (!state.selected.has(item.id)) setSelection([item.id]);
      openItemMenu(item, [...state.selected], { x: event.clientX, y: event.clientY }, card);
    }, 550);
  });

  ["pointerup", "pointercancel"].forEach((type) =>
    els.fileGrid.addEventListener(type, () => {
      clearTimeout(longPressTimer);
      longPressStart = null;
    }),
  );
  els.fileGrid.addEventListener("pointermove", (event) => {
    if (!longPressStart) return;
    if (Math.hypot(event.clientX - longPressStart.x, event.clientY - longPressStart.y) > 9) {
      clearTimeout(longPressTimer);
      longPressStart = null;
    }
  });
}

function wireManager() {
  els.formEmail.addEventListener("submit", (event) => {
    event.preventDefault();
    requestLink();
  });
  els.formLink.addEventListener("submit", loginWithLink);

  $("#resend-btn").addEventListener("click", async () => {
    if (!state.pendingEmail) return;
    setBusy(true);
    try {
      await sendLoginLink(state.pendingEmail);
      toast("Login link baru sudah dikirim.");
    } catch (error) {
      authError(humanError(error));
    } finally {
      setBusy(false);
    }
  });

  $("#change-email-btn").addEventListener("click", () => setStep(1));

  $("#paste-btn").addEventListener("click", async () => {
    try {
      const text = await navigator.clipboard.readText();
      els.loginLink.value = text;
      els.loginLink.focus();
      toast("Link ditempel dari clipboard.");
    } catch {
      toast("Clipboard tidak diizinkan. Tempel manual ya.", { error: true });
    }
  });

  els.syncBtn.addEventListener("click", async () => {
    const folder = currentFolder();
    if (!folder) return;
    els.syncBtn.classList.add("syncing");
    await loadFolder(folder.id, folder.name);
    els.syncBtn.classList.remove("syncing");
    toast("Folder tersinkron ulang.");
  });

  els.logoutBtn.addEventListener("click", () => signOut());
  els.emptyRetry.addEventListener("click", () => reloadCurrent());

  els.search.addEventListener("input", (event) => {
    state.search = event.target.value;
    clearSelection();
    renderFiles();
  });
  els.search.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      $(".file-card", els.fileGrid)?.focus();
    }
  });

  $$(".seg").forEach((button) =>
    button.addEventListener("click", () => {
      state.view = button.dataset.view;
      $$(".seg").forEach((candidate) => {
        candidate.classList.toggle("is-active", candidate === button);
        candidate.setAttribute("aria-pressed", String(candidate === button));
      });
      smoothUpdate(renderFiles);
    }),
  );

  const onScroll = () => els.topbar?.classList.toggle("is-condensed", window.scrollY > 40);
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  els.sortBtn.addEventListener("click", () => {
    if (state.menu?.opener === els.sortBtn) closeMenu();
    else openSortMenu();
  });
  els.filterBtn.addEventListener("click", () => {
    if (state.menu?.opener === els.filterBtn) closeMenu();
    else openFilterMenu();
  });

  els.uploadBtn.addEventListener("click", () => {
    if (!state.session) return;
    els.fileInput.click();
  });
  els.fileInput.addEventListener("change", () => {
    const folder = currentFolder();
    if (!folder) return;
    const files = els.fileInput.files;
    if (files?.length) handleFiles(files, folder.id, folder.name);
    els.fileInput.value = "";
  });
  els.newFolderBtn.addEventListener("click", newFolder);

  els.breadcrumbs.addEventListener("click", (event) => {
    const crumb = event.target.closest(".crumb");
    if (!crumb) return;
    const part = state.path[Number(crumb.dataset.index)];
    if (part) withTransition(() => loadFolder(part.id, part.name));
  });

  els.selectionBar.addEventListener("click", (event) => {
    const button = event.target.closest("button[id]");
    if (!button) return;
    const ids = [...state.selected];
    if (button.id === "sel-download") downloadMany(ids.map(itemById).filter(Boolean));
    else if (button.id === "sel-move") openTransferDialog("move", ids);
    else if (button.id === "sel-copy") openTransferDialog("copy", ids);
    else if (button.id === "sel-delete") confirmDelete(ids);
    else if (button.id === "sel-clear") clearSelection();
  });

  els.menu.addEventListener("click", (event) => {
    const button = event.target.closest(".menu-item");
    if (!button || button.disabled) return;
    const item = state.menuItems[Number(button.dataset.index)];
    if (!item) return;
    const opener = state.menu?.opener;
    closeMenu(false);
    if (opener?.isConnected) opener.focus?.();
    item.onSelect?.();
  });
  els.menu.addEventListener("keydown", handleMenuKeydown);

  els.detailsClose.addEventListener("click", closeDetails);
  els.detailsBody.addEventListener("click", async (event) => {
    const item = itemById(state.detailsId);
    if (!item) return;
    const copy = event.target.closest("[data-copy-field]");
    if (copy) {
      const field = copy.dataset.copyField;
      const value =
        field === "id" ? item.id : field === "md5" ? item.md5 : field === "direct" ? item.link : shareUrl(item);
      const ok = await copyText(value || "");
      toast(ok ? "Disalin ke clipboard." : "Gagal menyalin.", { error: !ok });
      return;
    }
    const action = event.target.closest("[data-detail-action]")?.dataset.detailAction;
    if (!action) return;
    if (action === "download") downloadItem(item);
    else if (action === "share") openShare(item);
    else if (action === "rename") openRename(item);
    else if (action === "open") {
      withTransition(() => loadFolder(item.id, item.name || "Folder"));
    } else if (action === "external") openExternal(`https://gofile.io/d/${encodeURIComponent(item.id)}`);
  });

  els.previewClose.addEventListener("click", closePreview);
  els.preview.addEventListener("click", (event) => {
    if (event.target === els.preview) closePreview();
  });
  els.preview.addEventListener("keydown", (event) => {
    if (event.key !== "Tab") return;
    const focusables = $$(
      'button:not(:disabled), [href], audio[controls], video[controls], iframe, [tabindex]:not([tabindex="-1"])',
      els.preview,
    ).filter((node) => node.offsetParent !== null);
    if (!focusables.length) return;
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  });
  els.previewActions.addEventListener("click", (event) => {
    const action = event.target.closest("[data-preview-action]")?.dataset.previewAction;
    const item = itemById(state.previewId);
    if (!action || !item) return;
    if (action === "download") downloadItem(item);
    else if (action === "share") openShare(item);
    else if (action === "details") openDetails(item);
  });

  els.transferPanel.addEventListener("click", (event) => {
    const button = event.target.closest("[data-cancel]");
    if (!button) return;
    state.uploads[button.dataset.cancel]?.abort?.();
  });

  els.modal.addEventListener("click", (event) => {
    if (event.target === els.modal) els.modal.close();
  });

  window.addEventListener("dragenter", (event) => {
    if (!hasFiles(event) || els.manager.hidden) return;
    event.preventDefault();
    dragDepth += 1;
    showDropOverlay();
  });
  window.addEventListener("dragover", (event) => {
    if (!hasFiles(event) || els.manager.hidden) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = "copy";
    setDropTarget(dropDestinationFromPoint(event.clientX, event.clientY));
  });
  window.addEventListener("dragleave", () => {
    if (els.dropOverlay.hidden) return;
    dragDepth -= 1;
    if (dragDepth <= 0) {
      dragDepth = 0;
      hideDropOverlay();
    }
  });
  window.addEventListener("blur", hideDropOverlay);
  window.addEventListener("drop", handleDrop);
  window.addEventListener("dragend", hideDropOverlay);

  document.addEventListener("click", (event) => {
    if (!longPressFired) return;
    longPressFired = false;
    event.preventDefault();
    event.stopPropagation();
  }, true);

  document.addEventListener("keydown", handleGlobalKeydown);
  document.addEventListener("pointerdown", (event) => {
    if (els.menu.hidden) return;
    if (event.target.closest("#menu")) return;
    if (event.target.closest("[aria-haspopup]")) return;
    closeMenu();
  });
  window.addEventListener("scroll", () => closeMenu(false), { passive: true, capture: true });
  window.addEventListener("resize", () => closeMenu(false));

  window.addEventListener("online", () => {
    updateNetStatus();
    const folder = currentFolder();
    if (state.session && folder && (state.offline || state.loadError)) {
      loadFolder(folder.id, folder.name, { silent: true });
      toast("Kembali online. Menyegarkan folder…");
    }
  });
  window.addEventListener("offline", updateNetStatus);
}

function signOut({ message = "Kamu sudah keluar.", error = false } = {}) {
  Object.values(state.uploads).forEach((upload) => upload.abort?.());
  state.uploads = {};
  clearSession();
  clearFolderCaches();
  state.session = null;
  state.items = [];
  state.path = [];
  state.selected.clear();
  state.anchorId = null;
  state.transfers = {};
  state.search = "";
  state.filter = "all";
  state.offline = false;
  state.detailsId = null;
  state.previewId = null;
  state.pendingEmail = "";
  els.search.value = "";
  els.loginLink.value = "";
  if (els.modal.open) els.modal.close();
  hideDropOverlay();
  closeMenu(false);
  closeDetails();
  closePreview();
  document.body.classList.remove("has-selection");
  els.transferPanel.hidden = true;
  els.transferPanel.innerHTML = "";
  els.fileGrid.innerHTML = "";
  els.selectionBar.hidden = true;
  updateNetStatus();
  showAuth();
  toast(message, { error });
}

/* ============================ PWA ============================ */

async function registerServiceWorker() {
  if (!("serviceWorker" in navigator) || !/^https?:$/.test(location.protocol)) return;
  try {
    const registration = await navigator.serviceWorker.register("./sw.js", { scope: "./" });
    setInterval(() => registration.update().catch(() => {}), 30 * 60 * 1000);
  } catch {
    /* PWA tetap berjalan tanpa cache offline */
  }
}

function wireInstallPrompt() {
  let deferred = null;
  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferred = event;
    els.installBtn.hidden = false;
  });
  els.installBtn.addEventListener("click", async () => {
    if (!deferred) return;
    deferred.prompt();
    await deferred.userChoice;
    deferred = null;
    els.installBtn.hidden = true;
  });
  window.addEventListener("appinstalled", () => {
    deferred = null;
    els.installBtn.hidden = true;
    toast("Aplikasi dipasang. Bisa dibuka offline.");
  });
}

/* ============================ init ============================ */

function init() {
  attachRipples();
  wireGrid();
  wireManager();
  wireInstallPrompt();
  updateNetStatus();
  registerServiceWorker();

  const saved = loadSession();
  if (saved) {
    state.session = saved;
    showManager();
    loadFolder(saved.account.rootFolder, "Root", { reset: true });
  } else {
    showAuth();
  }
}

init();
