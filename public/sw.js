const CACHE = "cj-runtime-v1";
const ORIGIN = self.location.origin;

function bolehCache(pathname) {
  if (pathname.startsWith("/api/auth")) return false; // jangan cache alur login
  return true;
}

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  let url;
  try {
    url = new URL(req.url);
  } catch {
    return;
  }
  if (url.origin !== ORIGIN) return;
  if (!bolehCache(url.pathname)) return;

  const nav = req.mode === "navigate";

  if (nav) {
    // Network-first, fallback ke cache (halaman terakhir) saat offline.
    event.respondWith(
      (async () => {
        const cache = await caches.open(CACHE);
        try {
          const res = await fetch(req);
          cache.put(req, res.clone());
          cache.put("/", res.clone());
          return res;
        } catch {
          return (await cache.match(req)) || (await cache.match("/")) || Response.error();
        }
      })()
    );
    return;
  }

  // Stale-while-revalidate untuk data & aset.
  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE);
      const cached = await cache.match(req);
      const jaringan = fetch(req)
        .then(async (res) => {
          if (res && res.ok) cache.put(req, res.clone());
          return res;
        })
        .catch(() => cached || Response.error());
      return cached || jaringan;
    })()
  );
});
