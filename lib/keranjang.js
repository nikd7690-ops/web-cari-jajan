export function formatRupiah(angka) {
  const nilai = Number(angka) || 0;
  return "Rp " + nilai.toLocaleString("id-ID");
}

export function cariItem(keranjang, id) {
  return keranjang.find((item) => item.id === id);
}

export function tambahItem(keranjang, menu) {
  const ada = cariItem(keranjang, menu.id);

  if (ada) {
    return keranjang.map((item) =>
      item.id === menu.id ? { ...item, qty: item.qty + 1 } : item
    );
  }

  return [
    ...keranjang,
    { id: menu.id, namaMenu: menu.namaMenu, harga: menu.harga, qty: 1 },
  ];
}

export function kurangItem(keranjang, id) {
  return keranjang
    .map((item) => (item.id === id ? { ...item, qty: item.qty - 1 } : item))
    .filter((item) => item.qty > 0);
}

export function hitungJumlahItem(keranjang) {
  return keranjang.reduce((jumlah, item) => jumlah + item.qty, 0);
}

export function hitungTotal(keranjang) {
  return keranjang.reduce((total, item) => total + item.harga * item.qty, 0);
}

export function normalisasiNomorWA(nomor) {
  const digit = String(nomor).replace(/\D/g, "");
  if (digit.startsWith("0")) return "62" + digit.slice(1);
  if (digit.startsWith("62")) return digit;
  return digit;
}

export function buatTeksPesanan({ namaToko, keranjang, mode }) {
  const baris = keranjang.map(
    (item, index) =>
      `${index + 1}. ${item.namaMenu} x${item.qty} = ${formatRupiah(
        item.harga * item.qty
      )}`
  );

  return [
    `Halo, saya mau pesan di ${namaToko}:`,
    "",
    ...baris,
    "",
    `Total: ${formatRupiah(hitungTotal(keranjang))}`,
    `Opsi: ${mode}`,
    "",
    "Terima kasih.",
  ].join("\n");
}
