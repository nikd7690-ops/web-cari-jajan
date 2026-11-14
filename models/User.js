import mongoose from "mongoose";

const UserSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
    },
    passwordHash: {
      type: String,
      required: true,
    },
    role: {
      type: String,
      enum: ["user", "admin"],
      default: "user",
      index: true,
    },
    tipeAkun: {
      type: String,
      enum: ["pengguna", "mitra"],
      default: "pengguna",
      index: true,
    },
    xp: {
      type: Number,
      min: 0,
      default: 0,
    },
    level: {
      type: Number,
      min: 1,
      default: 1,
    },
    emailVerified: {
      type: Boolean,
      default: false,
    },
    isBanned: {
      type: Boolean,
      default: false,
      index: true,
    },
    bannedAt: {
      type: Date,
      default: null,
    },
    bannedReason: {
      type: String,
      default: "",
    },
    otpCode: {
      type: String,
      default: null,
    },
    otpExpiresAt: {
      type: Date,
      default: null,
    },
    createdAt: {
      type: Date,
      default: Date.now,
    },
  },
  { versionKey: false }
);

export default mongoose.models.User || mongoose.model("User", UserSchema);
