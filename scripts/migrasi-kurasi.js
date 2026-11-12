import process from "node:process";
import mongoose from "mongoose";
import { connectDB } from "../lib/mongodb.js";
import User from "../models/User.js";
import Vendor from "../models/Vendor.js";

for (const file of [".env", ".env.local"]) {
  try {
    process.loadEnvFile(file);
  } catch {
    // file opsional, variabel bisa datang dari environment shell
  }
}

// Migrasi idempoten: hanya mengisi field yang belum ada.
// Warung lama dianggap sudah approved agar peta tidak kosong
// setelah API publik mulai memfilter status.
async function main() {
  await connectDB();

  const role = await User.collection.updateMany(
    { role: { $exists: false } },
    { $set: { role: "user" } }
  );
  const xp = await User.collection.updateMany(
    { xp: { $exists: false } },
    { $set: { xp: 0 } }
  );
  const level = await User.collection.updateMany(
    { level: { $exists: false } },
    { $set: { level: 1 } }
  );

  const status = await Vendor.collection.updateMany(
    { $or: [{ status: { $exists: false } }, { status: null }] },
    { $set: { status: "approved" } }
  );
  const submittedBy = await Vendor.collection.updateMany(
    { $or: [{ submittedBy: { $exists: false } }, { submittedBy: null }] },
    [{ $set: { submittedBy: "$owner" } }]
  );

  console.log("Migrasi selesai:");
  console.log(`- User role diisi: ${role.modifiedCount}`);
  console.log(`- User xp diisi: ${xp.modifiedCount}`);
  console.log(`- User level diisi: ${level.modifiedCount}`);
  console.log(`- Vendor status diisi: ${status.modifiedCount}`);
  console.log(`- Vendor submittedBy diisi: ${submittedBy.modifiedCount}`);

  await mongoose.disconnect();
}

main().catch(async (error) => {
  console.error("Migrasi gagal:", error);
  await mongoose.disconnect();
  process.exit(1);
});
