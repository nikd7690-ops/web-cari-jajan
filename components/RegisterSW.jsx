"use client";

import { useEffect } from "react";
import StatusBarAndroid from "@/components/StatusBarAndroid";

export default function RegisterSW() {
  useEffect(() => {
    if (typeof navigator === "undefined") return;
    if (!("serviceWorker" in navigator)) return;
    if (!window.isSecureContext) return;

    const timer = setTimeout(() => {
      navigator.serviceWorker.register("/sw.js", { scope: "/" }).catch(() => {
        // abaikan: offline hanyalah peningkatan, bukan syarat
      });
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  return <StatusBarAndroid />;
}