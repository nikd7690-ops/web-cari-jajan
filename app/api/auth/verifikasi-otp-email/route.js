import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import OtpVerification from "@/models/OtpVerification";

const verifikasiSchema = z.object({
  email: z
    .string()
    .trim()
    .email("Email tidak valid")
    .toLowerCase(),
  kodeOtp: z
    .string()
    .trim()
    .length(6, "Kode OTP harus 6 digit angka")
    .regex(/^\d{6}$/, "Kode OTP harus berupa angka"),
});

export async function POST(request) {
  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Body JSON tidak valid" }, { status: 400 });
  }

  const parsed = verifikasiSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Data tidak valid" },
      { status: 400 }
    );
  }

  const { email, kodeOtp } = parsed.data;

  try {
    await connectDB();

    const record = await OtpVerification.findOne({
      email,
      expiresAt: { $gt: new Date() },
    });

    if (!record) {
      return NextResponse.json(
        { error: "Kode OTP telah kedaluwarsa atau belum pernah diminta. Silakan minta kode baru." },
        { status: 400 }
      );
    }

    if (record.attempts >= 4) {
      await OtpVerification.deleteOne({ _id: record._id });
      return NextResponse.json(
        { error: "Terlalu banyak percobaan salah. Kode OTP telah dinonaktifkan demi keamanan. Silakan minta kode baru." },
        { status: 400 }
      );
    }

    const cocok = await bcrypt.compare(kodeOtp, record.otpHash);
    if (!cocok) {
      record.attempts += 1;
      await record.save();
      const sisa = 4 - record.attempts;
      return NextResponse.json(
        { error: `Kode OTP salah. Sisa kesempatan: ${sisa} kali.` },
        { status: 400 }
      );
    }

    // Kode cocok! Update status emailVerified di User dan hapus dokumen OTP
    await OtpVerification.deleteOne({ _id: record._id });
    const user = await User.findOneAndUpdate(
      { email },
      { emailVerified: true },
      { new: true }
    );

    return NextResponse.json({
      message: "Email @gmail.com berhasil diverifikasi!",
      emailVerified: true,
      user: {
        id: user._id.toString(),
        name: user.name,
        email: user.email,
        emailVerified: true,
      },
    });
  } catch (err) {
    console.error("POST /api/auth/verifikasi-otp-email gagal:", err);
    return NextResponse.json({ error: "Gagal memverifikasi OTP" }, { status: 500 });
  }
}
