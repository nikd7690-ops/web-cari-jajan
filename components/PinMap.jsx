"use client";

import { useEffect } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  useMap,
  useMapEvents,
} from "react-leaflet";
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

function PindahPusat({ pos }) {
  const map = useMap();

  useEffect(() => {
    map.setView([pos.lat, pos.lng], map.getZoom());
  }, [pos.lat, pos.lng, map]);

  return null;
}

function KlikPeta({ onChange }) {
  useMapEvents({
    click(event) {
      onChange({ lat: event.latlng.lat, lng: event.latlng.lng });
    },
  });

  return null;
}

export default function PinMap({ value, onChange }) {
  return (
    <MapContainer
      center={[value.lat, value.lng]}
      zoom={16}
      scrollWheelZoom={false}
      style={{ height: "240px", width: "100%" }}
      className="rounded-xl z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <PindahPusat pos={value} />
      <KlikPeta onChange={onChange} />
      <Marker
        draggable
        position={[value.lat, value.lng]}
        eventHandlers={{
          dragend(event) {
            const pos = event.target.getLatLng();
            onChange({ lat: pos.lat, lng: pos.lng });
          },
        }}
      />
    </MapContainer>
  );
}