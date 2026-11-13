import mongoose from "mongoose";

const MenuSchema = new mongoose.Schema(
  {
    namaMenu: {
      type: String,
      required: true,
      trim: true,
    },
    harga: {
      type: Number,
      required: true,
      min: 0,
    },
    tersedia: {
      type: Boolean,
      default: true,
    },
  },
  { _id: true }
);

const VendorSchema = new mongoose.Schema(
  {
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    submittedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      default: function () {
        return this.owner;
      },
    },
    status: {
      type: String,
      enum: ["pending", "approved", "rejected"],
      default: "pending",
      index: true,
    },
    namaToko: {
      type: String,
      required: true,
      trim: true,
    },
    kategori: {
      type: String,
      enum: ["warung", "pkl", "rumah_makan", "keliling"],
      required: true,
    },
    lokasi: {
      type: {
        type: String,
        enum: ["Point"],
        required: true,
        default: "Point",
      },
      coordinates: {
        type: [Number],
        required: true,
        validate: {
          validator: (val) => Array.isArray(val) && val.length === 2,
          message: "coordinates harus berisi [lng, lat]",
        },
      },
    },
    alamatPatokan: {
      type: String,
      required: true,
      trim: true,
    },
    jamBuka: {
      type: String,
      required: true,
      match: /^([01]\d|2[0-3]):[0-5]\d$/,
    },
    jamTutup: {
      type: String,
      required: true,
      match: /^([01]\d|2[0-3]):[0-5]\d$/,
    },
    nomorWA: {
      type: String,
      required: false,
      default: "",
      trim: true,
    },
    layananAntar: {
      type: Boolean,
      default: false,
    },
    foto: {
      type: String,
      default: "",
    },
    menus: {
      type: [MenuSchema],
      default: [],
    },
  },
  { timestamps: true }
);

VendorSchema.index({ lokasi: "2dsphere" });
VendorSchema.index({ status: 1, createdAt: -1 });

export default mongoose.models.Vendor || mongoose.model("Vendor", VendorSchema);
