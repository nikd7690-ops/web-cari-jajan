import { NextResponse } from "next/server";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import { requireUser } from "@/lib/apiAuth";

export async function GET(request) {
  const { user, response } = await requireUser(request);

  if (response) {
    return NextResponse.json({ user: null }, { status: 200 });
  }

  try {
    await connectDB();

    const profil = await User.findById(user.id)
      .select("name email role xp level")
      .lean();

    if (!profil) {
      return NextResponse.json({ user: null }, { status: 200 });
    }

    return NextResponse.json({
      user: {
        id: profil._id.toString(),
        name: profil.name,
        email: profil.email,
        role: profil.role || "user",
        xp: profil.xp ?? 0,
        level: profil.level ?? 1,
      },
    });
  } catch (error) {
    console.error("GET /api/session gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil profil" },
      { status: 500 }
    );
  }
}