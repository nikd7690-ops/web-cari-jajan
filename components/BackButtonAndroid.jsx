"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";

// Hanya aktif di dalam APK (Capacitor native). Di web menjadi no-op.
export default function BackButtonAndroid() {
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    let ignore = false;
    let sub = null;

    async function init() {
      await Promise.resolve();

      if (typeof window === "undefined" || !window.Capacitor?.isNativePlatform?.()) {
        return;
      }

      try {
        const { App } = await import("@capacitor/app");
        if (ignore || !App) return;

        sub = await App.addListener("backButton", () => {
          if (window.history.length > 1) {
            router.back();
            return;
          }
          if (window.location.pathname !== "/") {
            router.push("/");
            return;
          }
          App.minimizeApp();
        });
      } catch {
        // plugin tidak tersedia (mis. APK lama) -> abaikan
      }
    }

    init();

    return () => {
      ignore = true;
      if (sub && typeof sub.remove === "function") sub.remove();
    };
  }, [router, pathname]);

  return null;
}