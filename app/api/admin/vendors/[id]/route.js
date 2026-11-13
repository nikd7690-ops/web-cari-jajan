import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import User from "@/models/User";
import { requireAdmin } from "@/lib/apiAuth";
import { XP_PER_APPROVAL, XP_PER_LEVEL } from "@/lib/gamifikasi";

const aksiSchema = z
  .object({
    aksi: z.enum(["approve", "reject"]).optional(),
    action: z.enum(["approve", "reject"]).optional(),
  })
  .refine((data) => data.aksi || data.action, {
    message: "Aksi harus approve atau reject",
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

  const aksi = parsed.data.aksi || parsed.data.action;

  try {
    await connectDB();

    if (aksi === "reject") {
      const vendor = await Vendor.findOneAndUpdate(
        { _id: id, status: { $ne: "rejected" } },
        { $set: { status: "rejected" } },
        { returnDocument: "after" }
      );

      if (!vendor) {
        const ada = await Vendor.exists({ _id: id });
        return NextResponse.json(
          {
            error: ada
              ? "Warung sudah berstatus ditolak"
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
    let hadiah = null;

    if (pemilik) {
      try {
        const userObj = await User.findById(pemilik);
        if (userObj) {
          const xpBaru = (userObj.xp || 0) + XP_PER_APPROVAL;
          const levelBaru = Math.floor(xpBaru / XP_PER_LEVEL) + 1;
          userObj.xp = xpBaru;
          userObj.level = levelBaru;
          await userObj.save();
          hadiah = {
            id: userObj._id.toString(),
            name: userObj.name,
            xp: userObj.xp,
            level: userObj.level,
          };
        }
      } catch (rewardErr) {
        console.warn("Gagal memberikan XP hadiah kepada user pengaju:", rewardErr);
      }
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
