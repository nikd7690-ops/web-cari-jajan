import mongoose from "mongoose";

const OtpVerificationSchema = new mongoose.Schema(
  {
    email: {
      type: String,
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },
    otpHash: {
      type: String,
      required: true,
    },
    attempts: {
      type: Number,
      default: 0,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: { expires: 0 }, // TTL index: MongoDB otomatis menghapus dokumen saat expiresAt tercapai
    },
  },
  { timestamps: true, versionKey: false }
);

export default mongoose.models.OtpVerification ||
  mongoose.model("OtpVerification", OtpVerificationSchema);
