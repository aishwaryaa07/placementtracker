// Shared by the navbar avatars and CompanyLogo - first letters of up to 2 significant words.
export function getInitials(text, fallback = '?') {
  if (!text) return fallback;
  const parts = text.trim().split(/\s+/);
  const initials = parts.slice(0, 2).map((p) => p[0]?.toUpperCase() || '');
  return initials.join('') || text[0].toUpperCase();
}
