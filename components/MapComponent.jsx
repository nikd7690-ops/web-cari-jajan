"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import Map, { Marker, Popup } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import KategoriIcon from "@/components/KategoriIcon";
import { GAYA_OSM, GAYA_URL } from "@/lib/petaGaya";

function webglDidukung() {
  try {
    const canvas = document.createElement("canvas");
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext("webgl2") ||
        canvas.getContext("webgl") ||
        canvas.getContext("experimental-webgl"))
    );
  } catch {
    return false;
  }
}

function PinVendor({ vendor, aktif }) {
  const status = vendor.buka ? "buka" : "tutup";
  const diskonPersen =
    vendor.zeroWaste?.aktif && vendor.zeroWaste?.diskonPersen > 0
      ? vendor.zeroWaste.diskonPersen
      : 0;

  return (
    <div className={`cj-pin-wrap cj-pin-wrap--${status} flex flex-col items-center pointer-events-auto cursor-pointer`}>
      {/* Pop-up foto warung mengambang di atas pin saat diklik */}
      {aktif && vendor.foto && (
        <div className="mb-1.5 overflow-hidden rounded-xl border-2 border-white shadow-xl bg-white w-28 h-20 relative">
          <img
            src={vendor.foto}
            alt={vendor.namaToko}
            className="w-full h-full object-cover"
          />
          {diskonPersen > 0 && (
            <div className="absolute top-1 left-1 rounded-md bg-red-600 px-1.5 py-0.5 text-[9px] font-bold text-white shadow">
              Diskon {diskonPersen}%
            </div>
          )}
        </div>
      )}
      {!aktif && diskonPersen > 0 && (
        <div className="mb-1.5 flex items-center gap-1 rounded-full bg-red-600 px-2 py-0.5 text-[10px] font-bold text-white shadow-md border border-white whitespace-nowrap animate-bounce">
          <span className="flex h-3 w-3 items-center justify-center rounded-full bg-white text-[8px] font-black text-red-600 leading-none">
            %
          </span>
          <span>Diskon {diskonPersen}%</span>
        </div>
      )}
      <div
        className={`cj-pin cj-pin--${status}${aktif ? " cj-pin--aktif" : ""}`}
      >
        <span className="cj-pin__ikon">
          <KategoriIcon kategori={vendor.kategori} className="h-4 w-4" />
        </span>
        <span className="cj-pin__nama">{vendor.namaToko}</span>
      </div>
      <span className="cj-pin__ekor" />
    </div>
  );
}

