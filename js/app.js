import {
  sendLoginLink,
  getAccount,
  getFolder,
  loadSession,
  saveSession,
  clearSession,
  extractToken,
  GoFileError,
} from "./api.js?v=2";

const $ = (selector) => document.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const els = {
  auth: $("#auth"),
  manager: $("#manager"),
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
  search: $("#search"),
  syncBtn: $("#sync-btn"),
  logoutBtn: $("#logout-btn"),
  sortBtn: $("#sort-btn"),
  sortLabel: $("#sort-label"),
  snackbar: $("#snackbar"),
  busy: $("#busy"),
  accountName: $("#account-name"),
  avatar: $("#avatar"),
  uploadBtn: $("#upload-btn"),
  androidNotice: $("#android-notice"),
  grantNotif: $("#grant-notif"),
  grantBg: $("#grant-bg"),
  transferPanel: $("#transfer-panel"),
};

const state = {
  session: null,
  pendingEmail: "",
  path: [], // breadcrumb: [{ id, name }]
  items: [],
  sortIndex: 0,
  view: "grid",
  search: "",
  android: false,
  transfers: {},
  itemLinks: {},
};

const SORTS = [
  { field: "name", direction: 1, label: "Nama A–Z" },
  { field: "name", direction: -1, label: "Nama Z–A" },
  { field: "createTime", direction: -1, label: "Terbaru" },
  { field: "size", direction: -1, label: "Terbesar" },
];

/* ============================ helpers ============================ */

function icon(name, { cls = "", fill = false, size } = {}) {
  const style = [
    size ? `font-size:${size}px` : "",
    fill ? "font-variation-settings:'FILL' 1,'wght' 560,'opsz' 32" : "",
  ]
    .filter(Boolean)
    .join(";");
  return `<span class="ms ${cls}" ${style ? `style="${style}"` : ""}>${name}</span>`;
}

function openExternal(url) {
  if (state.android && typeof window.AndroidBridge?.openExternal === "function") {
    window.AndroidBridge.openExternal(url);
  } else {
    window.open(url, "_blank", "noopener,noreferrer");
  }
}

function toast(message, error = false) {
  const iconName = error ? "error" : "check_circle";
  els.snackbar.innerHTML = `${icon(iconName)}<span>${escapeHtml(message)}</span>`;
  els.snackbar.classList.toggle("is-error", error);
  els.snackbar.classList.add("is-visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => els.snackbar.classList.remove("is-visible"), 3800);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" })[c],
  );
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
  const d = new Date(Number(value) * 1000 || value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("id-ID", { day: "numeric", month: "short", year: "numeric" });
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
  if (/\.(js|ts|jsx|tsx|py|go|rs|java|css|html|json|md|sh)$/.test(name)) return { name: "code", cls: "code" };
  if (mime.includes("spreadsheet") || /\.(csv|xlsx|xls)$/.test(name)) return { name: "table_chart", cls: "doc" };
  if (mime.startsWith("text/") || /\.(txt|doc|docx|odt)$/.test(name)) return { name: "description", cls: "doc" };
  return { name: "draft", cls: "" };
}

function withTransition(update) {
  if (document.startViewTransition) document.startViewTransition(update);
  else update();
}

