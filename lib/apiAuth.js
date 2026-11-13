import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { getSession } from "@/lib/auth";
import { userDariRequest } from "@/lib/authToken";
import { apakahAdmin } from "@/lib/admin";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import Vendor from "@/models/Vendor";

export async function requireUser(request) {
  let targetUser = null;

  if (request) {
    const dariToken = userDariRequest(request);
    if (dariToken?.id) {
      targetUser = dariToken;
    }
  }

  if (!targetUser) {
    const session = await getSession();
    if (session?.user?.id) {
      targetUser = session.user;
    }
  }

  if (!targetUser?.id) {
    return {
      user: null,
      response: NextResponse.json(
        { error: "Harus login terlebih dahulu" },
        { status: 401 }
      ),
    };
  }

  try {
    await connectDB();
    const dbUser = await User.findById(targetUser.id).select("isBanned role").lean();
    if (dbUser?.isBanned) {
      return {
        user: null,
        response: NextResponse.json(
          { error: "Akun Anda telah ditangguhkan/diblokir oleh admin karena melanggar ketentuan komunitas." },
          { status: 403 }
        ),
      };
    }
  } catch (err) {
    console.error("Gagal memeriksa status banned user:", err);
  }

  return { user: targetUser, response: null };
}

export async function requireAdmin(request) {
  const { user, response } = await requireUser(request);
  if (response) return { user: null, response };

  if (!(await apakahAdmin(user.id))) {
    return {
      user: null,
      response: NextResponse.json(
        { error: "Hanya admin yang boleh mengakses" },
        { status: 403 }
      ),
    };
  }

  return { user, response: null };
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

  const isAdmin = await apakahAdmin(userId);
  if (!isAdmin && (!vendor.owner || vendor.owner.toString() !== userId)) {
    return {
      vendor: null,
      error: NextResponse.json(
        {
          error:
            "Hanya pemilik warung (mitra) resmi atau admin yang berhak mengelola warung ini. Pengguna/member tidak dapat mengubah data warung yang telah diajukan.",
        },
        { status: 403 }
      ),
    };
  }

  return { vendor, error: null };
}