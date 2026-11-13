import { describe, it, expect } from "vitest";
import { z } from "zod";

const reportSchema = z.object({
  vendorId: z.string().min(1, "ID warung wajib diisi"),
  alasan: z.enum(["lokasi_salah", "sudah_tutup", "menu_salah", "duplikat", "lainnya"]),
  deskripsi: z.string().max(500).optional().default(""),
});

describe("Sistem Pelaporan Warung & Dashboard Admin", () => {
  it("memvalidasi laporan yang benar dengan alasan 'lokasi_salah'", () => {
    const input = {
      vendorId: "654321654321654321654321",
      alasan: "lokasi_salah",
      deskripsi: "Titik pin di peta berada di tengah sawah, seharusnya di depan ruko",
    };

    const parsed = reportSchema.safeParse(input);
    expect(parsed.success).toBe(true);
    expect(parsed.data.alasan).toBe("lokasi_salah");
  });

  it("menolak alasan laporan di luar enum yang ditentukan", () => {
    const input = {
      vendorId: "654321654321654321654321",
      alasan: "makanannya_asin", // tidak ada di enum
    };

    const parsed = reportSchema.safeParse(input);
    expect(parsed.success).toBe(false);
  });

  it("menolak jika ID warung kosong", () => {
    const input = {
      vendorId: "",
      alasan: "sudah_tutup",
    };

    const parsed = reportSchema.safeParse(input);
    expect(parsed.success).toBe(false);
  });

  it("menghitung peringkat leaderboard member yang paling sering mengajukan warung", () => {
    const dataPengajuan = [
      { userId: "u1", nama: "Ahmad", status: "approved" },
      { userId: "u2", nama: "Budi", status: "approved" },
      { userId: "u1", nama: "Ahmad", status: "pending" },
      { userId: "u1", nama: "Ahmad", status: "approved" },
      { userId: "u3", nama: "Citra", status: "approved" },
      { userId: "u2", nama: "Budi", status: "pending" },
    ];

    const hitung = {};
    dataPengajuan.forEach((item) => {
      if (!hitung[item.userId]) {
        hitung[item.userId] = { userId: item.userId, nama: item.nama, total: 0, diterima: 0 };
      }
      hitung[item.userId].total += 1;
      if (item.status === "approved") hitung[item.userId].diterima += 1;
    });

    const leaderboard = Object.values(hitung).sort((a, b) => b.total - a.total);

    expect(leaderboard[0].nama).toBe("Ahmad");
    expect(leaderboard[0].total).toBe(3);
    expect(leaderboard[0].diterima).toBe(2);

    expect(leaderboard[1].nama).toBe("Budi");
    expect(leaderboard[1].total).toBe(2);
  });
});
