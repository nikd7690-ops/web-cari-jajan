export const KATEGORI = [
  { value: "pkl", label: "Pedagang kaki lima" },
  { value: "warung", label: "Warung" },
  { value: "rumah_makan", label: "Rumah makan" },
  { value: "keliling", label: "Penjual keliling menetap" },
];

const PETA_LABEL = new Map(KATEGORI.map((item) => [item.value, item.label]));

export function labelKategori(value) {
  if (value === "cafe") return "Rumah makan";
  return PETA_LABEL.get(value) ?? value ?? "-";
}

export function kategoriValid(value) {
  return PETA_LABEL.has(value);
}