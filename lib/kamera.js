// Pengambilan gambar: native pakai plugin Capacitor Camera, web pakai input file.
// Mengembalikan data: URL (base64) agar aman untuk pipeline canvas (tanpa CORS/taint).
function adalaNative() {
  return typeof window !== "undefined" && !!window.Capacitor?.isNativePlatform?.();
}

export function pakaiKameraNative() {
  return adalaNative();
}

export async function ambilFoto({ sumber = "kamera" } = {}) {
  const mod = await import("@capacitor/camera");
  const { Camera, CameraResultType, CameraSource } = mod;

  const photo = await Camera.getPhoto({
    resultType: CameraResultType.DataUrl,
    source: sumber === "galeri" ? CameraSource.Photos : CameraSource.Camera,
    quality: 80,
    allowEditing: false,
  });

  return photo.dataUrl || null;
}