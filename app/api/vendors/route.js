import { NextResponse } from "next/server";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import { isBuka } from "@/lib/jam";

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
      { $project: { owner: 0 } },
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
