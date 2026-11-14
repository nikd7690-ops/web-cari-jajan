import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";

const resetPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .email("Format email tidak valid")
    .toLowerCase(),
  passwordBaru: z
    .string()
    .min(8, "Password baru minimal 8 karakter"),
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

  const parsed = resetPasswordSchema.safeParse(body);

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

  const { email, passwordBaru } = parsed.data;

  try {
    await connectDB();

    const user = await User.findOne({ email });

    if (!user) {
      return NextResponse.json(
        { error: "Email akun tidak terdaftar di Cari Jajan." },
        { status: 404 }
      );
    }

    if (user.isBanned) {
      return NextResponse.json(
        {
          error:
            "Akun Anda telah ditangguhkan/diblokir oleh admin dan tidak dapat dipulihkan.",
        },
        { status: 403 }
      );
    }

    // Hash password baru menggunakan bcrypt dengan salt rounds 10 (standar keamanan)
    const passwordHash = await bcrypt.hash(passwordBaru, 10);
    user.passwordHash = passwordHash;
    await user.save();

    return NextResponse.json({
      ok: true,
      message: "Password berhasil diperbarui. Silakan masuk menggunakan password baru Anda.",
    });
  } catch (error) {
    console.error("POST /api/auth/reset-password gagal:", error);
    return NextResponse.json(
      { error: "Gagal memperbarui password, coba lagi nanti." },
      { status: 500 }
    );
  }
}