function attachRipples() {
  document.addEventListener("pointerdown", (event) => {
    const target = event.target.closest(".btn, .icon-btn, .seg, .file-card, .crumb");
    if (!target) return;
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
    authError(error.message || "Gagal mengirim login link.");
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
    await loadFolder(account.rootFolder, "Root", false);
    toast("Berhasil masuk. Selamat datang!");
  } catch (error) {
    authError(
      error instanceof GoFileError && error.kind === "network"
        ? error.message
        : "Login link tidak valid atau sudah kedaluwarsa. Minta link baru.",
    );
  } finally {
    setBusy(false);
  }
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

/* ============================ file manager ============================ */

async function loadFolder(id, name, push = true) {
  if (!state.session) return;
  const token = state.session.token;

  if (push) {
    const existing = state.path.findIndex((p) => p.id === id);
    if (existing >= 0) state.path = state.path.slice(0, existing + 1);
    else state.path.push({ id, name });
  } else {
    state.path = [{ id, name }];
  }

  renderBreadcrumbs();
  els.folderTitle.textContent = name;
  els.folderSub.textContent = "Menyinkronkan isi folder…";
  els.fileGrid.hidden = true;
  els.empty.hidden = true;
  els.loading.hidden = false;
  els.loading.innerHTML = Array.from({ length: 8 }, () => '<div class="skeleton"></div>').join("");

  try {
    const sort = SORTS[state.sortIndex];
    const data = await getFolder(token, id, { sortField: sort.field, sortDirection: sort.direction });
    if (data?.canAccess === false) {
      throw new GoFileError("Folder ini tidak dapat diakses dengan akun ini.", { kind: "input" });
    }
    const children = data?.children || {};
    state.itemLinks = {};
    state.items = Object.entries(children).map(([key, item]) => ({ id: key, ...item }));
    renderFiles();
  } catch (error) {
    state.items = [];
    renderFiles();
    toast(error.message || "Gagal memuat folder.", true);
  } finally {
    els.loading.hidden = true;
  }
}

function renderBreadcrumbs() {
  els.breadcrumbs.innerHTML = state.path
    .map(
      (part, index) => `
      <button class="crumb ${index === state.path.length - 1 ? "is-current" : ""}" data-index="${index}" type="button">
        ${icon(index === 0 ? "home" : "folder", { fill: index === 0 })}
        <span>${escapeHtml(part.name)}</span>
        ${index < state.path.length - 1 ? icon("chevron_right") : ""}
      </button>`,
    )
    .join("");
  $$(".crumb", els.breadcrumbs).forEach((node) =>
    node.addEventListener("click", () => {
      const index = Number(node.dataset.index);
      const target = state.path[index];
      state.path = state.path.slice(0, index + 1);
      withTransition(() => loadFolder(target.id, target.name, false));
    }),
  );
}

function visibleItems() {
  const term = state.search.trim().toLowerCase();
  const list = term ? state.items.filter((item) => String(item.name || "").toLowerCase().includes(term)) : state.items;
  // Folder selalu lebih dulu, lalu urutkan sesuai mode sort.
  const sort = SORTS[state.sortIndex];
  return [...list].sort((a, b) => {
    if (a.type !== b.type) return a.type === "folder" ? -1 : 1;
    let av, bv;
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

function renderFiles() {
  const items = visibleItems();
  const folders = state.items.filter((i) => i.type === "folder").length;
  const files = state.items.length - folders;
  const totalSize = state.items.reduce((sum, i) => sum + (Number(i.size) || 0), 0);

  els.stats.innerHTML = `
    <div class="stat" style="animation-delay:0ms">${icon("data_object")}<div><b>${state.items.length}</b><small>semua</small></div></div>
    <div class="stat folders" style="animation-delay:60ms">${icon("folder")}<div><b>${folders}</b><small>folder</small></div></div>
    <div class="stat files" style="animation-delay:120ms">${icon("description")}<div><b>${files}</b><small>file</small></div></div>
    <div class="stat" style="animation-delay:180ms">${icon("database")}<div><b>${formatBytes(totalSize)}</b><small>total</small></div></div>`;

  els.folderSub.textContent = `${state.items.length} item di folder ini · tersinkron dengan GoFile`;

  if (!items.length) {
    els.fileGrid.hidden = true;
    els.empty.hidden = false;
    els.emptyTitle.textContent = state.search ? "Tidak ada hasil" : "Folder ini kosong";
    els.emptyText.textContent = state.search
      ? `Tidak ada item yang cocok dengan “${state.search}”.`
      : "Belum ada file atau folder di sini.";
    els.empty.querySelector(".empty-icon").textContent = state.search ? "search_off" : "folder_off";
    return;
  }

  els.empty.hidden = true;
  els.fileGrid.hidden = false;
  els.fileGrid.classList.toggle("list-view", state.view === "list");
  els.fileGrid.innerHTML = items
    .map((item, index) => {
      const glyph = iconFor(item);
      const detail = item.type === "folder" ? "Folder" : `${formatBytes(item.size)} · ${formatDate(item.createTime)}`;
      const canDownload = state.android && item.type !== "folder" && !!item.link;
      if (canDownload) state.itemLinks[item.id] = item.link;
      return `
        <div class="file-card ${canDownload ? "has-download" : ""}" data-id="${escapeHtml(item.id)}" data-type="${escapeHtml(item.type)}" style="--i:${Math.min(index, 20)}" role="button" tabindex="0">
          <span class="icon-tile ${glyph.cls}">${icon(glyph.name)}</span>
          <span class="file-meta">
            <span class="file-name">${escapeHtml(item.name || "Tanpa nama")}</span>
            <span class="file-detail">${escapeHtml(detail)}</span>
          </span>
          ${canDownload ? `<span class="dl-btn" data-dl-id="${escapeHtml(item.id)}" title="Unduh">${icon("download")}</span>` : ""}
          ${icon("arrow_forward", { cls: "file-arrow" })}
        </div>`;
    })
    .join("");

  $$(".file-card", els.fileGrid).forEach((card) => {
    const open = () => {
      const item = state.items.find((i) => i.id === card.dataset.id);
      if (!item) return;
      if (item.type === "folder") {
        withTransition(() => loadFolder(item.id, item.name || "Folder", true));
      } else {
        openExternal(`https://gofile.io/d/${encodeURIComponent(item.id)}`);
      }
    };
    card.addEventListener("click", (event) => {
      if (event.target.closest(".dl-btn")) return;
      open();
    });
    card.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        open();
      }
    });
  });

  $$(".dl-btn", els.fileGrid).forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      if (!state.android) return;
      const id = button.dataset.dlId;
      const link = state.itemLinks[id];
      const item = state.items.find((i) => i.id === id);
      if (!link || !item) return;
      if (!window.AndroidBridge.hasNotificationPermission()) {
        refreshPermissionUI();
        toast("Nyalakan izin notifikasi dulu agar unduh tetap berjalan di latar belakang.", true);
        return;
      }
      window.AndroidBridge.download(link, item.name || "berkas");
    });
  });
}

