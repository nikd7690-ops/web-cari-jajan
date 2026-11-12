import process from "node:process";
import mongoose from "mongoose";
import { connectDB } from "../lib/mongodb.js";
import User from "../models/User.js";

for (const file of [".env", ".env.local"]) {
  try {
    process.loadEnvFile(file);
  } catch {
    // file opsional, variabel bisa datang dari environment shell
  }
}

const ROLE_VALID = ["user", "admin"];

// Pemakaian:
//   node scripts/set-role.js <email> <user|admin>
//   ADMIN_EMAIL=<email> node scripts/set-role.js admin
async function main() {
  const role = (process.argv[3] || process.argv[2] || "").trim().toLowerCase();
  const email = (process.argv[3] ? process.argv[2] : process.env.ADMIN_EMAIL || "")
    .trim()
    .toLowerCase();

  if (!email || !ROLE_VALID.includes(role)) {
    console.error(
      "Pemakaian: node scripts/set-role.js <email> <user|admin>\n" +
        "Atau: ADMIN_EMAIL=<email> node scripts/set-role.js <user|admin>"
    );
    process.exit(1);
  }

  await connectDB();

  const user = await User.findOneAndUpdate(
    { email },
    { $set: { role } },
    { returnDocument: "after" }
  ).select("name email role");

  if (!user) {
    console.error(`User dengan email ${email} tidak ditemukan.`);
    await mongoose.disconnect();
    process.exit(1);
  }

  console.log(`Role ${user.email} sekarang: ${user.role}`);

  await mongoose.disconnect();
}

main().catch(async (error) => {
  console.error("Set role gagal:", error);
  await mongoose.disconnect();
  process.exit(1);
});
