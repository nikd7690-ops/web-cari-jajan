const ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models";

export const PROMPT_MENU = [
  "Kamu membaca foto menu warung Indonesia.",
  "Baca HANYA teks yang benar-benar terlihat di gambar. Jangan mengarang atau menebak.",
  "Untuk setiap menu, tulis namaMenu apa adanya dan harga sebagai angka rupiah (tanpa titik, tanpa 'Rp').",
  "Jika harga tidak terbaca jelas, isi harga dengan null dan yakin false.",
  "Jika yakin dengan harga, isi yakin true. Jika ragu, isi yakin false.",
  'Balas HANYA JSON valid: {"items":[{"namaMenu":"...","harga":0,"yakin":true}]}.',
].join(" ");

export async function bacaMenuDariGambar({ base64, mimeType, timeoutMs = 20000 }) {
  const apiKey = process.env.GEMINI_API_KEY;
  const model = process.env.GEMINI_MODEL;

  if (!apiKey || !model) {
    throw new Error("GEMINI_API_KEY atau GEMINI_MODEL belum diatur");
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(
      `${ENDPOINT}/${model}:generateContent?key=${apiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        signal: controller.signal,
        body: JSON.stringify({
          contents: [
            {
              parts: [
                { text: PROMPT_MENU },
                { inline_data: { mime_type: mimeType, data: base64 } },
              ],
            },
          ],
          generationConfig: {
            temperature: 0,
            responseMimeType: "application/json",
          },
        }),
      }
    );

    if (!res.ok) {
      const teks = await res.text();
      throw new Error(`Gemini ${res.status}: ${teks.slice(0, 200)}`);
    }

    const json = await res.json();
    const parts = json?.candidates?.[0]?.content?.parts ?? [];
    return parts.map((part) => part.text || "").join("");
  } finally {
    clearTimeout(timer);
  }
}