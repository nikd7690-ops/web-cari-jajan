/**
 * Cek apakah warung sedang buka pada waktu tertentu.
 * Mendukung jam tutup yang lewat tengah malam (mis. buka 18:00, tutup 02:00).
 *
 * Contoh pemakaian:
 *
 * 1. Jam normal, sekarang 10:30
 *    isBuka("08:00", "17:00", new Date("2026-01-01T10:30:00"))
 *    // => true
 *
 * 2. Tutup lewat tengah malam, sekarang 23:30
 *    isBuka("18:00", "02:00", new Date("2026-01-01T23:30:00"))
 *    // => true
 *
 * 3. Tutup lewat tengah malam, sekarang 03:00 (sudah lewat jam tutup)
 *    isBuka("18:00", "02:00", new Date("2026-01-01T03:00:00"))
 *    // => false
 *
 * @param {string} jamBuka - format "HH:mm"
 * @param {string} jamTutup - format "HH:mm"
 * @param {Date} [now] - waktu acuan, default sekarang
 * @returns {boolean}
 */
export function isBuka(jamBuka, jamTutup, now = new Date()) {
  const toMinutes = (jam) => {
    if (!jam || typeof jam !== "string") return 0;
    const parts = jam.split(":");
    const h = parseInt(parts[0], 10) || 0;
    const m = parseInt(parts[1], 10) || 0;
    return h * 60 + m;
  };

  const buka = toMinutes(jamBuka);
  const tutup = toMinutes(jamTutup);

  if (buka === tutup) {
    return true;
  }

  // Ambil jam dan menit dalam zona waktu Indonesia (Asia/Jakarta / WIB)
  let sekarang;
  try {
    const formatter = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Jakarta",
      hour: "numeric",
      minute: "numeric",
      hour12: false,
    });
    const parts = formatter.formatToParts(now);
    let jamNow = 0;
    let menitNow = 0;
    for (const part of parts) {
      if (part.type === "hour") jamNow = parseInt(part.value, 10);
      if (part.type === "minute") menitNow = parseInt(part.value, 10);
    }
    if (jamNow === 24) jamNow = 0;
    sekarang = jamNow * 60 + menitNow;
  } catch {
    sekarang = now.getHours() * 60 + now.getMinutes();
  }

  if (tutup > buka) {
    return sekarang >= buka && sekarang < tutup;
  }

  return sekarang >= buka || sekarang < tutup;
}
