import process from "node:process";
import mongoose from "mongoose";
import bcrypt from "bcryptjs";
import { connectDB } from "../lib/mongodb.js";
import User from "../models/User.js";
import Vendor from "../models/Vendor.js";

// Muat environment variables (.env.local atau .env)
for (const file of [".env", ".env.local"]) {
  try {
    process.loadEnvFile(file);
  } catch {
    // opsional
  }
}

const PASSWORD_DEFAULT = process.env.JURI_PASSWORD || "password123";

const AKUN_JURI = [
  {
    name: "Juri Member (Pembeli)",
    email: "member@gmail.com",
    role: "user",
    tipeAkun: "pengguna",
    xp: 150,
    level: 2,
    deskripsi: "Akun pembeli / warga komunitas untuk eksplorasi peta, rute jajan, kirim laporan, dan gamifikasi XP.",
  },
  {
    name: "Juri Pemilik Warung (Mitra)",
    email: "pemilikwarung@gmail.com",
    role: "user",
    tipeAkun: "mitra",
    xp: 250,
    level: 3,
    deskripsi: "Akun penjual UMKM untuk mengelola menu, jam buka, dan mengaktifkan promo diskon Zero Waste Food.",
  },
  {
    name: "Juri Admin (Kurator)",
    email: "admin@gmail.com",
    role: "admin",
    tipeAkun: "pengguna",
    xp: 500,
    level: 5,
    deskripsi: "Akun kurator admin untuk meninjau warung baru, memeriksa laporan ketidaksesuaian titik peta, dan kelola member.",
  },
];

async function main() {
  console.log("==================================================");
  console.log("  Inisialisasi Akun Dummy untuk Evaluasi Juri");
  console.log("==================================================");

  await connectDB();

  const passwordHash = await bcrypt.hash(PASSWORD_DEFAULT, 10);
  const hasilAkun = [];

  for (const item of AKUN_JURI) {
    const user = await User.findOneAndUpdate(
      { email: item.email.toLowerCase() },
      {
        $set: {
          name: item.name,
          email: item.email.toLowerCase(),
          passwordHash,
          role: item.role,
          tipeAkun: item.tipeAkun,
          emailVerified: true,
          isBanned: false,
          xp: item.xp,
          level: item.level,
        },
      },
      { upsert: true, returnDocument: "after", setDefaultsOnInsert: true }
    );
    hasilAkun.push({ ...item, id: user._id.toString() });
  }

  // Cari akun pemilik warung
  const penjual = hasilAkun.find((a) => a.email === "pemilikwarung@gmail.com");

  if (penjual) {
    // Hubungkan atau perbarui satu warung contoh agar dimiliki akun pemilik warung
    // dan pastikan memiliki promo Zero Waste (Diskon 35%) agar pop-up diskon langsung muncul di peta
    const warungAda = await Vendor.findOne({
      $or: [{ owner: penjual.id }, { status: "approved" }],
    });

    if (warungAda) {
      await Vendor.findByIdAndUpdate(warungAda._id, {
        $set: {
          owner: penjual.id,
          status: "approved",
          zeroWaste: {
            aktif: true,
            diskonPersen: 35,
            keterangan: "Promo sore hemat! Diskon 35% untuk porsi jajan hari ini agar tidak mubazir.",
          },
        },
      });
      console.log(`[OK] Warung '${warungAda.namaToko}' dihubungkan ke ${penjual.email} dengan PROMO ZERO WASTE (Diskon 35%).`);
    } else {
      // Jika belum ada warung di database, buat 1 warung contoh dengan promo diskon
      await Vendor.create({
        owner: penjual.id,
        submittedBy: penjual.id,
        status: "approved",
        namaToko: "Warung Bu Siti (Demo Diskon)",
        kategori: "warung",
        lokasi: { type: "Point", coordinates: [113.7, -8.165] },
        alamatPatokan: "Jl. Kalimantan No. 10, Jember",
        jamBuka: "07:00",
        jamTutup: "21:00",
        nomorWA: "081234567890",
        layananAntar: true,
        foto: "",
        menus: [
          { namaMenu: "Nasi Uduk Spesial", harga: 12000, tersedia: true },
          { namaMenu: "Lontong Sayur", harga: 10000, tersedia: true },
        ],
        zeroWaste: {
          aktif: true,
          diskonPersen: 35,
          keterangan: "Promo sore hemat! Diskon 35% untuk porsi jajan hari ini agar tidak mubazir.",
        },
      });
      console.log(`[OK] Warung contoh demo berhasil dibuat dan dihubungkan ke ${penjual.email} dengan Diskon 35%.`);
    }
  }

  console.log("\nBerhasil menyiapkan 3 akun dummy untuk juri:");
  hasilAkun.forEach((a, idx) => {
    console.log(`\n${idx + 1}. ${a.name}`);
    console.log(`   Email    : ${a.email}`);
    console.log(`   Password : ${PASSWORD_DEFAULT}`);
    console.log(`   Role     : ${a.role} | Tipe: ${a.tipeAkun}`);
    console.log(`   Info     : ${a.deskripsi}`);
  });

  console.log("\n==================================================");
  console.log("  Siap digunakan di Web maupun Native Android!");
  console.log("==================================================");

  await mongoose.disconnect();
  process.exit(0);
}

main().catch(async (error) => {
  console.error("Gagal menyiapkan akun juri:", error);
  await mongoose.disconnect();
  process.exit(1);
});
