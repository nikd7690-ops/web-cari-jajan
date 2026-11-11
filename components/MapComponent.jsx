"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { MapContainer, TileLayer, Marker, Popup, Tooltip, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x.src || markerIcon2x,
  iconUrl: markerIcon.src || markerIcon,
  iconShadow: markerShadow.src || markerShadow,
});

const JARAK_MIN_PX = 72;

function idKartuTampak(map, vendors) {
  const urut = [...vendors].sort((a, b) => (a.jarak ?? 0) - (b.jarak ?? 0));
  const dipilih = [];

  for (const vendor of urut) {
    const titik = map.latLngToContainerPoint([
      vendor.lokasi.coordinates[1],
      vendor.lokasi.coordinates[0],
    ]);
    const terlaluDekat = dipilih.some((item) => {
      const dx = item.titik.x - titik.x;
      const dy = item.titik.y - titik.y;
      return Math.hypot(dx, dy) < JARAK_MIN_PX;
    });

    if (!terlaluDekat) {
      dipilih.push({ id: vendor._id, titik });
    }
  }

  return new Set(dipilih.map((item) => item.id));
}

function FokusVendor({ vendors, selectedId }) {
  const map = useMap();

  useEffect(() => {
    if (!selectedId) return;

    const vendor = vendors.find((item) => item._id === selectedId);
    if (!vendor) return;

    map.flyTo(
      [vendor.lokasi.coordinates[1], vendor.lokasi.coordinates[0]],
      Math.max(map.getZoom(), 16),
      { duration: 0.6 }
    );
  }, [selectedId, vendors, map]);

  return null;
}

function KartuTampak({ vendors, onChange }) {
  const map = useMap();

  useEffect(() => {
    function hitung() {
      onChange(idKartuTampak(map, vendors));
    }

    hitung();
    map.on("moveend zoomend", hitung);

    return () => {
      map.off("moveend zoomend", hitung);
    };
  }, [map, vendors, onChange]);

  return null;
}

export default function MapComponent({
  center,
  vendors = [],
  userLocation,
  selectedId,
  onSelectVendor,
}) {
  const [kartuTampak, setKartuTampak] = useState(() => new Set());

  return (
    <MapContainer
      key={`${center[0]},${center[1]}`}
      center={center}
      zoom={15}
      scrollWheelZoom={false}
      style={{ height: "100%", width: "100%" }}
      className="h-full w-full z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <FokusVendor vendors={vendors} selectedId={selectedId} />
      <KartuTampak vendors={vendors} onChange={setKartuTampak} />

      {userLocation ? (
        <Marker position={userLocation}>
          <Popup>Lokasi kamu</Popup>
        </Marker>
      ) : null}

      {vendors.map((vendor) => (
        <Marker
          key={vendor._id}
          position={[vendor.lokasi.coordinates[1], vendor.lokasi.coordinates[0]]}
          eventHandlers={{
            click: () => onSelectVendor?.(vendor._id),
          }}
        >
          {kartuTampak.has(vendor._id) ? (
            <Tooltip
              permanent
              direction="top"
              offset={[0, -8]}
              opacity={1}
              className="cj-pin-card"
            >
              <div className="relative h-14 w-14">
                <div
                  className={`relative h-14 w-14 overflow-hidden rounded-full border-2 bg-zinc-100 shadow-md ${
                    vendor.buka ? "border-emerald-500" : "border-red-500"
                  }`}
                >
                  {vendor.foto ? (
                    <Image
                      src={vendor.foto}
                      alt={`Foto ${vendor.namaToko}`}
                      fill
                      sizes="56px"
                      className="object-cover"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center text-[10px] text-zinc-400">
                      Foto
                    </div>
                  )}
                </div>
                <span
                  className={`absolute -bottom-1 left-1/2 -translate-x-1/2 rounded-full px-1.5 py-0.5 text-[9px] font-semibold leading-none ${
                    vendor.buka
                      ? "bg-emerald-600 text-white"
                      : "bg-red-600 text-white"
                  }`}
                >
                  {vendor.buka ? "Buka" : "Tutup"}
                </span>
              </div>
            </Tooltip>
          ) : null}

          <Popup>
            <div className="space-y-1">
              <p className="font-semibold">{vendor.namaToko}</p>
              <p className="text-xs">{vendor.alamatPatokan}</p>
              <p className="text-xs">
                {vendor.buka ? "Buka" : "Tutup"} - jarak{" "}
                {Math.round(vendor.jarak)} m
              </p>
              <Link
                href={`/warung/${vendor._id}`}
                className="text-xs font-medium text-emerald-700 underline"
              >
                Lihat &amp; pesan
              </Link>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
