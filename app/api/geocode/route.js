import { NextResponse } from "next/server";
import { cekRateLimit } from "@/lib/rateLimit";

const CACHE = global.__geocodeCache ?? (global.__geocodeCache = new Map());
const TTL = 24 * 60 * 60 * 1000;
const BATAS_CACHE = 200;

export async function GET(request) {
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";

  if (q.length < 3) {
    return NextResponse.json(
      { error: "Kata kunci minimal 3 huruf" },
      { status: 400 }
    );
  }

  const ip =
    request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "anon";
  const batas = cekRateLimit(`geocode:${ip}`, 20, 60 * 1000);

  if (!batas.ok) {
    return NextResponse.json(
      { error: `Terlalu banyak pencarian. Coba lagi dalam ${batas.sisaDetik} detik.` },
      { status: 429 }
    );
  }

  const kunci = q.toLowerCase();
  const tersimpan = CACHE.get(kunci);

  if (tersimpan && Date.now() - tersimpan.waktu < TTL) {
    return NextResponse.json({ data: tersimpan.data });
  }

  const url = new URL("https://nominatim.openstreetmap.org/search");
  url.searchParams.set("format", "jsonv2");
  url.searchParams.set("q", q);
  url.searchParams.set("countrycodes", "id");
  url.searchParams.set("limit", "5");

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8000);

  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": "CariJajan/1.0 (etalase UMKM hiperlokal)",
        "Accept-Language": "id",
        Accept: "application/json",
      },
      signal: controller.signal,
    });

    if (!res.ok) {
      throw new Error(`Nominatim ${res.status}`);
    }

    const json = await res.json();
    const data = (Array.isArray(json) ? json : [])
      .map((item) => ({
        nama: String(item.display_name ?? ""),
        lat: Number(item.lat),
        lng: Number(item.lon),
      }))
      .filter(
        (item) =>
          item.nama !== "" &&
          Number.isFinite(item.lat) &&
          Number.isFinite(item.lng)
      );

    CACHE.set(kunci, { waktu: Date.now(), data });

    if (CACHE.size > BATAS_CACHE) {
      CACHE.delete(CACHE.keys().next().value);
    }

    return NextResponse.json({ data });
  } catch (error) {
    console.error("GET /api/geocode gagal:", error);

    const pesan =
      error?.name === "AbortError"
        ? "Pencarian kawasan terlalu lama. Coba lagi."
        : "Gagal mencari kawasan. Coba lagi atau pilih dari daftar.";

    return NextResponse.json({ error: pesan }, { status: 502 });
  } finally {
    clearTimeout(timer);
  }
}