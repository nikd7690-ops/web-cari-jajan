import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import OtpVerification from "@/models/OtpVerification";
import { kirimEmailOtp } from "@/lib/email";

const registerSchema = z.object({
  nama: z.string().trim().min(1, "Nama wajib diisi"),
  email: z
    .string()
    .trim()
    .email("Email tidak valid")
    .toLowerCase()
    .refine((val) => val.endsWith("@gmail.com"), {
      message: "Hanya akun resmi @gmail.com yang diizinkan untuk mencegah akun palsu",
    }),
  password: z.string().min(8, "Password minimal 8 karakter"),
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

  const parsed = registerSchema.safeParse(body);

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

  const { nama, email, password } = parsed.data;

  try {
    await connectDB();

    const sudahAda = await User.findOne({ email });

    if (sudahAda) {
      return NextResponse.json(
        { error: "Email sudah terdaftar" },
        { status: 409 }
      );
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({
      name: nama,
      email,
      passwordHash,
      emailVerified: false,
    });

    // Buat kode OTP 6-Digit untuk verifikasi Gmail
    const kodeOtp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = await bcrypt.hash(kodeOtp, 8);
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 menit

    await OtpVerification.deleteMany({ email });
    await OtpVerification.create({ email, otpHash, expiresAt });

    const hasilKirim = await kirimEmailOtp(email, kodeOtp);

    return NextResponse.json(
      {
        data: {
          id: user._id.toString(),
          name: user.name,
          email: user.email,
          emailVerified: user.emailVerified,
        },
        pesan: "Akun berhasil dibuat. Kode verifikasi telah dikirim ke Gmail Anda.",
        devOtp: hasilKirim.devOtp,
      },
      { status: 201 }
    );
  } catch (error) {
    console.error("POST /api/register gagal:", error);
    return NextResponse.json(
      { error: "Gagal mendaftar, coba lagi" },
      { status: 500 }
    );
  }
}