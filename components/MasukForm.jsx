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
  const [resetEmail, setResetEmail] = useState("");
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

  async function handleResetPassword(e) {
    e.preventDefault();
    setResetError("");
    setResetSukses("");

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
                setResetEmail(email);
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
            <h2 className="text-lg font-bold text-zinc-900">Lupa Password</h2>
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
            ) : (
              <form onSubmit={handleResetPassword} className="mt-3 space-y-3">
                <p className="text-xs text-zinc-600">
                  Masukkan email akun terdaftar dan buat password baru (minimal 8 karakter).
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
                    {resetLoading ? "Menyimpan..." : "Perbarui"}
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