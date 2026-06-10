// Jeu d'icônes maison : traits fins, cohérents, héritent de la couleur du texte.
// Remplace les emojis (qui « font IA / amateur »).

const PATHS = {
  board: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /></>,
  tasks: <><rect x="3" y="3" width="18" height="18" rx="2.5" /><path d="M8 12.5l2.5 2.5 5-5.5" /></>,
  bell: <><path d="M18 8.5a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14.5 18 8.5" /><path d="M13.5 19.5a2 2 0 0 1-3 0" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0" /></>,
  users: <><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20.5a6.5 6.5 0 0 1 13 0" /><path d="M16 4.8a3.5 3.5 0 0 1 0 6.4" /><path d="M17.5 13.5a6.5 6.5 0 0 1 4 6" /></>,
  flag: <><path d="M5 21V4" /><path d="M5 4.5h12l-2.2 3.8L17 12H5" /></>,
  list: <><path d="M8.5 6H21" /><path d="M8.5 12H21" /><path d="M8.5 18H21" /><circle cx="3.6" cy="6" r="1" /><circle cx="3.6" cy="12" r="1" /><circle cx="3.6" cy="18" r="1" /></>,
  lock: <><rect x="4" y="10.5" width="16" height="10.5" rx="2.5" /><path d="M8 10.5V7a4 4 0 0 1 8 0v3.5" /></>,
  inbox: <><path d="M3 13.5h5l1.5 2.5h5L21 13.5" /><path d="M5.5 5h13l3 8.5V19a1 1 0 0 1-1 1H3.5a1 1 0 0 1-1-1v-5.5z" /></>,
  chevron: <path d="M5.5 8.5L12 15l6.5-6.5" />,
  power: <><path d="M12 3.5v8.5" /><path d="M7.5 6.3a8 8 0 1 0 9 0" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></>,
}

// Logo Atlas — double chevron ascendant (pic de montagne / progression).
// Pas de boîte : le symbole se suffit à lui-même. Monochrome encre.
export function Logo({ size = 28, className, style }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      className={className}
      style={{ display: 'block', flexShrink: 0, ...style }}
      aria-hidden="true"
    >
      <path d="M4.5 19.5L16 6.5L27.5 19.5" stroke="var(--ink)" strokeWidth="3.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M10 25.5L16 16.5L22 25.5" stroke="var(--z400)" strokeWidth="3.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function Icon({ name, size = 18, strokeWidth = 1.7, style, className }) {
  const d = PATHS[name]
  if (!d) return null
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={{ display: 'block', ...style }}
      aria-hidden="true"
    >
      {d}
    </svg>
  )
}
