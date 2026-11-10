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
    const [h, m] = jam.split(":").map(Number);
    return h * 60 + m;
  };

  const buka = toMinutes(jamBuka);
  const tutup = toMinutes(jamTutup);
  const sekarang = now.getHours() * 60 + now.getMinutes();

  if (buka === tutup) {
    return true;
  }

  if (tutup > buka) {
    return sekarang >= buka && sekarang < tutup;
  }

  return sekarang >= buka || sekarang < tutup;
}
