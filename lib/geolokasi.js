// Deteksi & minta lokasi: pakai plugin native Capacitor di dalam APK,
// fallback geolocation browser di web biasa.
async function pluginGeolokasi() {
  const mod = await import("@capacitor/geolocation");
  return mod.Geolocation;
}

function errLokasi(kode, pesan) {
  const e = new Error(pesan);
  e.kode = kode;
  return e;
}

function adalaNative() {
  return typeof window !== "undefined" && !!window.Capacitor?.isNativePlatform?.();
}

let izinDimintaSesi = false;

function pesanIzinDitolak() {
  return errLokasi(
    "denied",
    "Izin lokasi belum diberikan. Aktifkan lewat Settings atau pilih area manual."
  );
}

async function siapkanIzinNatif({ paksa = false } = {}) {
  const Geolocation = await pluginGeolokasi();
  let izin = await Geolocation.checkPermissions();
  let state = izin?.location ?? "prompt";

  if (state === "prompt" && (!izinDimintaSesi || paksa)) {
    izinDimintaSesi = true;
    izin = await Geolocation.requestPermissions();
    state = izin?.location ?? "prompt";
  }

  return { Geolocation, state };
}

export function aturUlangIzinSesi() {
  izinDimintaSesi = false;
}

export async function deteksiLokasi() {
  if (typeof window === "undefined") {
    return { status: "unsupported", native: false };
  }

  if (adalaNative()) {
    try {
      const Geolocation = await pluginGeolokasi();
      const izin = await Geolocation.checkPermissions();
      const mentah = izin?.location ?? "prompt";
      const status =
        mentah === "granted"
          ? "granted"
          : mentah === "denied"
            ? "denied"
            : "prompt";
      return { status, native: true };
    } catch {
      return { status: "unsupported", native: true };
    }
  }

  if (!window.isSecureContext) return { status: "insecure", native: false };
  if (!navigator.geolocation) return { status: "unsupported", native: false };

  try {
    const izin = await navigator.permissions.query({ name: "geolocation" });
    return { status: izin.state, native: false };
  } catch {
    return { status: "prompt", native: false };
  }
}

export function pantauIzinLokasi(cb) {
  if (typeof window === "undefined" || adalaNative()) {
    return () => {};
  }

  let batal = false;
  let status = null;
  const perms = navigator.permissions?.query
    ? navigator.permissions
        .query({ name: "geolocation" })
        .then((s) => {
          if (batal) return;
          status = s;
          s.onchange = () => cb(s.state);
        })
        .catch(() => {})
    : null;

  return () => {
    batal = true;
    if (status) status.onchange = null;
    void perms;
  };
}

export async function mintaLokasi({ timeout = 10000, retryPermission = false } = {}) {
  if (typeof window === "undefined") {
    throw errLokasi("unsupported", "Lokasi tidak tersedia.");
  }

  if (adalaNative()) {
    try {
      const { Geolocation, state } = await siapkanIzinNatif({
        paksa: retryPermission,
      });
      if (state !== "granted") throw pesanIzinDitolak();
      const pos = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout,
      });
      return { lat: pos.coords.latitude, lng: pos.coords.longitude };
    } catch (e) {
      if (e?.kode) throw e;
      const teks = String(e?.message || e || "");
      if (/permission|denied/i.test(teks)) {
        throw pesanIzinDitolak();
      }
      if (/timeout/i.test(teks)) {
        throw errLokasi("timeout", "Gagal membaca lokasi. Coba lagi.");
      }
      throw errLokasi("unavailable", "Gagal membaca lokasi. Coba lagi.");
    }
  }

  if (!window.isSecureContext) {
    throw errLokasi("insecure", "Lokasi hanya aktif lewat HTTPS.");
  }
  if (!navigator.geolocation) {
    throw errLokasi("unsupported", "Perangkat tidak mendukung lokasi.");
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      (e) => {
        const peta = { 1: "denied", 2: "unavailable", 3: "timeout" };
        const kode = peta[e.code] || "unavailable";
        const pesan =
          kode === "denied"
            ? "Izin lokasi ditolak."
            : kode === "timeout"
              ? "Waktu permintaan lokasi habis. Coba lagi."
              : "Lokasi tidak tersedia saat ini.";
        reject(errLokasi(kode, pesan));
      },
      { enableHighAccuracy: true, timeout, maximumAge: 60000 }
    );
  });
}