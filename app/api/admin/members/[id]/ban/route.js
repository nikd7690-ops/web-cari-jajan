import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import { requireAdmin } from "@/lib/apiAuth";

const banSchema = z.object({
  banned: z.boolean(),
  reason: z.string().optional().default(""),
});

export async function POST(request, { params }) {
  const { user: adminUser, response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json({ error: "ID member tidak valid" }, { status: 400 });
  }

  if (adminUser.id === id) {
    return NextResponse.json(
      { error: "Anda tidak dapat memblokir akun Anda sendiri" },
      { status: 400 }
    );
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Body JSON tidak valid" }, { status: 400 });
  }

  const parsed = banSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Data status banned tidak valid" }, { status: 400 });
  }

  try {
    await connectDB();

    const targetUser = await User.findById(id);
    if (!targetUser) {
      return NextResponse.json({ error: "Member tidak ditemukan" }, { status: 404 });
    }

    if (targetUser.role === "admin") {
      return NextResponse.json(
        { error: "Tidak dapat memblokir akun sesama Administrator" },
        { status: 403 }
      );
    }

    targetUser.isBanned = parsed.data.banned;
    targetUser.bannedAt = parsed.data.banned ? new Date() : null;
    targetUser.bannedReason = parsed.data.banned ? parsed.data.reason : "";
    await targetUser.save();

    return NextResponse.json({
      message: parsed.data.banned
        ? `Akun ${targetUser.email} berhasil diblokir.`
        : `Blokir akun ${targetUser.email} berhasil dicabut.`,
      data: {
        id: targetUser._id.toString(),
        isBanned: targetUser.isBanned,
        bannedAt: targetUser.bannedAt,
        bannedReason: targetUser.bannedReason,
      },
    });
  } catch (error) {
    console.error("POST /api/admin/members/[id]/ban gagal:", error);
    return NextResponse.json(
      { error: "Gagal memproses pemblokiran member" },
      { status: 500 }
    );
  }
}
