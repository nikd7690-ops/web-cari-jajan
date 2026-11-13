import { describe, it, expect } from "vitest";

function hitungJarakMeter(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function isValidGmail(email) {
  const bersih = String(email).trim().toLowerCase();
  return /^[^\s@]+@gmail\.com$/.test(bersih);
}

describe("Proof of Presence (Maksimal 50 Meter)", () => {
  it("menerima jika koordinat fisik sama dengan titik warung (0 meter)", () => {
    const jarak = hitungJarakMeter(-8.1689, 113.7020, -8.1689, 113.7020);
    expect(jarak).toBeLessThanOrEqual(50.0);
  });

  it("menerima jika pengguna berada di depan warung (radius ~25 meter)", () => {
    // Geser koordinat sedikit (~0.0002 derajat ~22 meter)
    const jarak = hitungJarakMeter(-8.1689, 113.7020, -8.1691, 113.7020);
    expect(jarak).toBeLessThanOrEqual(50.0);
  });

  it("menolak jika pengunggah berada jauh dari warung (> 50 meter)", () => {
    // Geser koordinat sejauh ~0.001 derajat (~111 meter)
    const jarak = hitungJarakMeter(-8.1689, 113.7020, -8.1700, 113.7020);
    expect(jarak).toBeGreaterThan(50.0);
  });
});

describe("Validasi Akun Wajib @gmail.com", () => {
  it("menerima alamat email resmi @gmail.com", () => {
    expect(isValidGmail("budi.santoso@gmail.com")).toBe(true);
    expect(isValidGmail("warung.berkah2026@gmail.com")).toBe(true);
  });

  it("menolak domain email sementara atau non-gmail", () => {
    expect(isValidGmail("spammer@tempmail.com")).toBe(false);
    expect(isValidGmail("bot@10minutemail.net")).toBe(false);
    expect(isValidGmail("user@yahoo.com")).toBe(false);
    expect(isValidGmail("user@outlook.com")).toBe(false);
    expect(isValidGmail("fake@gmail.com.co")).toBe(false);
  });
});
