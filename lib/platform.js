// Helper untuk membedakan lingkungan web (browser biasa) vs native (APK Capacitor)
// dari sisi klien. Tidak dipakai saat SSR (typeof window === "undefined").
export async function cekJembatan() {
  if (typeof window === "undefined") return "server";

  if (!window.Capacitor?.isNativePlatform?.()) {
    return "web";
  }

  try {
    const { Geolocation } = await import("@capacitor/geolocation");
    await Geolocation.checkPermissions();
    return "native-bridge-ok";
  } catch {
    return "native-bridge-gagal";
  }
}