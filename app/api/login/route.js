import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import { buatToken } from "@/lib/token";

const loginSchema = z.object({
  email: z.string().trim().email("Email tidak valid").toLowerCase(),
  password: z.string().min(1, "Password wajib diisi"),
});

export async function POST(request) {
  let body;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Body JSON tidak valid" },
      { status: 400 }
    );
  }

  const parsed = loginSchema.safeParse(body);

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

  const { email, password } = parsed.data;

  try {
    await connectDB();

    const user = await User.findOne({ email });

    if (!user) {
      return NextResponse.json(
        { error: "Email atau password salah" },
        { status: 401 }
      );
    }

    const cocok = await bcrypt.compare(password, user.passwordHash);

    if (!cocok) {
      return NextResponse.json(
        { error: "Email atau password salah" },
        { status: 401 }
      );
    }

    if (user.isBanned) {
      return NextResponse.json(
        {
          error:
            "Akun Anda telah ditangguhkan/diblokir oleh admin karena melanggar ketentuan komunitas.",
        },
        { status: 403 }
      );
    }

    const token = buatToken({
      id: user._id,
      name: user.name,
      email: user.email,
      role: user.role || "user",
      tipeAkun: user.tipeAkun || "pengguna",
    });

    return NextResponse.json({
      token,
      user: {
        id: user._id.toString(),
        name: user.name,
        email: user.email,
        role: user.role || "user",
        tipeAkun: user.tipeAkun || "pengguna",
        emailVerified: user.emailVerified ?? false,
        xp: user.xp ?? 0,
        level: user.level ?? 1,
      },
    });
  } catch (error) {
    console.error("POST /api/login gagal:", error);
    return NextResponse.json(
      { error: "Gagal masuk, coba lagi" },
      { status: 500 }
    );
  }
}