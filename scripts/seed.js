import process from "node:process";
import mongoose from "mongoose";
import bcrypt from "bcryptjs";
import { connectDB } from "../lib/mongodb.js";
import User from "../models/User.js";
import Vendor from "../models/Vendor.js";

try {
  process.loadEnvFile(".env");
} catch {
  // .env opsional, variabel bisa datang dari environment shell
}

const demoEmail = process.env.DEMO_EMAIL;
const demoPassword = process.env.DEMO_PASSWORD;

if (!demoEmail || !demoPassword) {
  console.error("DEMO_EMAIL dan DEMO_PASSWORD wajib diisi di environment.");
  process.exit(1);
}

const WARUNG_CONTOH = [
  {
    namaToko: "Warung Bu Siti",
    kategori: "warung",
    coordinates: [113.7, -8.165],
    alamatPatokan: "Jl. Kalimantan, dekat Masjid Agung Jember",
    jamBuka: "06:00",
    jamTutup: "21:00",
    nomorWA: "081100000001",
    layananAntar: true,
    foto: "",
    menus: [
      { namaMenu: "Nasi Uduk", harga: 8000, tersedia: true },
      { namaMenu: "Tempe Orek", harga: 3000, tersedia: true },
      { namaMenu: "Es Teh", harga: 3000, tersedia: true },
    ],
  },
  {
    namaToko: "PKL Tahu Petis Pak Jo",
    kategori: "pkl",
    coordinates: [113.705, -8.17],
    alamatPatokan: "Alun-alun Jember, sisi barat",
    jamBuka: "16:00",
    jamTutup: "23:00",
    nomorWA: "081100000002",
    layananAntar: false,
    foto: "",
    menus: [
      { namaMenu: "Tahu Petis", harga: 2000, tersedia: true },
      { namaMenu: "Tahu Isi", harga: 2500, tersedia: true },
    ],
  },
  {
    namaToko: "Cafe Kopi Rakyat",
    kategori: "cafe",
    coordinates: [113.699, -8.16],
    alamatPatokan: "Jl. Gajah Mada No. 12",
    jamBuka: "10:00",
    jamTutup: "23:00",
    nomorWA: "081100000003",
    layananAntar: true,
    foto: "",
    menus: [
      { namaMenu: "Kopi Tubruk", harga: 10000, tersedia: true },
      { namaMenu: "Kopi Susu", harga: 15000, tersedia: true },
      { namaMenu: "Roti Bakar", harga: 12000, tersedia: false },
    ],
  },
  {
    namaToko: "Warung Nasi Pecel Mak Rini",
    kategori: "warung",
    coordinates: [113.708, -8.175],
    alamatPatokan: "Jl. Mastrip, depan Pasar Tanjung",
    jamBuka: "06:30",
    jamTutup: "14:00",
    nomorWA: "081100000004",
    layananAntar: true,
    foto: "",
    menus: [
      { namaMenu: "Nasi Pecel", harga: 9000, tersedia: true },
      { namaMenu: "Nasi Rawon", harga: 15000, tersedia: true },
      { namaMenu: "Pisang Goreng", harga: 2000, tersedia: true },
    ],
  },
  {
    namaToko: "PKL Sate Ayam Pak Anto",
    kategori: "pkl",
    coordinates: [113.695, -8.172],
    alamatPatokan: "Jl. Sultan Agung, dekat Tugu Jember",
    jamBuka: "17:00",
    jamTutup: "02:00",
    nomorWA: "081100000005",
    layananAntar: false,
    foto: "",
    menus: [
      { namaMenu: "Sate Ayam (10 tusuk)", harga: 20000, tersedia: true },
      { namaMenu: "Sate Kambing (10 tusuk)", harga: 35000, tersedia: true },
      { namaMenu: "Lontong", harga: 3000, tersedia: true },
    ],
  },
];

async function main() {
  await connectDB();

  const passwordHash = await bcrypt.hash(demoPassword, 10);

  const user = await User.findOneAndUpdate(
    { email: demoEmail.toLowerCase() },
    { name: "Penjual Demo", email: demoEmail.toLowerCase(), passwordHash },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  await Vendor.deleteMany({ owner: user._id });

  const vendors = WARUNG_CONTOH.map((warung) => ({
    owner: user._id,
    namaToko: warung.namaToko,
    kategori: warung.kategori,
    lokasi: { type: "Point", coordinates: warung.coordinates },
    alamatPatokan: warung.alamatPatokan,
    jamBuka: warung.jamBuka,
    jamTutup: warung.jamTutup,
    nomorWA: warung.nomorWA,
    layananAntar: warung.layananAntar,
    foto: warung.foto,
    menus: warung.menus,
  }));

  await Vendor.insertMany(vendors);

  console.log(`Akun demo: ${user.email}`);
  console.log(`Berhasil menyisipkan ${vendors.length} warung contoh di Jember.`);

  await mongoose.disconnect();
}

main().catch(async (error) => {
  console.error("Seed gagal:", error);
  await mongoose.disconnect();
  process.exit(1);
});
