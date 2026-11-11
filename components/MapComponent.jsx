"use client";

import { useEffect } from "react";
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

export default function MapComponent({
  center,
  vendors = [],
  userLocation,
  selectedId,
  onSelectVendor,
}) {
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
          <Popup>
            <div className="space-y-1">
              <p className="font-semibold">{vendor.namaToko}</p>
              <p className="text-xs">{vendor.alamatPatokan}</p>
              <p className="text-xs">
                {vendor.buka ? "Buka" : "Tutup"} - jarak{" "}
                {Math.round(vendor.jarak)} m
              </p>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}