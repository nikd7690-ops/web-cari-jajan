const svgProps = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  "aria-hidden": "true",
  focusable: "false",
};

function Pkl() {
  return (
    <>
      <path d="M4 8h16l-1.5 8h-13L4 8Z" />
      <path d="M4 8 3 4H1" />
      <circle cx="8" cy="19.5" r="1.5" />
      <circle cx="17" cy="19.5" r="1.5" />
    </>
  );
}

function Warung() {
  return (
    <>
      <path d="M3 9h18" />
      <path d="M4.5 9 6 4h12l1.5 5" />
      <path d="M5 9v11h14V9" />
      <path d="M9.5 20v-6h5v6" />
    </>
  );
}

function RumahMakan() {
  return (
    <>
      <path d="M4 11h16" />
      <path d="M5 11a7 7 0 0 0 14 0" />
      <path d="M9 7c0-1 .8-1.4.8-2.5" />
      <path d="M13.2 7c0-1 .8-1.4.8-2.5" />
    </>
  );
}

function Keliling() {
  return (
    <>
      <circle cx="6" cy="17" r="3" />
      <circle cx="18" cy="17" r="3" />
      <path d="M6 17 10 9h4l4 8" />
      <path d="M9 9h5" />
    </>
  );
}

export default function KategoriIcon({ kategori, className = "h-4 w-4" }) {
  const kunci = kategori === "cafe" ? "rumah_makan" : kategori;

  const isi = {
    pkl: <Pkl />,
    warung: <Warung />,
    rumah_makan: <RumahMakan />,
    keliling: <Keliling />,
  };

  return (
    <svg className={className} {...svgProps}>
      {isi[kunci] ?? isi.warung}
    </svg>
  );
}