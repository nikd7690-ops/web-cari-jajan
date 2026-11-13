import { describe, it, expect } from "vitest";
import mongoose from "mongoose";

describe("Fitur Hapus Member Admin", () => {
  function validasiHapusMember(adminUser, targetMember) {
    if (!mongoose.Types.ObjectId.isValid(targetMember._id)) {
      return { boleh: false, pesan: "ID member tidak valid", status: 400 };
    }
    if (adminUser.id === targetMember._id) {
      return { boleh: false, pesan: "Tidak dapat menghapus akun admin yang sedang aktif", status: 400 };
    }
    if (targetMember.role === "admin") {
      return { boleh: false, pesan: "Akun dengan peran admin tidak dapat dihapus", status: 403 };
    }
    return { boleh: true, pesan: "Member boleh dihapus", status: 200 };
  }

  it("mencegah admin menghapus akun miliknya sendiri", () => {
    const adminUser = { id: "650000000000000000000001" };
    const target = { _id: "650000000000000000000001", role: "admin" };
    const res = validasiHapusMember(adminUser, target);
    expect(res.boleh).toBe(false);
    expect(res.status).toBe(400);
  });

  it("mencegah penghapusan akun sesama admin", () => {
    const adminUser = { id: "650000000000000000000001" };
    const target = { _id: "650000000000000000000002", role: "admin" };
    const res = validasiHapusMember(adminUser, target);
    expect(res.boleh).toBe(false);
    expect(res.status).toBe(403);
  });

  it("mengizinkan admin menghapus akun pengguna/mitra yang melanggar", () => {
    const adminUser = { id: "650000000000000000000001" };
    const target = { _id: "650000000000000000000003", role: "user", name: "Akun Palsu" };
    const res = validasiHapusMember(adminUser, target);
    expect(res.boleh).toBe(true);
    expect(res.status).toBe(200);
  });

  it("menolak ID yang tidak valid", () => {
    const adminUser = { id: "650000000000000000000001" };
    const target = { _id: "bukan-id-valid", role: "user" };
    const res = validasiHapusMember(adminUser, target);
    expect(res.boleh).toBe(false);
    expect(res.status).toBe(400);
  });
});
