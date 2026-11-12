"use client";

import { useEffect } from "react";

// Hanya aktif di dalam APK (Capacitor native). Di web menjadi no-op.
export default function StatusBarAndroid() {
  useEffect(() => {
    let ignore = false;

    async function init() {
      await Promise.resolve();

      if (typeof window === "undefined" || !window.Capacitor?.isNativePlatform?.()) {
        return;
      }

      try {
        const { StatusBar, Style } = await import("@capacitor/status-bar");
        if (ignore || !StatusBar) return;
        await StatusBar.setStyle({ style: Style.Light });
        await StatusBar.setBackgroundColor({ color: "#059669" });
      } catch {
        // sebagian perangkat tidak mendukung pewarnaan status bar
      }
    }

    init();

    return () => {
      ignore = true;
    };
  }, []);

  return null;
}