/* ============================ Android bridge ============================ */

function initAndroidBridge() {
  const bridge = window.AndroidBridge;
  if (!bridge || typeof bridge.isAvailable !== "function" || !bridge.isAvailable()) return;
  state.android = true;

  // Dipanggil Kotlin setiap kali progres transfer berubah.
  window.onTransferUpdate = (update) => {
    const id = String(update?.id || "");
    if (!id) return;
    state.transfers[id] = { ...update, updatedAt: Date.now() };
    renderTransfers();

    if (update.state === "success") {
      if (update.type === "upload") {
        const current = state.path[state.path.length - 1];
        if (current && !els.manager.hidden) loadFolder(current.id, current.name, false);
      }
      toast(
        update.type === "upload"
          ? `${update.fileName} berhasil diunggah.`
          : `${update.fileName} tersimpan di Downloads.`,
      );
    } else if (update.state === "error") {
      toast(update.message || "Transfer gagal.", true);
    }
  };

  window.onPermissionResult = (kind, granted) => {
    if (kind === "notifications" && granted) {
      refreshPermissionUI();
      toast("Izin notifikasi sudah aktif.");
    }
  };

  window.onPermissionRequired = () => {
    refreshPermissionUI();
    els.androidNotice.scrollIntoView({ behavior: "smooth", block: "center" });
  };

  els.uploadBtn.hidden = false;

  try {
    const saved = JSON.parse(bridge.getTransfers() || "[]");
    saved.forEach((item) => {
      state.transfers[item.id] = { ...item, updatedAt: Date.now() };
    });
    renderTransfers();
  } catch {
    /* abaikan kalau format tidak dikenali */
  }

  refreshPermissionUI();
}

function refreshPermissionUI() {
  if (!state.android) return;
  const bridge = window.AndroidBridge;
  const notifications = !!bridge.hasNotificationPermission();
  const background = typeof bridge.isBackgroundAllowed === "function" ? !!bridge.isBackgroundAllowed() : true;
  els.androidNotice.hidden = notifications && background;
  els.grantNotif.classList.toggle("is-active-ok", notifications);
  els.grantBg.classList.toggle("is-active-ok", background);
}

