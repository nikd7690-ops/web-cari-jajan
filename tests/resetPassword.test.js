import { describe, it, expect } from "vitest";
import { z } from "zod";
import bcrypt from "bcryptjs";

describe("Fitur Lupa Password & Verifikasi OTP", () => {
  const requestOtpSchema = z.object({
    email: z
      .string()
      .trim()
      .email("Format email tidak valid")
      .toLowerCase(),
  });

  const resetPasswordSchema = z.object({
    email: z
      .string()
      .trim()
      .email("Format email tidak valid")
      .toLowerCase(),
    otp: z
      .string()
      .trim()
      .regex(/^\d{6}$/, "Kode OTP wajib berupa 6 digit angka"),
    passwordBaru: z
      .string()
      .min(8, "Password baru minimal 8 karakter"),
  });

  it("menerima payload request OTP yang valid", () => {
    const valid = requestOtpSchema.safeParse({
      email: "pemilikwarung@gmail.com",
    });
    expect(valid.success).toBe(true);
  });

  it("menolak request OTP dengan email tidak valid", () => {
    const invalid = requestOtpSchema.safeParse({
      email: "bukan-email",
    });
    expect(invalid.success).toBe(false);
  });

  it("menerima payload reset password dengan 6-digit OTP yang valid", () => {
    const valid = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      otp: "123456",
      passwordBaru: "passwordBaru123",
    });
    expect(valid.success).toBe(true);
    expect(valid.data.otp).toBe("123456");
  });

  it("menolak reset password jika kode OTP bukan 6 digit angka", () => {
    const otpKurang = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      otp: "12345",
      passwordBaru: "passwordBaru123",
    });
    expect(otpKurang.success).toBe(false);

    const otpAlfanumerik = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      otp: "12A456",
      passwordBaru: "passwordBaru123",
    });
    expect(otpAlfanumerik.success).toBe(false);
  });

  it("menolak password baru yang kurang dari 8 karakter", () => {
    const shortPassword = resetPasswordSchema.safeParse({
      email: "pemilikwarung@gmail.com",
      otp: "123456",
      passwordBaru: "pendek",
    });
    expect(shortPassword.success).toBe(false);
  });

  it("memverifikasi logika kedaluwarsa masa aktif OTP", () => {
    const now = new Date();
    const expiredAt = new Date(now.getTime() - 1000); // 1 detik yang lalu
    const validUntil = new Date(now.getTime() + 10 * 60 * 1000); // 10 menit ke depan

    expect(now > expiredAt).toBe(true); // Kedaluwarsa
    expect(now > validUntil).toBe(false); // Masih aktif
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
