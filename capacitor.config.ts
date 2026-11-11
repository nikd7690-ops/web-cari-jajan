import type { CapacitorConfig } from "@capacitor/cli";

// URL muat aplikasi web. Bisa dioverride lewat env CAP_SERVER_URL saat build APK.
// Default: domain produksi Vercel yang sudah stabil.
const serverUrl =
  process.env.CAP_SERVER_URL || "https://web-cari-jajan.vercel.app";

const config: CapacitorConfig = {
  appId: "com.carijajan.app",
  appName: "Cari Jajan",
  webDir: "mobile",
  server: {
    url: serverUrl,
    androidScheme: "https",
    allowNavigation: ["web-cari-jajan.vercel.app"],
  },
  android: {
    allowMixedContent: false,
  },
  plugins: {
    Browser: {
      // Buka tautan luar (mis. wa.me) memakai browser sistem.
      presentationStyle: "automatic",
    },
  },
};

export default config;