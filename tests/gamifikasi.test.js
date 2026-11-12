import { describe, it, expect } from "vitest";
import { levelDariXp, XP_PER_APPROVAL, XP_PER_LEVEL } from "../lib/gamifikasi.js";

describe("gamifikasi", () => {
  it("level 1 untuk XP 0", () => {
    expect(levelDariXp(0)).toBe(1);
  });

  it("level 1 sampai sebelum 100 XP", () => {
    expect(levelDariXp(99)).toBe(1);
  });

  it("naik level tepat di kelipatan 100", () => {
    expect(levelDariXp(100)).toBe(2);
    expect(levelDariXp(200)).toBe(3);
  });

  it("level di antara kelipatan", () => {
    expect(levelDariXp(250)).toBe(3);
  });

  it("nilai tidak valid dianggap 0 XP", () => {
    expect(levelDariXp(undefined)).toBe(1);
    expect(levelDariXp(-10)).toBe(1);
  });

  it("konstanta XP sesuai aturan", () => {
    expect(XP_PER_APPROVAL).toBe(50);
    expect(XP_PER_LEVEL).toBe(100);
  });
});
