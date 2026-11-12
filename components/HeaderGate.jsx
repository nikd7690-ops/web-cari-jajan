"use client";

import { usePathname } from "next/navigation";

const TANPA_HEADER = ["/", "/masuk", "/daftar"];

export default function HeaderGate({ children }) {
  const pathname = usePathname();

  if (TANPA_HEADER.includes(pathname)) return null;
  if (pathname.startsWith("/dashboard")) return null;

  return children;
}