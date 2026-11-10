import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { getSession } from "@/lib/auth";
import Vendor from "@/models/Vendor";

export async function requireUser() {
  const session = await getSession();

  if (!session?.user?.id) {
    return {
      user: null,
      response: NextResponse.json(
        { error: "Harus login terlebih dahulu" },
        { status: 401 }
      ),
    };
  }

  return { user: session.user, response: null };
}

export async function requireVendorOwner(id, userId) {
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return {
      vendor: null,
      error: NextResponse.json(
        { error: "ID warung tidak valid" },
        { status: 400 }
      ),
    };
  }

  const vendor = await Vendor.findById(id);

  if (!vendor) {
    return {
      vendor: null,
      error: NextResponse.json(
        { error: "Warung tidak ditemukan" },
        { status: 404 }
      ),
    };
  }

  if (vendor.owner.toString() !== userId) {
    return {
      vendor: null,
      error: NextResponse.json(
        { error: "Tidak boleh mengubah warung milik penjual lain" },
        { status: 403 }
      ),
    };
  }

  return { vendor, error: null };
}