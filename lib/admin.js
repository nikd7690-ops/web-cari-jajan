import mongoose from "mongoose";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";

// Role dibaca dari database, bukan dari JWT/session, agar perubahan role
// langsung berlaku dan token APK lama tidak bisa dipakai untuk admin.
export async function roleUser(userId) {
  if (!userId || !mongoose.Types.ObjectId.isValid(userId)) return null;

  await connectDB();

  const user = await User.findById(userId).select("role").lean();
  return user?.role || null;
}

export async function apakahAdmin(userId) {
  return (await roleUser(userId)) === "admin";
}