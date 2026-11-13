"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn } from "next-auth/react";

export default function DaftarForm() {
  const router = useRouter();
  const [nama, setNama] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [tipeAkun, setTipeAkun] = useState("mitra"); // "mitra" atau "pengguna"
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // OTP Verification Step
  const [tahapOtp, setTahapOtp] = useState(false);
  const [kodeOtp, setKodeOtp] = useState("");
  const [devOtp, setDevOtp] = useState("");
  const [otpPesan, setOtpPesan] = useState("");
  const [otpLoading, setOtpLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");

    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail.endsWith("@gmail.com")) {
      setError("Wajib menggunakan alamat email resmi @gmail.com untuk mencegah akun palsu/spam.");
      return;
    }

    setLoading(true);

    try {
      const res = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nama, email: cleanEmail, password, tipeAkun }),
      });
      const json = await res.json();

      if (!res.ok) {
        setError(json.error || "Gagal mendaftar.");
        return;
      }

      setDevOtp(json.devOtp || "");
      setOtpPesan(json.pesan || "Kode OTP 6-digit telah dikirim ke Gmail Anda.");
      setTahapOtp(true);
    } catch {
      setError("Gagal mendaftar, periksa koneksi lalu coba lagi.");
    } finally {
      setLoading(false);
    }
  }

  async function onVerifikasiOtp(e) {
    e.preventDefault();
    setError("");

    if (kodeOtp.trim().length !== 6) {
      setError("Masukkan 6 digit kode OTP verifikasi.");
      return;
    }

    setOtpLoading(true);

    try {
      const cleanEmail = email.trim().toLowerCase();
      const res = await fetch("/api/auth/verifikasi-otp-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: cleanEmail, kodeOtp: kodeOtp.trim() }),
      });
      const json = await res.json();

      if (!res.ok) {
        setError(json.error || "Kode OTP salah atau telah kadaluarsa.");
        return;
      }

      const hasil = await signIn("credentials", {
        redirect: false,
        email: cleanEmail,
        password,
      });

      if (!hasil || hasil.error) {
        router.push("/masuk");
        return;
      }

      router.push("/dashboard");
      router.refresh();
    } catch {
      setError("Gagal memverifikasi OTP, periksa koneksi lalu coba lagi.");
    } finally {
      setOtpLoading(false);
    }
  }

  async function onKirimUlangOtp() {
    setError("");
    setOtpLoading(true);
    try {
      const cleanEmail = email.trim().toLowerCase();
      const res = await fetch("/api/auth/kirim-otp-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: cleanEmail }),
      });
      const json = await res.json();
      if (!res.ok) {
        setError(json.error || "Gagal mengirim ulang OTP.");
        return;
      }
      if (json.devOtp) setDevOtp(json.devOtp);
      setOtpPesan("Kode OTP baru berhasil dikirim ke email Anda.");
    } catch {
      setError("Gagal menghubungi server untuk mengirim ulang OTP.");
    } finally {
      setOtpLoading(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm px-4 py-10 pb-24">
      <h1 className="mb-1 text-2xl font-bold text-zinc-900">
        {tahapOtp ? "Verifikasi Email" : "Daftar Akun"}
      </h1>
      <p className="mb-6 text-sm text-zinc-600">
        {tahapOtp
          ? `Masukkan 6 digit kode verifikasi yang dikirim ke ${email}.`
          : "Buat akun terverifikasi untuk mulai menjelajah atau mendaftarkan warung UMKM."}
      </p>

      {tahapOtp ? (
        <form onSubmit={onVerifikasiOtp} className="space-y-4">
          {devOtp ? (
            <div className="rounded-xl border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
              <span className="font-bold">💡 Mode Pengujian / Penjurian Hackathon:</span>
              <p className="mt-1 font-mono text-base font-extrabold text-amber-700">
                Kode OTP: {devOtp}
              </p>
            </div>
          ) : null}

          {otpPesan ? (
            <p className="rounded-lg bg-emerald-50 p-2.5 text-xs text-emerald-800">
              {otpPesan}
            </p>
          ) : null}

          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Kode OTP (6 Angka)
            </label>
            <input
              type="text"
              maxLength={6}
              required
              value={kodeOtp}
              onChange={(e) => setKodeOtp(e.target.value.replace(/\D/g, ""))}
              className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-center font-mono text-2xl font-bold tracking-widest text-zinc-900 outline-none focus:border-emerald-600"
              placeholder="123456"
            />
          </div>

          {error ? (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={otpLoading || kodeOtp.length !== 6}
            className="w-full rounded-xl bg-emerald-600 py-3 text-sm font-semibold text-white active:bg-emerald-700 disabled:opacity-60"
          >
            {otpLoading ? "Memverifikasi..." : "Verifikasi & Lanjut"}
          </button>

          <div className="flex justify-between items-center pt-2 text-xs">
            <button
              type="button"
              onClick={onKirimUlangOtp}
              disabled={otpLoading}
              className="font-medium text-blue-600 hover:underline"
            >
              Kirim Ulang Kode OTP
            </button>
            <button
              type="button"
              onClick={() => {
                setTahapOtp(false);
                setError("");
              }}
              className="text-zinc-500 hover:underline"
            >
              Ubah Data
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-zinc-500">
              Pilih Jenis Akun
            </label>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                type="button"
                onClick={() => setTipeAkun("mitra")}
                className={`flex flex-col text-left p-3 rounded-xl border transition-all ${
                  tipeAkun === "mitra"
                    ? "border-emerald-600 bg-emerald-50 text-emerald-950 ring-1 ring-emerald-600"
                    : "border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50"
                }`}
              >
                <span className="text-xl mb-1">🏪</span>
                <span className="text-xs font-bold">Mitra / Pemilik Warung</span>
                <span className="text-[10px] text-zinc-500 mt-1 leading-snug">
                  Kelola 1 warung resmi, menu, & diskon darurat Zero Waste Food.
                </span>
              </button>

              <button
                type="button"
                onClick={() => setTipeAkun("pengguna")}
                className={`flex flex-col text-left p-3 rounded-xl border transition-all ${
                  tipeAkun === "pengguna"
                    ? "border-blue-600 bg-blue-50 text-blue-950 ring-1 ring-blue-600"
                    : "border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50"
                }`}
              >
                <span className="text-xl mb-1">👤</span>
                <span className="text-xs font-bold">Pengguna / Member</span>
                <span className="text-[10px] text-zinc-500 mt-1 leading-snug">
                  Bebas ajukan data warung baru di sekitar untuk dikurasi admin.
                </span>
              </button>
            </div>
          </div>

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
              Email (@gmail.com)
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
              placeholder="nama@gmail.com"
            />
            <p className="mt-1 text-[11px] text-emerald-700">
              🛡️ Hanya menerima akun resmi @gmail.com untuk mencegah spam & bot.
            </p>
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
            {loading ? "Memproses..." : "Daftar Akun"}
          </button>
        </form>
      )}

      <p className="mt-4 text-center text-sm text-zinc-600">
        Sudah punya akun?{" "}
        <Link href="/masuk" className="font-medium text-emerald-700 underline">
          Masuk
        </Link>
      </p>
    </main>
  );
}
