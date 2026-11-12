// Gaya peta.
// Utama: gaya vektor OpenFreeMap "liberty" (bisa dioverride via NEXT_PUBLIC_MAP_STYLE_URL).
// Cadangan: raster OpenStreetMap bila gaya utama gagal dimuat.

export const GAYA_URL =
  process.env.NEXT_PUBLIC_MAP_STYLE_URL ||
  "https://tiles.openfreemap.org/styles/liberty";

export const GAYA_OSM = {
  version: 8,
  sources: {
    osm: {
      type: "raster",
      tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      tileSize: 256,
      maxzoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    },
  },
  layers: [{ id: "osm", type: "raster", source: "osm" }],
};