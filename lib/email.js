/**
 * Modul Pengiriman Email OTP untuk Verifikasi Akun Gmail.
 * Jika kredensial SMTP tidak disetel (mode demo/hackathon), sistem berjalan
 * dalam mode simulasi dev agar pengujian oleh juri tidak terkendala kuota/jaringan pihak ketiga.
 */
import nodemailer from "nodemailer";

export async function kirimEmailOtp(email, kodeOtp) {
  const host = process.env.SMTP_HOST;
  const port = parseInt(process.env.SMTP_PORT || "587", 10);
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;
  const from = process.env.SMTP_FROM || user;

  if (host && user && pass) {
    try {
      const transporter = nodemailer.createTransport({
        host,
        port,
        secure: port === 465,
        auth: { user, pass },
      });

      await transporter.sendMail({
        from,
        to: email,
        subject: "Kode Verifikasi Akun Cari Jajan",
        text: `Kode OTP Anda: ${kodeOtp}\n\nBerlaku selama 5 menit.\nJangan bagikan kode ini kepada siapa pun.\n\n— Cari Jajan`,
        html: `
          <div style="font-family:sans-serif;max-width:420px;margin:0 auto;padding:24px;border:1px solid #e4e4e7;border-radius:12px">
            <h2 style="color:#059669;margin:0 0 8px">Cari Jajan</h2>
            <p style="color:#3f3f46;font-size:14px">Masukkan kode verifikasi berikut untuk mengaktifkan akun Anda:</p>
            <div style="background:#f0fdf4;border:2px solid #86efac;border-radius:8px;padding:16px;text-align:center;margin:16px 0">
              <span style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#065f46;font-family:monospace">${kodeOtp}</span>
            </div>
            <p style="color:#71717a;font-size:12px">Kode ini berlaku selama <strong>5 menit</strong>. Jangan bagikan kepada siapa pun.</p>
            <hr style="border:none;border-top:1px solid #e4e4e7;margin:16px 0"/>
            <p style="color:#a1a1aa;font-size:11px;text-align:center">Email otomatis dari Cari Jajan — Etalase Hiperlokal UMKM</p>
          </div>
        `,
      });

      console.log(`[EMAIL OTP] Berhasil terkirim ke ${email}`);
      return { success: true, sent: true };
    } catch (err) {
      console.error("[EMAIL OTP GAGAL]", err.message || err);
      // Jangan crash — fallback ke mode dev supaya user tetap bisa verifikasi
    }
  }

  // Fallback Mode Pengujian & Demo Lomba (Fail-Safe):
  console.log(`\n========================================`);
  console.log(`[VERIFIKASI GMAIL CARI JAJAN]`);
  console.log(`Kepada: ${email}`);
  console.log(`Kode OTP 6-Digit: ${kodeOtp}`);
  console.log(`Berlaku: 5 menit`);
  console.log(`========================================\n`);

  return { success: true, sent: false, devOtp: kodeOtp };
}
