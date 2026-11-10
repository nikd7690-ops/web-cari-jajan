const catatan =
  global.__rateLimitCatatan ?? (global.__rateLimitCatatan = new Map());

export function cekRateLimit(kunci, max = 5, jendelaMs = 10 * 60 * 1000) {
  const sekarang = Date.now();
  const daftar = (catatan.get(kunci) || []).filter(
    (waktu) => sekarang - waktu < jendelaMs
  );

  if (daftar.length >= max) {
    const sisaMs = jendelaMs - (sekarang - daftar[0]);
    catatan.set(kunci, daftar);
    return { ok: false, sisaDetik: Math.ceil(sisaMs / 1000) };
  }

  daftar.push(sekarang);
  catatan.set(kunci, daftar);
  return { ok: true, sisaDetik: 0 };
}