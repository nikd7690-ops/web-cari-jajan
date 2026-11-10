import { describe, it, expect } from "vitest";
import {
  formatRupiah,
  hitungJumlahItem,
  hitungTotal,
  kurangItem,
  tambahItem,
} from "../lib/keranjang.js";

describe("hitungTotal", () => {
  it("menjumlahkan harga dikali qty", () => {
    const keranjang = [
      { id: "a", namaMenu: "Nasi", harga: 8000, qty: 2 },
      { id: "b", namaMenu: "Teh", harga: 3000, qty: 1 },
    ];
    expect(hitungTotal(keranjang)).toBe(19000);
  });

  it("mengembalikan 0 untuk keranjang kosong", () => {
    expect(hitungTotal([])).toBe(0);
  });

  it("membulatkan total ke bilangan bulat", () => {
    expect(hitungTotal([{ id: "a", harga: 1000.4, qty: 1 }])).toBe(1000);
    expect(hitungTotal([{ id: "a", harga: 1000.6, qty: 1 }])).toBe(1001);
  });

  it("menghitung total dengan qty lebih dari satu", () => {
    expect(hitungTotal([{ id: "a", harga: 2500, qty: 4 }])).toBe(10000);
  });
});

describe("hitungJumlahItem", () => {
  it("menjumlahkan seluruh qty", () => {
    expect(
      hitungJumlahItem([
        { id: "a", qty: 2 },
        { id: "b", qty: 3 },
      ])
    ).toBe(5);
  });

  it("mengembalikan 0 untuk keranjang kosong", () => {
    expect(hitungJumlahItem([])).toBe(0);
  });
});

describe("formatRupiah", () => {
  it("memformat ribuan dengan pemisah titik", () => {
    expect(formatRupiah(1500)).toBe("Rp 1.500");
    expect(formatRupiah(1234567)).toBe("Rp 1.234.567");
  });

  it("menangani nol dan nilai tak valid", () => {
    expect(formatRupiah(0)).toBe("Rp 0");
    expect(formatRupiah(undefined)).toBe("Rp 0");
  });
});

describe("tambahItem dan kurangItem", () => {
  it("menambah item baru dengan qty 1", () => {
    const hasil = tambahItem([], { id: "a", namaMenu: "Nasi", harga: 8000 });
    expect(hasil).toEqual([
      { id: "a", namaMenu: "Nasi", harga: 8000, qty: 1 },
    ]);
  });

  it("menambah qty item yang sudah ada", () => {
    const awal = [{ id: "a", namaMenu: "Nasi", harga: 8000, qty: 1 }];
    const hasil = tambahItem(awal, { id: "a", namaMenu: "Nasi", harga: 8000 });
    expect(hasil[0].qty).toBe(2);
  });

  it("mengurangi qty dan menghapus item saat qty 0", () => {
    const awal = [{ id: "a", namaMenu: "Nasi", harga: 8000, qty: 1 }];
    expect(kurangItem(awal, "a")).toEqual([]);
  });
});
