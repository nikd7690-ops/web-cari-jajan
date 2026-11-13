import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import User from "@/models/User";
import { requireAdmin } from "@/lib/apiAuth";
import { XP_PER_APPROVAL, XP_PER_LEVEL } from "@/lib/gamifikasi";

const aksiSchema = z.object({
  aksi: z.enum(["approve", "reject"]),
});

export async function PATCH(request, { params }) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json(
      { error: "ID warung tidak valid" },
      { status: 400 }
    );
  }

  let body;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Body JSON tidak valid" },
      { status: 400 }
    );
  }

  const parsed = aksiSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      { error: "Aksi harus approve atau reject" },
      { status: 400 }
    );
  }

  try {
    await connectDB();

    if (parsed.data.aksi === "reject") {
      const vendor = await Vendor.findOneAndUpdate(
        { _id: id, status: "pending" },
        { $set: { status: "rejected" } },
        { returnDocument: "after" }
      );

      if (!vendor) {
        const ada = await Vendor.exists({ _id: id });
        return NextResponse.json(
          {
            error: ada
              ? "Hanya warung berstatus pending yang bisa ditolak"
              : "Warung tidak ditemukan",
          },
          { status: ada ? 409 : 404 }
        );
      }

      return NextResponse.json({ data: { vendor } });
    }

    // Guard status: XP hanya diberikan saat transisi ke approved,
    // sehingga approve berulang tidak menambah XP.
    const vendor = await Vendor.findOneAndUpdate(
      { _id: id, status: { $ne: "approved" } },
      { $set: { status: "approved" } },
      { returnDocument: "after" }
    );

    if (!vendor) {
      const ada = await Vendor.exists({ _id: id });
      return NextResponse.json(
        {
          error: ada ? "Warung sudah disetujui" : "Warung tidak ditemukan",
        },
        { status: ada ? 409 : 404 }
      );
    }

    const pemilik = vendor.submittedBy || vendor.owner;
    if (!pemilik) {
      await Vendor.updateOne(
        { _id: vendor._id, status: "approved" },
        { $set: { status: "pending" } }
      );
      return NextResponse.json(
        { error: "Warung tidak memiliki user pengaju yang valid" },
        { status: 409 }
      );
    }

    let hadiah;

    try {
      hadiah = await User.findByIdAndUpdate(
        pemilik,
        [
          {
            $set: {
              xp: {
                $add: [{ $ifNull: ["$xp", 0] }, XP_PER_APPROVAL],
              },
            },
          },
          {
            $set: {
              level: {
                $add: [
                  1,
                  {
                    $floor: {
                      $divide: ["$xp", XP_PER_LEVEL],
                    },
                  },
                ],
              },
            },
          },
        ],
        {
          returnDocument: "after",
          projection: { name: 1, xp: 1, level: 1 },
        }
      ).lean();
    } catch (error) {
      await Vendor.updateOne(
        { _id: vendor._id, status: "approved" },
        { $set: { status: "pending" } }
      );
      throw error;
    }

    if (!hadiah) {
      await Vendor.updateOne(
        { _id: vendor._id, status: "approved" },
        { $set: { status: "pending" } }
      );
      return NextResponse.json(
        { error: "User pengaju warung tidak ditemukan" },
        { status: 409 }
      );
    }

    return NextResponse.json({ data: { vendor, user: hadiah } });
  } catch (error) {
    console.error("PATCH /api/admin/vendors/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal memperbarui status warung" },
      { status: 500 }
    );
  }
}

export async function DELETE(request, { params }) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json(
      { error: "ID warung tidak valid" },
      { status: 400 }
    );
  }

  try {
    await connectDB();
    const vendor = await Vendor.findByIdAndDelete(id);
    if (!vendor) {
      return NextResponse.json({ error: "Warung tidak ditemukan" }, { status: 404 });
    }
    return NextResponse.json({ message: "Warung berhasil dihapus oleh admin" });
  } catch (error) {
    console.error("DELETE /api/admin/vendors/[id] gagal:", error);
    return NextResponse.json({ error: "Gagal menghapus warung" }, { status: 500 });
  }
}
