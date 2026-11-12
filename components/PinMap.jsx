"use client";

import { useEffect } from "react";
import Map, { Marker, useMap } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import { GAYA_URL } from "@/lib/petaGaya";

function PindahPusat({ pos }) {
  const { current: map } = useMap();

  useEffect(() => {
    map?.flyTo({ center: [pos.lng, pos.lat], zoom: 16 });
  }, [pos.lat, pos.lng, map]);

  return null;
}

export default function PinMap({ value, onChange }) {
  return (
    <Map
      mapStyle={GAYA_URL}
      initialViewState={{ longitude: value.lng, latitude: value.lat, zoom: 16 }}
      style={{ height: "240px", width: "100%" }}
      className="rounded-xl z-0"
      scrollZoom={false}
      dragRotate={false}
      touchPitch={false}
      onClick={(event) => {
        onChange({ lat: event.lngLat.lat, lng: event.lngLat.lng });
      }}
    >
      <PindahPusat pos={value} />
      <Marker
        longitude={value.lng}
        latitude={value.lat}
        anchor="bottom"
        draggable
        onDragEnd={(event) => {
          onChange({ lat: event.lngLat.lat, lng: event.lngLat.lng });
        }}
      />
    </Map>
  );
}
