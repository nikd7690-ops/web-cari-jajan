import { NextResponse } from "next/server";
import { z } from "zod";
import crypto from "crypto";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";

const requestOtpSchema = z.object({
  email: z
    .string()
    .trim()
    .email("Format email tidak valid")
    .toLowerCase(),
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

  const parsed = requestOtpSchema.safeParse(body);

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

  const { email } = parsed.data;

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

    // Generate 6-digit OTP kriptografis aman
    const otp = crypto.randomInt(100000, 1000000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 menit

    user.otpCode = otp;
    user.otpExpiresAt = expiresAt;
    await user.save();

    return NextResponse.json({
      ok: true,
      message: "Kode OTP 6-digit berhasil dibuat. Berlaku selama 10 menit.",
      demoOtp: otp,
    });
  } catch (error) {
    console.error("POST /api/auth/request-otp gagal:", error);
    return NextResponse.json(
      { error: "Gagal meminta kode OTP, coba lagi nanti." },
      { status: 500 }
    );
  }
}
