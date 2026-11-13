import mongoose from "mongoose";

const ReportSchema = new mongoose.Schema(
  {
    vendorId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Vendor",
      required: true,
      index: true,
    },
    namaToko: {
      type: String,
      required: true,
      trim: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },
    namaPelapor: {
      type: String,
      default: "Warga Komunitas",
      trim: true,
    },
    alasan: {
      type: String,
      enum: ["lokasi_salah", "sudah_tutup", "menu_salah", "duplikat", "lainnya"],
      default: "lokasi_salah",
      index: true,
    },
    deskripsi: {
      type: String,
      trim: true,
      default: "",
    },
    status: {
      type: String,
      enum: ["pending", "selesai", "diabaikan"],
      default: "pending",
      index: true,
    },
    catatanAdmin: {
      type: String,
      default: "",
    },
    createdAt: {
      type: Date,
      default: Date.now,
    },
  },
  { timestamps: true }
);

export default mongoose.models.Report || mongoose.model("Report", ReportSchema);
