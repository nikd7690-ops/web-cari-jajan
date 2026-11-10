import { describe, it, expect } from "vitest";
import { normalisasiNomorWA } from "../lib/keranjang.js";

describe("normalisasiNomorWA", () => {
  it("mengubah awalan 0 menjadi 62", () => {
    expect(normalisasiNomorWA("081234567890")).toBe("6281234567890");
  });

  it("membuang karakter non-digit", () => {
    expect(normalisasiNomorWA("0812-3456-7890")).toBe("6281234567890");
    expect(normalisasiNomorWA("+62 812 3456 7890")).toBe("6281234567890");
  });

  it("membiarkan nomor yang sudah berawalan 62", () => {
    expect(normalisasiNomorWA("6281234567890")).toBe("6281234567890");
  });

  it("mengembalikan digit apa adanya bila bukan awalan 0 atau 62", () => {
    expect(normalisasiNomorWA("81234567890")).toBe("81234567890");
  });
});
