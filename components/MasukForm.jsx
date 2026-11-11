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
          <label className="mb-1 block text-sm font-medium text-zinc-700">
            Password
          </label>
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
    </main>
  );
}