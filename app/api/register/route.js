import { NextResponse } from "next/server";
import { z } from "zod";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";

const registerSchema = z.object({
  nama: z.string().trim().min(1, "Nama wajib diisi"),
  email: z.string().trim().email("Email tidak valid").toLowerCase(),
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
    const user = await User.create({ name: nama, email, passwordHash });

    return NextResponse.json(
      {
        data: {
          id: user._id.toString(),
          name: user.name,
          email: user.email,
        },
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