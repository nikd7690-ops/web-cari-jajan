import { NextResponse } from "next/server";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import User from "@/models/User";
import { isBuka } from "@/lib/jam";
import { requireUser } from "@/lib/apiAuth";
import { vendorInputSchema } from "@/lib/vendorSchema";

function hitungJarakMeter(lat1, lon1, lat2, lon2) {
  const R = 6371000; // meter
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

const querySchema = z.object({
  lat: z.coerce
    .number({ error: "lat wajib berupa angka" })
    .min(-90, "lat minimal -90")
    .max(90, "lat maksimal 90"),
  lng: z.coerce
    .number({ error: "lng wajib berupa angka" })
    .min(-180, "lng minimal -180")
    .max(180, "lng maksimal 180"),
  radius: z.coerce
    .number({ error: "radius wajib berupa angka" })
    .min(0, "radius minimal 0 meter")
    .max(10000, "radius maksimal 10000 meter")
    .default(2000),
});

export async function GET(request) {
  const params = request.nextUrl.searchParams;

  const parsed = querySchema.safeParse({
    lat: params.get("lat") ?? undefined,
    lng: params.get("lng") ?? undefined,
    radius: params.get("radius") ?? undefined,
  });

  if (!parsed.success) {
    return NextResponse.json(
      {
        error: "Parameter tidak valid",
        details: parsed.error.issues.map((issue) => ({
          field: issue.path.join("."),
          message: issue.message,
        })),
      },
      { status: 400 }
    );
  }

  const { lat, lng, radius } = parsed.data;

  try {
    await connectDB();

    const vendors = await Vendor.aggregate([
      {
        $geoNear: {
          near: { type: "Point", coordinates: [lng, lat] },
          key: "lokasi",
          distanceField: "jarak",
          maxDistance: radius,
          spherical: true,
        },
      },
      { $match: { status: "approved" } },
      { $project: { owner: 0, submittedBy: 0 } },
    ]);

    const data = vendors.map((vendor) => ({
      ...vendor,
      jarak: Math.round(vendor.jarak),
      buka: isBuka(vendor.jamBuka, vendor.jamTutup),
    }));

    return NextResponse.json({ data });
  } catch (error) {
    console.error("GET /api/vendors gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil data warung" },
      { status: 500 }
    );
  }
}

export async function POST(request) {
  const { user, response } = await requireUser(request);
  if (response) return response;

  let body;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Body JSON tidak valid" },
      { status: 400 }
    );
  }

  const parsed = vendorInputSchema.safeParse(body);

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

  const d = parsed.data;

  try {
    await connectDB();

    const userDb = await User.findById(user.id);
    if (userDb && !userDb.emailVerified && userDb.role !== "admin") {
      return NextResponse.json(
        {
          error:
            "Akun wajib memverifikasi alamat @gmail.com terlebih dahulu sebelum mendaftarkan warung.",
        },
        { status: 403 }
      );
    }

    // ── Validasi Proof of Presence (Maksimal Radius 50 Meter) ─────────────
    let jarakPengajuanMeter = 0;
    if (userDb?.role !== "admin") {
      if (typeof d.userLat !== "number" || typeof d.userLng !== "number") {
        return NextResponse.json(
          {
            error:
              "Koordinat GPS perangkat fisik (Proof of Presence) wajib disertakan untuk memverifikasi lokasi nyata warung.",
          },
          { status: 400 }
        );
      }

      const jarak = hitungJarakMeter(d.userLat, d.userLng, d.lat, d.lng);
      if (jarak > 50.0) {
        return NextResponse.json(
          {
            error: `Proof of Presence Gagal: Posisi GPS fisik Anda berjarak ${Math.round(jarak)} meter dari titik warung. Anda harus berada di lokasi fisik warung (maksimal radius 50 meter) demi mencegah pendaftaran fiktif.`,
          },
          { status: 400 }
        );
      }
      jarakPengajuanMeter = Math.round(jarak);
    }

    const vendor = await Vendor.create({
      owner: user.id,
      submittedBy: user.id,
      status: "pending",
      namaToko: d.namaToko,
      kategori: d.kategori,
      lokasi: { type: "Point", coordinates: [d.lng, d.lat] },
      alamatPatokan: d.alamatPatokan,
      jamBuka: d.jamBuka,
      jamTutup: d.jamTutup,
      nomorWA: d.nomorWA,
      layananAntar: d.layananAntar,
      foto: d.foto,
      menus: d.menus,
      jarakPengajuanMeter,
    });

    return NextResponse.json({ data: vendor }, { status: 201 });
  } catch (error) {
    console.error("POST /api/vendors gagal:", error);
    return NextResponse.json(
      { error: "Gagal menyimpan warung" },
      { status: 500 }
    );
  }
}
