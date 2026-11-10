import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import { isBuka } from "@/lib/jam";

export async function GET(request, { params }) {
  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json({ error: "ID warung tidak valid" }, { status: 400 });
  }

  try {
    await connectDB();

    const vendor = await Vendor.findById(id).select("-owner").lean();

    if (!vendor) {
      return NextResponse.json(
        { error: "Warung tidak ditemukan" },
        { status: 404 }
      );
    }

    return NextResponse.json({
      data: { ...vendor, buka: isBuka(vendor.jamBuka, vendor.jamTutup) },
    });
  } catch (error) {
    console.error("GET /api/vendors/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil data warung" },
      { status: 500 }
    );
  }
}
