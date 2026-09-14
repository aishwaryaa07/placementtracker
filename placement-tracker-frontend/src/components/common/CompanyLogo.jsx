import { useState } from 'react';
import { getInitials } from '../../utils/initials';

// One of each pair per company, deterministically chosen by name so the same company always
// gets the same color - drawn from the app's existing theme tokens (index.css :root), not a
// new palette.
const PALETTE = [
  ['var(--arctic)', 'var(--peacock)'],
  ['var(--sage)', 'var(--peacock)'],
  ['var(--bubblegum)', 'var(--peacock)'],
  ['var(--pistachio)', 'var(--peacock)'],
  ['var(--ballet)', 'var(--peacock)'],
  ['var(--sapphire)', 'var(--peacock)'],
];

function hashString(text) {
  let hash = 0;
  for (let i = 0; i < text.length; i++) {
    hash = (hash * 31 + text.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

// If a real logoUrl is set (Company.logoUrl - a direct image URL, e.g. a favicon-service URL
// for the company's own official domain), show that image instead of the initials avatar.
// onError falls back to the initials avatar so a dead/blocked URL never shows a broken-image
// icon - `failed` is per-mount state, not persisted, so a later successful load (e.g. after
// the admin fixes the URL and the page reloads) always gets a fresh attempt.
export default function CompanyLogo({ name, logoUrl, size = 36 }) {
  const [failed, setFailed] = useState(false);

  if (logoUrl && !failed) {
    return (
      <img
        src={logoUrl}
        alt=""
        className="company-logo company-logo-image"
        style={{ width: size, height: size }}
        onError={() => setFailed(true)}
      />
    );
  }

  const [background, color] = PALETTE[hashString(name || '') % PALETTE.length];
  return (
    <span
      className="company-logo"
      style={{ width: size, height: size, background, color, fontSize: Math.round(size * 0.4) }}
      aria-hidden="true"
    >
      {getInitials(name, '?')}
    </span>
  );
}
