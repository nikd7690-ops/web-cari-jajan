import { verifikasiToken } from "@/lib/token";

// Baca token dari header Authorization: Bearer <token>.
export function userDariRequest(request) {
  const header = request.headers.get("authorization") || "";
  const cocok = header.match(/^Bearer\s+(.+)$/i);
  if (!cocok) return null;

  const data = verifikasiToken(cocok[1]);
  if (!data || !data.id) return null;

  return { id: data.id, name: data.name, email: data.email };
}