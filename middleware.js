import { NextResponse } from "next/server";

// CORS agar APK (origin Capacitor, mis. https://localhost) bisa memanggil API Vercel.
// Pakai token (Authorization header), bukan cookie, jadi tidak perlu credentials.
function headerCORS(origin) {
  const h = new Headers();
  h.set("Access-Control-Allow-Origin", origin || "*");
  if (origin) h.set("Vary", "Origin");
  h.set(
    "Access-Control-Allow-Methods",
    "GET,POST,PUT,PATCH,DELETE,OPTIONS"
  );
  h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  h.set("Access-Control-Max-Age", "600");
  return h;
}

export function middleware(request) {
  const origin = request.headers.get("origin");

  if (request.method === "OPTIONS") {
    return new NextResponse(null, { status: 204, headers: headerCORS(origin) });
  }

  const res = NextResponse.next();
  headerCORS(origin).forEach((nilai, nama) => res.headers.set(nama, nilai));
  return res;
}

export const config = {
  matcher: "/api/:path*",
};