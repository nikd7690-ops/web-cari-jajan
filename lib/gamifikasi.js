export const XP_PER_APPROVAL = 50;
export const XP_PER_LEVEL = 100;

// Level 1 dimulai dari 0 XP. Setiap kelipatan 100 XP naik satu level.
export function levelDariXp(xp) {
  const nilai = Number(xp);
  if (!Number.isFinite(nilai) || nilai <= 0) return 1;
  return 1 + Math.floor(nilai / XP_PER_LEVEL);
}
