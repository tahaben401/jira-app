import { badgeColor, label } from '../lib/constants.js'
import { initials } from '../lib/format.js'

export function Badge({ value, text, dot = true }) {
  return (
    <span className={`badge badge-${badgeColor(value)}`}>
      {dot && <span className="dot" />}
      {text ?? label(value)}
    </span>
  )
}

export function Avatar({ name, size }) {
  return <span className={`avatar ${size === 'lg' ? 'avatar-lg' : ''}`}>{initials(name)}</span>
}

export function Spinner({ lg }) {
  return <span className={`spinner ${lg ? 'spinner-lg' : ''}`} />
}

export function Loading({ label = 'Chargement…' }) {
  return (
    <div className="center-spin">
      <div style={{ textAlign: 'center' }}>
        <Spinner lg />
        <div className="muted mt-8">{label}</div>
      </div>
    </div>
  )
}

export function Empty({ icon = '∅', title, children }) {
  return (
    <div className="empty">
      <div className="em-ic">{icon}</div>
      {title && <h4>{title}</h4>}
      {children && <div>{children}</div>}
    </div>
  )
}

// ── Champs contrôlés ─────────────────────────────────────────────────────
export function Field({ label, hint, error, children }) {
  return (
    <div className="field">
      {label && <label>{label}</label>}
      {children}
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="err">{error}</span>}
    </div>
  )
}

export function Input(props) { return <input className="input" {...props} /> }
export function Textarea(props) { return <textarea className="textarea" {...props} /> }

export function Select({ children, ...props }) {
  return <select className="select" {...props}>{children}</select>
}

// Select pré-rempli depuis une liste d'enums + libellés.
export function EnumSelect({ options, value, onChange, placeholder, ...props }) {
  return (
    <Select value={value} onChange={(e) => onChange(e.target.value)} {...props}>
      {placeholder && <option value="">{placeholder}</option>}
      {options.map((o) => (
        <option key={o} value={o}>{label(o)}</option>
      ))}
    </Select>
  )
}
