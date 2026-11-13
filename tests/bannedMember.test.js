import { describe, it, expect } from "vitest";
import { z } from "zod";

describe("Fitur Banned & Moderasi Member Admin", () => {
  const banSchema = z.object({
    banned: z.boolean(),
    reason: z.string().optional().default(""),
  });

  it("memvalidasi payload status banned dengan benar", () => {
    const valid = banSchema.safeParse({ banned: true, reason: "Spam warung fiktif" });
    expect(valid.success).toBe(true);
    expect(valid.data.banned).toBe(true);
    expect(valid.data.reason).toBe("Spam warung fiktif");

    const unban = banSchema.safeParse({ banned: false });
    expect(unban.success).toBe(true);
    expect(unban.data.banned).toBe(false);
    expect(unban.data.reason).toBe("");

    const invalid = banSchema.safeParse({ banned: "ya" });
    expect(invalid.success).toBe(false);
  });

  it("mencegah login untuk pengguna yang di-ban", () => {
    const userDb = {
      email: "pelanggar@gmail.com",
      isBanned: true,
      bannedReason: "Melanggar etika komunitas",
    };

    function verifikasiAksesUser(user) {
      if (user.isBanned) {
        return { bolehLogin: false, pesan: "Akun Anda telah ditangguhkan/diblokir oleh admin." };
      }
      return { bolehLogin: true, pesan: "Akses diizinkan" };
    }

    const hasil = verifikasiAksesUser(userDb);
    expect(hasil.bolehLogin).toBe(false);
    expect(hasil.pesan).toContain("diblokir");
  });

  it("memformat data vendor admin agar submittedBy selalu String (tidak membingungkan DTO mobile)", () => {
    const rawVendorFromMongoose = {
      _id: "650000000000000000000001",
      namaToko: "Warung Bu Siti",
      submittedBy: {
        _id: "650000000000000000000099",
        name: "Siti Rahma",
        email: "siti@gmail.com",
      },
    };

    function formatVendorUntukClient(v) {
      let namaPengaju = "Warga Komunitas";
      let submittedById = null;

      if (v.submittedBy && typeof v.submittedBy === "object") {
        namaPengaju = v.submittedBy.name || v.submittedBy.email || "Warga Komunitas";
        submittedById = v.submittedBy._id ? v.submittedBy._id.toString() : null;
      } else if (v.submittedBy) {
        submittedById = v.submittedBy.toString();
      }

      return {
        ...v,
        _id: v._id.toString(),
        submittedBy: submittedById,
        namaKontributor: namaPengaju,
      };
    }

    const formatted = formatVendorUntukClient(rawVendorFromMongoose);
    expect(typeof formatted.submittedBy).toBe("string");
    expect(formatted.submittedBy).toBe("650000000000000000000099");
    expect(formatted.namaKontributor).toBe("Siti Rahma");
  });

  it("memformat data laporan admin agar vendorId selalu String", () => {
    const rawReport = {
      _id: "650000000000000000000088",
      vendorId: {
        _id: "650000000000000000000001",
        namaToko: "Warung Mas Joko",
      },
      alasan: "lokasi_salah",
    };

    function formatLaporan(r) {
      let vendorIdStr = null;
      let namaToko = r.namaToko || "Warung";

      if (r.vendorId && typeof r.vendorId === "object") {
        vendorIdStr = r.vendorId._id ? r.vendorId._id.toString() : null;
        namaToko = r.vendorId.namaToko || namaToko;
      } else if (r.vendorId) {
        vendorIdStr = r.vendorId.toString();
      }

      return {
        ...r,
        _id: r._id.toString(),
        vendorId: vendorIdStr,
        namaToko,
      };
    }

    const formatted = formatLaporan(rawReport);
    expect(typeof formatted.vendorId).toBe("string");
    expect(formatted.vendorId).toBe("650000000000000000000001");
    expect(formatted.namaToko).toBe("Warung Mas Joko");
  });
});
