import { useEffect, useState, useCallback } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { notificationsApi } from '../api/notifications.js'
import { Avatar } from './ui.jsx'
import { Icon, Logo } from './Icons.jsx'
import { label } from '../lib/constants.js'

// Permet à n'importe quelle page de demander un rafraîchissement du compteur
// de notifications non lues (ex. après avoir marqué une notif comme lue).
export function refreshUnread() {
  window.dispatchEvent(new Event('atlas:unread'))
}

const NAV = [
  { to: '/projects', icon: 'board', label: 'Projets' },
  { to: '/my-tasks', icon: 'tasks', label: 'Mes tâches' },
  { to: '/notifications', icon: 'bell', label: 'Notifications', badge: true },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)
  const [menuOpen, setMenuOpen] = useState(false)

  const loadUnread = useCallback(async () => {
    try {
      const list = await notificationsApi.unread()
      setUnread(Array.isArray(list) ? list.length : 0)
    } catch { /* silencieux : le compteur n'est pas critique */ }
  }, [])

  useEffect(() => {
    loadUnread()
    const id = setInterval(loadUnread, 20000)
    window.addEventListener('atlas:unread', loadUnread)
    return () => { clearInterval(id); window.removeEventListener('atlas:unread', loadUnread) }
  }, [loadUnread])

  const onLogout = async () => { await logout(); navigate('/login') }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <Logo size={26} />
          <span className="wordmark">Atlas</span>
        </div>
        <nav className="nav">
          <div className="nav-label">Espace de travail</div>
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="ic"><Icon name={item.icon} size={17} /></span>
              {item.label}
              {item.badge && unread > 0 && <span className="nav-count">{unread}</span>}
            </NavLink>
          ))}
          <div className="nav-label">Compte</div>
          <NavLink to="/profile" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="ic"><Icon name="user" size={17} /></span>
            Mon profil
          </NavLink>
        </nav>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="spacer" />
          <div style={{ position: 'relative' }}>
            <button
              className="btn btn-ghost"
              onClick={() => setMenuOpen((o) => !o)}
              style={{ height: 40, paddingLeft: 6 }}
            >
              <Avatar name={user?.fullName} />
              <span style={{ textAlign: 'left', lineHeight: 1.2 }}>
                <div style={{ fontWeight: 600, fontSize: 13 }}>{user?.fullName}</div>
                <div className="muted" style={{ fontSize: 11.5 }}>{label(user?.role)}</div>
              </span>
              <span className="muted" style={{ display: 'flex' }}><Icon name="chevron" size={13} /></span>
            </button>
            {menuOpen && (
              <>
                <div style={{ position: 'fixed', inset: 0, zIndex: 30 }} onClick={() => setMenuOpen(false)} />
                <div className="card" style={{ position: 'absolute', right: 0, top: 46, width: 220, zIndex: 40, padding: 6, boxShadow: 'var(--shadow-md)' }}>
                  <div style={{ padding: '8px 10px' }} className="muted truncate">{user?.email}</div>
                  <div className="divider" style={{ margin: '4px 0' }} />
                  <button className="nav-item" style={{ width: '100%' }} onClick={() => { setMenuOpen(false); navigate('/profile') }}>
                    <span className="ic"><Icon name="user" size={16} /></span> Profil &amp; sécurité
                  </button>
                  <button className="nav-item" style={{ width: '100%', color: 'var(--red)' }} onClick={onLogout}>
                    <span className="ic" style={{ color: 'var(--red)' }}><Icon name="power" size={16} /></span> Se déconnecter
                  </button>
                </div>
              </>
            )}
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