function renderTransfers() {
  const list = Object.values(state.transfers)
    .filter((item) => {
      const fresh = Date.now() - (item.updatedAt || 0) < 12000;
      return item.state === "running" || fresh;
    })
    .sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));

  els.transferPanel.hidden = list.length === 0;
  if (!list.length) {
    els.transferPanel.innerHTML = "";
    return;
  }

  els.transferPanel.innerHTML = `
    <div class="transfer-head">${icon("swap_vert")} Transfer GoFile<span>${list.length}</span></div>
    ${list
      .map((item) => {
        const cls =
          item.state === "error" ? "is-error" : item.state === "success" ? "is-success" : "";
        const glyph = item.type === "upload" ? "upload" : "download";
        const caption =
          item.state === "success"
            ? "Selesai"
            : item.state === "error"
              ? item.message || "Gagal"
              : Number(item.progress) >= 0
                ? `${item.progress}% · ${item.message || "Berjalan"}`
                : "Menyiapkan…";
        const progress = Math.max(0, Math.min(100, Number(item.progress) || 0));
        return `
          <div class="transfer-row ${cls}">
            <span class="ms" data-fill="1">${glyph}</span>
            <div class="transfer-body">
              <div class="transfer-name">${escapeHtml(item.fileName || "berkas")}</div>
              <div class="progress"><i style="width:${progress}%"></i></div>
              <small>${escapeHtml(caption)}</small>
            </div>
          </div>`;
      })
      .join("")}`;
}

/* ============================ wiring ============================ */

function init() {
  attachRipples();
  initAndroidBridge();

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
      authError(error.message || "Gagal mengirim ulang link.");
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
      toast("Clipboard tidak diizinkan. Tempel manual ya.", true);
    }
  });

  els.syncBtn.addEventListener("click", async () => {
    if (!state.path.length) return;
    const current = state.path[state.path.length - 1];
    els.syncBtn.classList.add("syncing");
    await loadFolder(current.id, current.name, false);
    els.syncBtn.classList.remove("syncing");
    toast("Folder tersinkron ulang.");
  });

  els.logoutBtn.addEventListener("click", () => {
    clearSession();
    state.session = null;
    state.items = [];
    state.path = [];
    showAuth();
    toast("Kamu sudah keluar.");
  });

  els.search.addEventListener("input", (event) => {
    state.search = event.target.value;
    renderFiles();
  });

  $$(".seg").forEach((button) =>
    button.addEventListener("click", () => {
      state.view = button.dataset.view;
      $$(".seg").forEach((b) => b.classList.toggle("is-active", b === button));
      renderFiles();
    }),
  );

  els.sortBtn.addEventListener("click", () => {
    state.sortIndex = (state.sortIndex + 1) % SORTS.length;
    els.sortLabel.textContent = SORTS[state.sortIndex].label;
    if (state.path.length) {
      const current = state.path[state.path.length - 1];
      loadFolder(current.id, current.name, false);
    }
  });

  els.uploadBtn.addEventListener("click", () => {
    if (!state.android || !state.session) return;
    const current = state.path[state.path.length - 1];
    if (!current) return;
    if (!window.AndroidBridge.hasNotificationPermission()) {
      refreshPermissionUI();
      toast("Nyalakan izin notifikasi dulu agar unggah tetap berjalan di latar belakang.", true);
      return;
    }
    window.AndroidBridge.pickAndUpload(current.id, state.session.token);
  });

  els.grantNotif.addEventListener("click", () => {
    window.AndroidBridge.requestNotificationPermission();
  });

  els.grantBg.addEventListener("click", () => {
    window.AndroidBridge.requestBackgroundPermission();
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "/" && !/input|textarea/i.test(document.activeElement.tagName) && !els.manager.hidden) {
      event.preventDefault();
      els.search.focus();
    }
    if (event.key === "Escape" && document.activeElement === els.search) {
      els.search.value = "";
      state.search = "";
      renderFiles();
      els.search.blur();
    }
  });

  // Pulihkan sesi yang tersimpan.
  const saved = loadSession();
  if (saved) {
    state.session = saved;
    showManager();
    loadFolder(saved.account.rootFolder, "Root", false);
  } else {
    showAuth();
  }
}

init();
