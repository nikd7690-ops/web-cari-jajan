import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import OtpVerification from "@/models/OtpVerification";
import { kirimEmailOtp } from "@/lib/email";
import { cekRateLimit } from "@/lib/rateLimit";

const kirimSchema = z.object({
  email: z
    .string()
    .trim()
    .email("Email tidak valid")
    .toLowerCase()
    .refine((val) => val.endsWith("@gmail.com"), {
      message: "Hanya akun resmi @gmail.com yang diizinkan",
    }),
});

export async function POST(request) {
  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Body JSON tidak valid" }, { status: 400 });
  }

  const parsed = kirimSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Email tidak valid" },
      { status: 400 }
    );
  }

  const { email } = parsed.data;

  // Rate limit: maksimal 1 permintaan kirim ulang per 60 detik per email
  const batas = cekRateLimit(`kirim-otp:${email}`, 1, 60 * 1000);
  if (!batas.ok) {
    return NextResponse.json(
      { error: `Mohon tunggu ${batas.sisaDetik} detik sebelum meminta kode OTP baru.` },
      { status: 429 }
    );
  }

  try {
    await connectDB();

    const user = await User.findOne({ email });
    if (!user) {
      return NextResponse.json({ error: "Email belum terdaftar" }, { status: 404 });
    }

    if (user.emailVerified) {
      return NextResponse.json(
        { message: "Email ini sudah terverifikasi sebelumnya.", emailVerified: true },
        { status: 200 }
      );
    }

    const kodeOtp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = await bcrypt.hash(kodeOtp, 8);
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 menit

    await OtpVerification.deleteMany({ email });
    await OtpVerification.create({ email, otpHash, expiresAt });

    const hasilKirim = await kirimEmailOtp(email, kodeOtp);

    return NextResponse.json({
      message: "Kode OTP baru telah dikirim ke alamat Gmail Anda.",
      devOtp: hasilKirim.devOtp,
    });
  } catch (err) {
    console.error("POST /api/auth/kirim-otp-email gagal:", err);
    return NextResponse.json({ error: "Gagal mengirim OTP" }, { status: 500 });
  }
}
