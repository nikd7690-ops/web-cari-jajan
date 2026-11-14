import { describe, it, expect } from "vitest";
import { z } from "zod";
import bcrypt from "bcryptjs";

describe("Fitur Lupa Password & Reset Password", () => {
  const resetPasswordSchema = z.object({
    email: z
      .string()
      .trim()
      .email("Format email tidak valid")
      .toLowerCase(),
    passwordBaru: z
      .string()
      .min(8, "Password baru minimal 8 karakter"),
  });

  it("menerima payload reset password yang valid", () => {
    const valid = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      passwordBaru: "passwordBaru123",
    });
    expect(valid.success).toBe(true);
    expect(valid.data.email).toBe("pemilikwarung@gmail.com");
    expect(valid.data.passwordBaru).toBe("passwordBaru123");
  });

  it("menolak email yang tidak valid", () => {
    const invalid = resetPasswordSchema.safeParse({
      email: "bukan-email",
      passwordBaru: "passwordBaru123",
    });
    expect(invalid.success).toBe(false);
  });

  it("menolak password baru yang kurang dari 8 karakter", () => {
    const shortPassword = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      passwordBaru: "pendek",
    });
    expect(shortPassword.success).toBe(false);
  });

  it("memastikan password baru selalu di-hash dengan bcrypt", async () => {
    const passwordBaru = "rahasiaBaru2026";
    const hash = await bcrypt.hash(passwordBaru, 10);

    expect(hash).not.toBe(passwordBaru);
    expect(hash.startsWith("$2a$") || hash.startsWith("$2b$")).toBe(true);

    const cocok = await bcrypt.compare(passwordBaru, hash);
    expect(cocok).toBe(true);

    const salah = await bcrypt.compare("passwordSalah", hash);
    expect(salah).toBe(false);
  });
});
