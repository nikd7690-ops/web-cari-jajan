"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn } from "next-auth/react";

export default function DaftarPage() {
  const router = useRouter();
  const [nama, setNama] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nama, email, password }),
      });
      const json = await res.json();

      if (!res.ok) {
        setError(json.error || "Gagal mendaftar.");
        return;
      }

      const hasil = await signIn("credentials", {
        redirect: false,
        email,
        password,
      });

      if (!hasil || hasil.error) {
        router.push("/masuk");
        return;
      }

      router.push("/dashboard");
      router.refresh();
    } catch {
      setError("Gagal mendaftar, periksa koneksi lalu coba lagi.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm px-4 py-10">
      <h1 className="mb-1 text-2xl font-bold text-zinc-900">Daftar</h1>
      <p className="mb-6 text-sm text-zinc-600">
        Buat akun penjual untuk mendaftarkan warungmu.
      </p>

      <form onSubmit={onSubmit} className="space-y-3">
        <div>
          <label className="mb-1 block text-sm font-medium text-zinc-700">
            Nama
          </label>
          <input
            type="text"
            required
            value={nama}
            onChange={(e) => setNama(e.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
            placeholder="Nama kamu atau warung"
          />
        </div>

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
            minLength={8}
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
          {loading ? "Memproses..." : "Daftar"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-zinc-600">
        Sudah punya akun?{" "}
        <Link href="/masuk" className="font-medium text-emerald-700 underline">
          Masuk
        </Link>
      </p>
    </main>
  );
}