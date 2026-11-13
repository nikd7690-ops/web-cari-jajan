import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import Vendor from "@/models/Vendor";
import { requireAdmin } from "@/lib/apiAuth";

export async function DELETE(request, { params }) {
  const { user: adminUser, response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json(
      { error: "ID member tidak valid" },
      { status: 400 }
    );
  }

  // Lindungi admin agar tidak menghapus akun dirinya sendiri
  if (adminUser?.id === id) {
    return NextResponse.json(
      { error: "Tidak dapat menghapus akun admin yang sedang aktif" },
      { status: 400 }
    );
  }

  try {
    await connectDB();

    const targetUser = await User.findById(id);
    if (!targetUser) {
      return NextResponse.json(
        { error: "Member tidak ditemukan" },
        { status: 404 }
      );
    }

    if (targetUser.role === "admin") {
      return NextResponse.json(
        { error: "Akun dengan peran admin tidak dapat dihapus" },
        { status: 403 }
      );
    }

    // Bersihkan referensi submittedBy pada warung yang pernah diajukan
    await Vendor.updateMany(
      { submittedBy: targetUser._id },
      { $set: { submittedBy: null } }
    );

    await User.findByIdAndDelete(id);

    return NextResponse.json({
      message: `Akun member ${targetUser.name || targetUser.email} berhasil dihapus permanen.`,
    });
  } catch (error) {
    console.error("DELETE /api/admin/members/[id] gagal:", error);
    return NextResponse.json(
      { error: "Gagal menghapus member" },
      { status: 500 }
    );
  }
}
