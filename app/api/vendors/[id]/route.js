import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import { isBuka } from "@/lib/jam";
import { requireUser, requireVendorOwner } from "@/lib/apiAuth";
import { apakahAdmin } from "@/lib/admin";
import { vendorUpdateSchema } from "@/lib/vendorSchema";

export async function GET(request, { params }) {
  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json({ error: "ID warung tidak valid" }, { status: 400 });
  }

  try {
    await connectDB();

    const vendor = await Vendor.findById(id).lean();

    if (!vendor) {
      return NextResponse.json(
        { error: "Warung tidak ditemukan" },
        { status: 404 }
      );
    }

    if (vendor.status !== "approved") {
      const { user, response } = await requireUser(request);
      const userId = user?.id;
      const pemilik =
        userId &&
        (vendor.owner?.toString() === userId ||
          vendor.submittedBy?.toString() === userId);
      const admin = userId ? await apakahAdmin(userId) : false;

      if (response || (!pemilik && !admin)) {
        return NextResponse.json(
          { error: "Warung tidak ditemukan" },
          { status: 404 }
        );
      }
    }

    const data = {
      ...vendor,
      buka: isBuka(vendor.jamBuka, vendor.jamTutup),
    };
    delete data.owner;
    delete data.submittedBy;

    return NextResponse.json({ data });
  } catch (error) {
    console.error("GET /api/vendors/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil data warung" },
      { status: 500 }
    );
  }
}

export async function PATCH(request, { params }) {
  const { id } = await params;

  const { user, response } = await requireUser(request);
  if (response) return response;

  let body;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Body JSON tidak valid" },
      { status: 400 }
    );
  }

  const parsed = vendorUpdateSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      {
        error: "Data tidak valid",
        details: parsed.error.issues.map((issue) => ({
          field: issue.path.join("."),
          message: issue.message,
        })),
      },
      { status: 400 }
    );
  }

  try {
    await connectDB();

    const { vendor, error } = await requireVendorOwner(id, user.id);
    if (error) return error;

    const d = parsed.data;
    const update = {};

    for (const key of [
      "namaToko",
      "kategori",
      "alamatPatokan",
      "jamBuka",
      "jamTutup",
      "nomorWA",
      "layananAntar",
      "foto",
      "menus",
    ]) {
      if (d[key] !== undefined) update[key] = d[key];
    }

    if (d.lat !== undefined && d.lng !== undefined) {
      update.lokasi = { type: "Point", coordinates: [d.lng, d.lat] };
    }

    // Setiap perubahan oleh penjual wajib dikurasi ulang.
    if (vendor.status !== "pending") {
      update.status = "pending";
    }

    const updated = await Vendor.findByIdAndUpdate(vendor._id, update, {
      returnDocument: "after",
      runValidators: true,
    });

    return NextResponse.json({ data: updated });
  } catch (error) {
    console.error("PATCH /api/vendors/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal memperbarui warung" },
      { status: 500 }
    );
  }
}

export async function DELETE(request, { params }) {
  const { id } = await params;

  const { user, response } = await requireUser(request);
  if (response) return response;

  try {
    await connectDB();

    const { vendor, error } = await requireVendorOwner(id, user.id);
    if (error) return error;

    await Vendor.findByIdAndDelete(vendor._id);

    return NextResponse.json({ ok: true });
  } catch (error) {
    console.error("DELETE /api/vendors/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal menghapus warung" },
      { status: 500 }
    );
  }
}

export async function PUT(request, context) {
  return PATCH(request, context);
}
