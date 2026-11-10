"use client";

import Link from "next/link";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
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

export default function MapComponent({ center, vendors = [], userLocation }) {
  return (
    <MapContainer
      key={`${center[0]},${center[1]}`}
      center={center}
      zoom={15}
      scrollWheelZoom={false}
      style={{ height: "320px", width: "100%" }}
      className="rounded-xl z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {userLocation ? (
        <Marker position={userLocation}>
          <Popup>Lokasi kamu</Popup>
        </Marker>
      ) : null}

      {vendors.map((vendor) => (
        <Marker
          key={vendor._id}
          position={[vendor.lokasi.coordinates[1], vendor.lokasi.coordinates[0]]}
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
                Lihat warung
              </Link>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