export default function MapComponent({
  center,
  zoom = 15,
  vendors = [],
  userLocation,
  selectedId,
  onSelectVendor,
  onGagal,
}) {
  const mapRef = useRef(null);
  const sudahLoad = useRef(false);
  const timerRef = useRef(null);
  const [popupId, setPopupId] = useState(null);
  const [gaya, setGaya] = useState(GAYA_URL);
  const [gagal, setGagal] = useState(false);

  const lat = center[0];
  const lng = center[1];

  useEffect(() => {
    async function cek() {
      await Promise.resolve();
      if (typeof window === "undefined") return;
      if (!webglDidukung()) {
        setGagal(true);
        onGagal?.();
      }
    }
    cek();
  }, [onGagal]);

  useEffect(() => {
    if (gagal) return;

    timerRef.current = setTimeout(() => {
      if (sudahLoad.current) return;
      if (gaya !== GAYA_OSM) {
        setGaya(GAYA_OSM);
      } else {
        setGagal(true);
        onGagal?.();
      }
    }, 12000);

    return () => clearTimeout(timerRef.current);
  }, [gaya, gagal, onGagal]);

  useEffect(() => {
    if (gagal) return;
    mapRef.current?.flyTo({ center: [lng, lat], zoom, duration: 600 });
  }, [lng, lat, zoom, gagal]);

  useEffect(() => {
    if (gagal || !selectedId) return;
    const vendor = vendors.find((item) => item._id === selectedId);
    if (!vendor) return;
    mapRef.current?.flyTo({
      center: [vendor.lokasi.coordinates[0], vendor.lokasi.coordinates[1]],
      zoom: 16,
      duration: 600,
    });
  }, [selectedId, vendors, gagal]);

  if (gagal) {
    return (
      <div className="flex h-full w-full items-center justify-center bg-zinc-100 p-6 text-center text-sm text-zinc-600">
        Peta tidak dapat dimuat di perangkat ini. Daftar warung tetap bisa dipakai.
      </div>
    );
  }

  const vendorPopup = vendors.find((item) => item._id === popupId);

  return (
    <Map
      ref={mapRef}
      mapStyle={gaya}
      initialViewState={{ longitude: lng, latitude: lat, zoom }}
      style={{ height: "100%", width: "100%" }}
      className="h-full w-full z-0"
      dragPan={true}
      dragRotate={true}
      touchZoomRotate={true}
      touchPitch={false}
      pitchWithRotate={false}
      cooperativeGestures={false}
      onLoad={() => {
        sudahLoad.current = true;
      }}
      onError={() => {
        if (sudahLoad.current) return;
        if (gaya !== GAYA_OSM) {
          setGaya(GAYA_OSM);
        } else {
          setGagal(true);
          onGagal?.();
        }
      }}
    >
      {userLocation ? (
        <Marker
          longitude={userLocation[1]}
          latitude={userLocation[0]}
          anchor="bottom"
          color="#2563eb"
        />
      ) : null}

      {vendors.map((vendor) => (
        <Marker
          key={vendor._id}
          longitude={vendor.lokasi.coordinates[0]}
          latitude={vendor.lokasi.coordinates[1]}
          anchor="bottom"
          onClick={(event) => {
            event.originalEvent.stopPropagation();
            setPopupId(vendor._id);
            onSelectVendor?.(vendor._id);
          }}
        >
          <PinVendor vendor={vendor} aktif={selectedId === vendor._id} />
        </Marker>
      ))}

      {vendorPopup ? (
        <Popup
          longitude={vendorPopup.lokasi.coordinates[0]}
          latitude={vendorPopup.lokasi.coordinates[1]}
          anchor="bottom"
          closeOnClick={false}
          onClose={() => setPopupId(null)}
        >
          <div className="w-56 space-y-2 p-1 text-zinc-900">
            {vendorPopup.foto ? (
              <img
                src={vendorPopup.foto}
                alt={vendorPopup.namaToko}
                className="h-24 w-full rounded-lg object-cover"
              />
            ) : null}
            <div>
              <p className="font-bold text-sm leading-snug">{vendorPopup.namaToko}</p>
              <p className="text-xs text-zinc-600 line-clamp-1">{vendorPopup.alamatPatokan}</p>
              <p className="text-[11px] text-blue-700 font-medium mt-0.5">
                👤 Diajukan oleh: {vendorPopup.namaKontributor || "Warga Komunitas"}
              </p>
            </div>
            <div className="flex items-center justify-between text-xs pt-1 border-t border-zinc-100">
              <span
                className={`font-bold ${
                  vendorPopup.buka ? "text-emerald-700" : "text-red-600"
                }`}
              >
                {vendorPopup.buka ? "● Sedang Buka" : "● Sudah Tutup"}
              </span>
              <span className="text-[11px] text-zinc-500">
                {vendorPopup.jamBuka} - {vendorPopup.jamTutup}
              </span>
            </div>
            <div className="flex gap-1.5 pt-1">
              <a
                href={`https://www.google.com/maps/dir/?api=1&destination=${vendorPopup.lokasi.coordinates[1]},${vendorPopup.lokasi.coordinates[0]}`}
                target="_blank"
                rel="noreferrer"
                className="flex-1 text-center rounded-lg border border-zinc-300 bg-white py-1.5 text-xs font-semibold text-zinc-800 hover:bg-zinc-50"
              >
                Navigasi
              </a>
              <Link
                href={`/warung/${vendorPopup._id}`}
                className="flex-1 text-center rounded-lg bg-emerald-600 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700"
              >
                Menu & Info
              </Link>
            </div>
          </div>
        </Popup>
      ) : null}
    </Map>
  );
}