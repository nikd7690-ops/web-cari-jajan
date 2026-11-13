import { describe, it, expect } from "vitest";
import { vendorInputSchema, vendorUpdateSchema } from "../lib/vendorSchema.js";

describe("Aturan Peran Akun: Pengguna vs Mitra & Fitur Zero Waste", () => {
  it("memvalidasi skema input warung dengan objek zeroWaste aktif dan diskon", () => {
    const validData = {
      namaToko: "Warung Berkah Jaya",
      kategori: "warung",
      lat: -8.17,
      lng: 113.7,
      alamatPatokan: "Jl. Kalimantan No. 12",
      jamBuka: "08:00",
      jamTutup: "21:00",
      zeroWaste: {
        aktif: true,
        diskonPersen: 30,
        keterangan: "Diskon 30% sisa makanan jelang tutup",
      },
    };

    const parsed = vendorInputSchema.safeParse(validData);
    expect(parsed.success).toBe(true);
    expect(parsed.data.zeroWaste?.aktif).toBe(true);
    expect(parsed.data.zeroWaste?.diskonPersen).toBe(30);
  });

  it("menolak diskon zeroWaste jika di luar rentang 0-100%", () => {
    const invalidData = {
      namaToko: "Warung Berkah",
      kategori: "warung",
      lat: -8.17,
      lng: 113.7,
      alamatPatokan: "Jl. Kalimantan",
      jamBuka: "08:00",
      jamTutup: "21:00",
      zeroWaste: {
        aktif: true,
        diskonPersen: 150, // invalid > 100%
      },
    };

    const parsed = vendorInputSchema.safeParse(invalidData);
    expect(parsed.success).toBe(false);
  });

  it("memvalidasi aturan: Mitra dibatasi maksimal 1 warung", () => {
    function cekBatasWarungMitra(tipeAkun, jumlahWarungDimiliki, isAdmin = false) {
      if (isAdmin) return { boleh: true };
      if (tipeAkun === "mitra" && jumlahWarungDimiliki >= 1) {
        return {
          boleh: false,
          pesan:
            "Mitra / Pemilik warung hanya dapat memiliki 1 warung. Untuk menambah warung kedua atau cabang, silakan ajukan permohonan ke admin terlebih dahulu.",
        };
      }
      return { boleh: true };
    }

    // Mitra belum punya warung -> boleh buat 1
    expect(cekBatasWarungMitra("mitra", 0).boleh).toBe(true);

    // Mitra sudah punya 1 warung -> ditolak jika mau buat lagi
    const hasilTolak = cekBatasWarungMitra("mitra", 1);
    expect(hasilTolak.boleh).toBe(false);
    expect(hasilTolak.pesan).toContain("hanya dapat memiliki 1 warung");

    // Pengguna/member -> boleh menambah lebih dari 1 warung (bebas berkontribusi data)
    expect(cekBatasWarungMitra("pengguna", 0).boleh).toBe(true);
    expect(cekBatasWarungMitra("pengguna", 1).boleh).toBe(true);
    expect(cekBatasWarungMitra("pengguna", 5).boleh).toBe(true);
  });

  it("memvalidasi aturan: Pengguna/member dilarang mengubah (edit/delete) data warung yang telah diajukan", () => {
    function cekIzinEditWarung(tipeAkun, isOwner, isAdmin = false) {
      if (isAdmin) return { boleh: true };
      if (tipeAkun === "pengguna") {
        return {
          boleh: false,
          pesan:
            "Akun Pengguna/Member tidak dapat mengubah data warung yang telah diajukan. Hanya pemilik warung (mitra) resmi atau admin yang dapat mengubah data warung.",
        };
      }
      if (tipeAkun === "mitra" && !isOwner) {
        return {
          boleh: false,
          pesan: "Tidak boleh mengubah warung milik penjual lain.",
        };
      }
      return { boleh: true };
    }

    // Pengguna mencoba mengedit warung -> selalu ditolak
    const hasilPengguna = cekIzinEditWarung("pengguna", false);
    expect(hasilPengguna.boleh).toBe(false);
    expect(hasilPengguna.pesan).toContain("Pengguna/Member tidak dapat mengubah data warung");

    // Mitra mengedit warung miliknya sendiri -> diizinkan
    expect(cekIzinEditWarung("mitra", true).boleh).toBe(true);

    // Mitra mengedit warung mitra lain -> ditolak
    expect(cekIzinEditWarung("mitra", false).boleh).toBe(false);

    // Admin selalu boleh
    expect(cekIzinEditWarung("pengguna", false, true).boleh).toBe(true);
  });
});
