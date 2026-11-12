import { NextResponse } from "next/server";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import { requireUser } from "@/lib/apiAuth";
import { isBuka } from "@/lib/jam";

export async function GET(request) {
  const { user, response } = await requireUser(request);
  if (response) return response;

  try {
    await connectDB();

    const vendors = await Vendor.find({ owner: user.id })
      .sort({ createdAt: -1 })
      .lean();

    const data = vendors.map((vendor) => ({
      ...vendor,
      buka: isBuka(vendor.jamBuka, vendor.jamTutup),
    }));

    return NextResponse.json({ data });
  } catch (error) {
    console.error("GET /api/vendors/mine gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil data warung" },
      { status: 500 }
    );
  }
}