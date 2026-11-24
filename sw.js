/**
 * Service worker Gofly.
 *
 * - App shell (HTML/CSS/JS/ikon) : cache-first + pembaruan latar belakang.
 * - API GoFile (/contents/*)     : network-first, disalin ke cache.
 * - Navigasi                     : network-first, fallback index.html/offline.html.
 * - Font Google                  : stale-while-revalidate agar ikon tetap ada offline.
 *
 * Cache diberi versi; cache lama dibersihkan saat activate.
 */

const VERSION = "v11";
const SHELL_CACHE = `gofly-shell-${VERSION}`;
const API_CACHE = `gofly-api-${VERSION}`;
const FONT_CACHE = `gofly-fonts-${VERSION}`;
const OWN_CACHES = new Set([SHELL_CACHE, API_CACHE, FONT_CACHE]);

const resolve = (path) => new URL(path, self.location.href).href;

const FONT_CSS_URL =
  "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&display=swap";

const SHELL = [
  "./",
  "./index.html",
  "./offline.html",
  "./styles.css",
  "./js/app.js",
  "./js/api.js",
  "./js/qr.js",
  "./manifest.webmanifest",
  "./icons/icon.svg",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./icons/icon-maskable.svg",
  "./icons/icon-maskable-512.png",
];

async function precacheFonts() {
  try {
    const cache = await caches.open(FONT_CACHE);
    const response = await fetch(FONT_CSS_URL, { cache: "reload" });
    if (!response.ok) return;
    await cache.put(FONT_CSS_URL, response.clone());
    const css = await response.text();
    const urls = [
      ...new Set(
        [...css.matchAll(/url\((https:\/\/[^)]+)\)/g)].map((match) => match[1].replace(/["']/g, "")),
      ),
    ];
    await Promise.all(
      urls.map(async (url) => {
        try {
          const font = await fetch(url);
          if (font.ok || font.type === "opaque") await cache.put(url, font);
        } catch {
          /* satu berkas font gagal — ikon memakai fallback */
        }
      }),
    );
  } catch {
    /* offline saat instalasi: font dimuat saat online berikutnya */
  }
}

self.addEventListener("install", (event) => {
  event.waitUntil(
    Promise.all([caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL)), precacheFonts()]).then(() =>
      self.skipWaiting(),
    ),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(keys.filter((key) => (key.startsWith("gofile-") || key.startsWith("gofly-")) && !OWN_CACHES.has(key)).map((key) => caches.delete(key))),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("message", (event) => {
  if (event.data?.type === "SKIP_WAITING") self.skipWaiting();
});

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) {
    fetch(request)
      .then((response) => {
        if (response && (response.ok || response.type === "opaque")) cache.put(request, response.clone());
      })
      .catch(() => {});
    return cached;
  }
  try {
    const response = await fetch(request);
    if (response && (response.ok || response.type === "opaque")) cache.put(request, response.clone());
    return response;
  } catch {
    return new Response("Aset belum tersedia offline.", {
      status: 504,
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }
}

async function networkFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  try {
    const response = await fetch(request);
    if (response?.ok) {
      cache.put(request, response.clone());
      trimCache(cache, 60);
    }
    return response;
  } catch {
    const cached = await cache.match(request, { ignoreSearch: true });
    if (cached) return cached;
    return new Response(JSON.stringify({ status: "error-offline", data: { message: "Offline." } }), {
      status: 503,
      headers: { "Content-Type": "application/json" },
    });
  }
}

async function staleWhileRevalidate(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  const network = fetch(request)
    .then((response) => {
      if (response && (response.ok || response.type === "opaque")) cache.put(request, response.clone());
      return response;
    })
    .catch(() => null);
  if (cached) return cached;
  const response = await network;
  return response || Response.error();
}

async function trimCache(cache, maxEntries) {
  const keys = await cache.keys();
  if (keys.length <= maxEntries) return;
  await Promise.all(keys.slice(0, keys.length - maxEntries).map((key) => cache.delete(key)));
}

async function handleNavigation(request) {
  const path = new URL(request.url).pathname;
  const isAppShell = path.endsWith("/") || path.endsWith("/index.html");
  try {
    const response = await fetch(request);
    if (response?.ok && isAppShell) {
      const cache = await caches.open(SHELL_CACHE);
      cache.put(resolve("./index.html"), response.clone()).catch(() => {});
    }
    return response;
  } catch {
    const cache = await caches.open(SHELL_CACHE);
    const index = (await cache.match(resolve("./index.html"))) || (await cache.match(resolve("./")));
    const offlinePage = await cache.match(resolve("./offline.html"));
    if (isAppShell) {
      return (
        index ||
        offlinePage ||
        new Response("Offline.", { status: 503, headers: { "Content-Type": "text/plain; charset=utf-8" } })
      );
    }
    return (
      offlinePage ||
      index ||
      new Response("Offline.", { status: 503, headers: { "Content-Type": "text/plain; charset=utf-8" } })
    );
  }
}

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);

  if (request.mode === "navigate") {
    event.respondWith(handleNavigation(request));
    return;
  }

  if (url.origin === self.location.origin) {
    if (url.pathname.endsWith("/sw.js")) return;
    event.respondWith(cacheFirst(request, SHELL_CACHE));
    return;
  }

  if (url.hostname === "api.gofile.io") {
    // Hanya isi folder yang di-cache; profil akun tidak pernah disimpan.
    if (url.pathname.startsWith("/contents/")) {
      event.respondWith(networkFirst(request, API_CACHE));
    }
    return;
  }

  if (url.hostname === "upload.gofile.io") return;

  if (url.hostname === "fonts.googleapis.com" || url.hostname === "fonts.gstatic.com") {
    event.respondWith(staleWhileRevalidate(request, FONT_CACHE));
  }
});
