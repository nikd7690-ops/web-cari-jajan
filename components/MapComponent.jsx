"use client";

import { useEffect } from "react";
import Link from "next/link";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
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

const IKON = {
  pkl: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 8h16l-1.5 8h-13L4 8Z"/><path d="M4 8 3 4H1"/><circle cx="8" cy="19.5" r="1.5"/><circle cx="17" cy="19.5" r="1.5"/></svg>',
  warung:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 9h18"/><path d="M4.5 9 6 4h12l1.5 5"/><path d="M5 9v11h14V9"/><path d="M9.5 20v-6h5v6"/></svg>',
  rumah_makan:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 11h16"/><path d="M5 11a7 7 0 0 0 14 0"/><path d="M9 7c0-1 .8-1.4.8-2.5"/><path d="M13.2 7c0-1 .8-1.4.8-2.5"/></svg>',
  keliling:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="6" cy="17" r="3"/><circle cx="18" cy="17" r="3"/><path d="M6 17 10 9h4l4 8"/><path d="M9 9h5"/></svg>',
};

function escapeHtml(nilai) {
  return String(nilai).replace(/[&<>"']/g, (huruf) => {
    const peta = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    };
    return peta[huruf];
  });
}

function buatIkon(vendor, aktif) {
  const kunci = vendor.kategori === "cafe" ? "rumah_makan" : vendor.kategori;
  const ikon = IKON[kunci] ?? IKON.warung;
  const status = vendor.buka ? "buka" : "tutup";

  return L.divIcon({
    className: `cj-pin-wrap cj-pin-wrap--${status}`,
    html: `<div class="cj-pin cj-pin--${status}${
      aktif ? " cj-pin--aktif" : ""
    }"><span class="cj-pin__ikon">${ikon}</span><span class="cj-pin__nama">${escapeHtml(
      vendor.namaToko
    )}</span></div><span class="cj-pin__ekor"></span>`,
    iconSize: [60, 70],
    iconAnchor: [30, 70],
    popupAnchor: [0, -66],
  });
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

function PusatkanPeta({ center, zoom }) {
  const map = useMap();
  const lat = center[0];
  const lng = center[1];

  useEffect(() => {
    map.flyTo([lat, lng], zoom ?? map.getZoom(), { duration: 0.6 });
  }, [lat, lng, zoom, map]);

  return null;
}

export default function MapComponent({
  center,
  zoom = 15,
  vendors = [],
  userLocation,
  selectedId,
  onSelectVendor,
}) {
  return (
    <MapContainer
      center={center}
      zoom={zoom}
      scrollWheelZoom
      touchZoom
      doubleClickZoom
      style={{ height: "100%", width: "100%" }}
      className="h-full w-full z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <PusatkanPeta center={center} zoom={zoom} />
      <FokusVendor vendors={vendors} selectedId={selectedId} />

      {userLocation ? (
        <Marker position={userLocation}>
          <Popup>Lokasi kamu</Popup>
        </Marker>
      ) : null}

      {vendors.map((vendor) => (
        <Marker
          key={vendor._id}
          position={[vendor.lokasi.coordinates[1], vendor.lokasi.coordinates[0]]}
          icon={buatIkon(vendor, selectedId === vendor._id)}
          eventHandlers={{
            click: () => onSelectVendor?.(vendor._id),
          }}
        >
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