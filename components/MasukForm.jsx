"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn } from "next-auth/react";

export default function MasukForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // State dialog lupa password
  const [bukaLupaPassword, setBukaLupaPassword] = useState(false);
  const [stepOtp, setStepOtp] = useState(1); // 1 = minta OTP, 2 = verifikasi OTP & buat password baru
  const [resetEmail, setResetEmail] = useState("");
  const [otpInput, setOtpInput] = useState("");
  const [demoOtpInfo, setDemoOtpInfo] = useState("");
  const [resetPasswordBaru, setResetPasswordBaru] = useState("");
  const [resetKonfirmasi, setResetKonfirmasi] = useState("");
  const [resetLoading, setResetLoading] = useState(false);
  const [resetError, setResetError] = useState("");
  const [resetSukses, setResetSukses] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const hasil = await signIn("credentials", {
        redirect: false,
        email,
        password,
      });

      if (!hasil || hasil.error) {
        setError("Email atau password salah.");
        return;
      }

      router.push("/dashboard");
      router.refresh();
    } catch {
      setError("Gagal masuk, periksa koneksi lalu coba lagi.");
    } finally {
      setLoading(false);
    }
  }

  async function handleMintaOtp(e) {
    e.preventDefault();
    setResetError("");
    setDemoOtpInfo("");

    if (!resetEmail || !resetEmail.includes("@")) {
      setResetError("Masukkan alamat email yang valid.");
      return;
    }

    setResetLoading(true);
    try {
      const res = await fetch("/api/auth/request-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: resetEmail }),
      });

      const data = await res.json();
      if (!res.ok) {
        setResetError(data.error || "Gagal meminta kode OTP.");
      } else {
        if (data.demoOtp) {
          setDemoOtpInfo(data.demoOtp);
          setOtpInput(data.demoOtp);
        }
        setStepOtp(2);
      }
    } catch {
      setResetError("Gagal menghubungi server. Periksa koneksi Anda.");
    } finally {
      setResetLoading(false);
    }
  }

  async function handleResetPassword(e) {
    e.preventDefault();
    setResetError("");
    setResetSukses("");

    if (!otpInput || otpInput.trim().length !== 6) {
      setResetError("Kode OTP wajib 6 digit angka.");
      return;
    }
    if (resetPasswordBaru.length < 8) {
      setResetError("Password baru minimal 8 karakter.");
      return;
    }
    if (resetPasswordBaru !== resetKonfirmasi) {
      setResetError("Konfirmasi password baru tidak cocok.");
      return;
    }

    setResetLoading(true);
    try {
      const res = await fetch("/api/auth/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: resetEmail,
          otp: otpInput.trim(),
          passwordBaru: resetPasswordBaru,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        setResetError(data.error || "Gagal memperbarui password.");
      } else {
        setResetSukses(data.message || "Password berhasil diperbarui!");
      }
    } catch {
      setResetError("Gagal menghubungi server. Periksa koneksi Anda.");
    } finally {
      setResetLoading(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm px-4 py-10 pb-24">
      <h1 className="mb-1 text-2xl font-bold text-zinc-900">Masuk</h1>
      <p className="mb-6 text-sm text-zinc-600">
        Masuk untuk mengelola warungmu.
      </p>

      <form onSubmit={onSubmit} className="space-y-3">
        <div>
          <label className="mb-1 block text-sm font-medium text-zinc-700">
            Email
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
            placeholder="penjual@contoh.com"
          />
        </div>

        <div>
          <div className="flex items-center justify-between mb-1">
            <label className="block text-sm font-medium text-zinc-700">
              Password
            </label>
            <button
              type="button"
              onClick={() => {
                setStepOtp(1);
                setResetEmail(email);
                setOtpInput("");
                setDemoOtpInfo("");
                setResetError("");
                setResetSukses("");
                setBukaLupaPassword(true);
              }}
              className="text-xs font-semibold text-emerald-700 hover:underline"
            >
              Lupa password?
            </button>
          </div>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
            placeholder="Minimal 8 karakter"
          />
        </div>

        {error ? (
          <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-emerald-600 py-3 text-sm font-semibold text-white active:bg-emerald-700 disabled:opacity-60"
        >
          {loading ? "Memproses..." : "Masuk"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-zinc-600">
        Belum punya akun?{" "}
        <Link href="/daftar" className="font-medium text-emerald-700 underline">
          Daftar
        </Link>
      </p>

      {/* Modal Lupa Password */}
      {bukaLupaPassword && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-bold text-zinc-900">Lupa Password</h2>
              <span className="text-xs font-medium text-zinc-500">
                {stepOtp === 1 ? "Langkah 1/2" : "Langkah 2/2"}
              </span>
            </div>

            {resetSukses ? (
              <div className="mt-3 space-y-4">
                <div className="rounded-lg border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-800">
                  {resetSukses}
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setEmail(resetEmail);
                    setPassword("");
                    setBukaLupaPassword(false);
                  }}
                  className="w-full rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700"
                >
                  Tutup & Masuk Sekarang
                </button>
              </div>
            ) : stepOtp === 1 ? (
              <form onSubmit={handleMintaOtp} className="mt-3 space-y-3">
                <p className="text-xs text-zinc-600">
                  Masukkan email akun terdaftar Anda untuk menerima kode OTP 6 digit.
                </p>
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-700">
                    Email Akun
                  </label>
                  <input
                    type="email"
                    required
                    value={resetEmail}
                    onChange={(e) => setResetEmail(e.target.value)}
                    className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
                    placeholder="email@contoh.com"
                  />
                </div>

                {resetError && (
                  <p className="rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700">
                    {resetError}
                  </p>
                )}

                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    disabled={resetLoading}
                    onClick={() => setBukaLupaPassword(false)}
                    className="flex-1 rounded-xl border border-zinc-300 py-2.5 text-sm font-semibold text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
                  >
                    Batal
                  </button>
                  <button
                    type="submit"
                    disabled={resetLoading}
                    className="flex-1 rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {resetLoading ? "Mengirim..." : "Kirim Kode OTP"}
                  </button>
                </div>
              </form>
            ) : (
              <form onSubmit={handleResetPassword} className="mt-3 space-y-3">
                {demoOtpInfo && (
                  <div className="rounded-lg border border-amber-300 bg-amber-50 p-2.5 text-xs text-amber-900">
                    <span className="font-semibold">Kode OTP Verifikasi: </span>
                    <span className="font-mono font-bold tracking-wider text-amber-800 bg-amber-200/60 px-1.5 py-0.5 rounded">
                      {demoOtpInfo}
                    </span>
                    <p className="text-[11px] text-amber-700 mt-1">
                      (Untuk pengujian demo juri, kode OTP ditampilkan langsung & otomatis terisi).
                    </p>
                  </div>
                )}

                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-700">
                    Kode OTP (6 Digit)
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={otpInput}
                    onChange={(e) => setOtpInput(e.target.value)}
                    className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-center font-mono font-semibold tracking-widest text-zinc-900 outline-none focus:border-emerald-600"
                    placeholder="123456"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-700">
                    Password Baru
                  </label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={resetPasswordBaru}
                    onChange={(e) => setResetPasswordBaru(e.target.value)}
                    className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
                    placeholder="Minimal 8 karakter"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-700">
                    Konfirmasi Password Baru
                  </label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={resetKonfirmasi}
                    onChange={(e) => setResetKonfirmasi(e.target.value)}
                    className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
                    placeholder="Ketik ulang password baru"
                  />
                </div>

                {resetError && (
                  <p className="rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700">
                    {resetError}
                  </p>
                )}

                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    disabled={resetLoading}
                    onClick={() => setStepOtp(1)}
                    className="flex-1 rounded-xl border border-zinc-300 py-2.5 text-sm font-semibold text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
                  >
                    Kembali
                  </button>
                  <button
                    type="submit"
                    disabled={resetLoading}
                    className="flex-1 rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {resetLoading ? "Menyimpan..." : "Simpan Password"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </main>
  );
}