import { describe, it, expect } from "vitest";
import { isBuka } from "../lib/jam.js";

function jam(h, m = 0, tanggal = "2026-01-15") {
  return new Date(`${tanggal}T${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:00`);
}

describe("isBuka jam normal", () => {
  it("buka di tengah rentang", () => {
    expect(isBuka("08:00", "17:00", jam(10, 30))).toBe(true);
  });

  it("buka tepat saat jamBuka", () => {
    expect(isBuka("08:00", "17:00", jam(8, 0))).toBe(true);
  });

  it("tutup tepat saat jamTutup", () => {
    expect(isBuka("08:00", "17:00", jam(17, 0))).toBe(false);
  });

  it("tutup sebelum jamBuka", () => {
    expect(isBuka("08:00", "17:00", jam(7, 59))).toBe(false);
  });
});

describe("isBuka lewat tengah malam", () => {
  it("buka malam hari", () => {
    expect(isBuka("18:00", "02:00", jam(23, 30))).toBe(true);
  });

  it("buka setelah tengah malam", () => {
    expect(isBuka("18:00", "02:00", jam(1, 0))).toBe(true);
  });

  it("tutup setelah jamTutup dini hari", () => {
    expect(isBuka("18:00", "02:00", jam(3, 0))).toBe(false);
  });

  it("tutup sebelum jamBuka sore", () => {
    expect(isBuka("18:00", "02:00", jam(17, 0))).toBe(false);
  });
});

describe("isBuka jam sama", () => {
  it("dianggap buka 24 jam bila jamBuka sama dengan jamTutup", () => {
    expect(isBuka("00:00", "00:00", jam(12, 0))).toBe(true);
  });
});
