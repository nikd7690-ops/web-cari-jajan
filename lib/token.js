import jwt from "jsonwebtoken";

function secret() {
  const s = process.env.JWT_SECRET || process.env.NEXTAUTH_SECRET;
  if (!s) {
    throw new Error("JWT_SECRET (atau NEXTAUTH_SECRET) belum diatur");
  }
  return s;
}

export function buatToken(user) {
  return jwt.sign(
    { id: String(user.id), name: user.name || "", email: user.email || "" },
    secret(),
    { expiresIn: "30d" }
  );
}

export function verifikasiToken(token) {
  try {
    return jwt.verify(token, secret());
  } catch {
    return null;
  }
}