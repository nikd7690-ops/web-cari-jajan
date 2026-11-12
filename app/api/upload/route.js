import { NextResponse } from "next/server";
import { put } from "@vercel/blob";
import { requireUser } from "@/lib/apiAuth";

const TIPE_DIIZINKAN = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 2 * 1024 * 1024;

export async function POST(request) {
  const { user, response } = await requireUser(request);
  if (response) return response;

  let form;

  try {
    form = await request.formData();
  } catch {
    return NextResponse.json({ error: "Form tidak valid" }, { status: 400 });
  }

  const file = form.get("file");

  if (!file || typeof file === "string") {
    return NextResponse.json(
      { error: "File wajib diunggah" },
      { status: 400 }
    );
  }

  if (!TIPE_DIIZINKAN.includes(file.type)) {
    return NextResponse.json(
      { error: "Tipe file harus JPEG, PNG, atau WebP" },
      { status: 400 }
    );
  }

  if (file.size > MAX_BYTES) {
    return NextResponse.json(
      { error: "Ukuran file maksimal 2 MB" },
      { status: 400 }
    );
  }

  try {
    const blob = await put(`warung/${user.id}-${Date.now()}.jpg`, file, {
      access: "public",
      contentType: file.type,
    });

    return NextResponse.json({ url: blob.url }, { status: 201 });
  } catch (error) {
    console.error("POST /api/upload gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengunggah foto" },
      { status: 500 }
    );
  }
}