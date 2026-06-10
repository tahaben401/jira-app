import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth.js'
import { tokens } from '../api/tokens.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { Field, Input, Avatar, Badge, Loading } from '../components/ui.jsx'
import { ConfirmDialog } from '../components/Modal.jsx'
import { label } from '../lib/constants.js'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const [me, setMe] = useState(null)
  const [loading, setLoading] = useState(true)

  const [pwd, setPwd] = useState({ currentPassword: '', newPassword: '' })
  const [pwdBusy, setPwdBusy] = useState(false)

  const [lookupId, setLookupId] = useState('')
  const [lookupResult, setLookupResult] = useState(null)
  const [lookupBusy, setLookupBusy] = useState(false)

  const [confirmLogoutAll, setConfirmLogoutAll] = useState(false)
  const [logoutAllBusy, setLogoutAllBusy] = useState(false)

  useEffect(() => {
    authApi.me()
      .then(setMe)
      .catch((e) => toast.error(e.message))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  const changePassword = async (e) => {
    e.preventDefault()
    setPwdBusy(true)
    try {
      await authApi.changePassword(pwd)
      toast.success('Mot de passe changé. Reconnexion requise.')
      await logout()
      navigate('/login')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setPwdBusy(false)
    }
  }

  const doLogoutAll = async () => {
    setLogoutAllBusy(true)
    try {
      await authApi.logoutAll()
      toast.success('Déconnecté de tous les appareils')
      tokens.clear()
      navigate('/login')
    } catch (err) {
      toast.error(err.message)
      setLogoutAllBusy(false)
      setConfirmLogoutAll(false)
    }
  }

  const doLookup = async (e) => {
    e.preventDefault()
    setLookupBusy(true); setLookupResult(null)
    try {
      const u = await authApi.getUser(lookupId)
      setLookupResult(u)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLookupBusy(false)
    }
  }

  const testRefresh = async () => {
    try {
      const auth = await authApi.refresh(tokens.refresh)
      tokens.setTokens(auth.accessToken, auth.refreshToken)
      toast.success('Tokens rafraîchis (rotation effectuée)')
    } catch (err) {
      toast.error(err.message)
    }
  }

  if (loading) return <Loading />

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Profil &amp; sécurité</h1>
          <div className="sub">Gérez votre compte et vos sessions</div>
        </div>
      </div>

      <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', alignItems: 'start' }}>
        {/* Identité (GET /me) */}
        <div className="card card-pad">
          <div className="flex items-center gap-12" style={{ marginBottom: 16 }}>
            <Avatar name={me?.fullName} size="lg" />
            <div>
              <h3 style={{ fontSize: 17 }}>{me?.fullName}</h3>
              <div className="muted">{me?.email}</div>
            </div>
          </div>
          <div className="detail-row"><span className="k">ID utilisateur</span><span className="v mono">#{me?.id}</span></div>
          <div className="detail-row"><span className="k">Rôle</span><span className="v"><Badge value={me?.role} /></span></div>
          <div className="detail-row"><span className="k">Statut</span><span className="v">{me?.enabled ? <Badge value="ACTIVE" text="Activé" /> : <Badge value="ARCHIVED" text="Désactivé" />}</span></div>
          <div className="note mt-16">
            <strong>GET /api/v1/auth/me</strong> — profil renvoyé d'après l'en-tête <code>X-User-Id</code> injecté par le Gateway.
          </div>
        </div>

        {/* Changement de mot de passe (PUT /change-password) */}
        <div className="card card-pad">
          <h3 style={{ marginBottom: 4 }}>Changer le mot de passe</h3>
          <p className="muted" style={{ marginBottom: 16, fontSize: 13 }}>
            Toutes les sessions seront invalidées après le changement.
          </p>
          <form onSubmit={changePassword}>
            <Field label="Mot de passe actuel">
              <Input type="password" required value={pwd.currentPassword}
                onChange={(e) => setPwd((p) => ({ ...p, currentPassword: e.target.value }))} />
            </Field>
            <Field label="Nouveau mot de passe" hint="8 caractères minimum">
              <Input type="password" required minLength={8} value={pwd.newPassword}
                onChange={(e) => setPwd((p) => ({ ...p, newPassword: e.target.value }))} />
            </Field>
            <button className="btn btn-primary" disabled={pwdBusy}>
              {pwdBusy ? 'Changement…' : 'Mettre à jour'}
            </button>
          </form>
        </div>

        {/* Sessions (POST /logout-all + refresh) */}
        <div className="card card-pad">
          <h3 style={{ marginBottom: 4 }}>Sessions</h3>
          <p className="muted" style={{ marginBottom: 16, fontSize: 13 }}>
            Le backend autorise jusqu'à 5 sessions actives par utilisateur (rotation des refresh tokens).
          </p>
          <div className="flex gap-10">
            <button className="btn" onClick={testRefresh}>↻ Tester le refresh</button>
            <button className="btn btn-danger" onClick={() => setConfirmLogoutAll(true)}>
              Déconnecter tous les appareils
            </button>
          </div>
          <div className="note mt-16">
            <strong>↻ Refresh</strong> appelle <code>POST /auth/refresh</code> et fait tourner le couple de tokens.
            La rotation est aussi déclenchée automatiquement en cas de 401.
          </div>
        </div>

        {/* Recherche d'utilisateur (GET /users/{id}) */}
        <div className="card card-pad">
          <h3 style={{ marginBottom: 4 }}>Rechercher un utilisateur</h3>
          <p className="muted" style={{ marginBottom: 16, fontSize: 13 }}>
            Utile pour récupérer un ID avant d'inviter un membre à un projet.
          </p>
          <form onSubmit={doLookup} className="flex gap-10" style={{ alignItems: 'flex-end' }}>
            <div style={{ flex: 1 }}>
              <Field label="ID utilisateur">
                <Input type="number" min={1} required value={lookupId}
                  onChange={(e) => setLookupId(e.target.value)} placeholder="ex. 2" />
              </Field>
            </div>
            <button className="btn btn-primary" disabled={lookupBusy} style={{ marginBottom: 16 }}>
              {lookupBusy ? '…' : 'Chercher'}
            </button>
          </form>
          {lookupResult && (
            <div className="card-pad" style={{ background: 'var(--surface-2)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
              <div className="flex items-center gap-10">
                <Avatar name={lookupResult.fullName} />
                <div>
                  <div style={{ fontWeight: 600 }}>{lookupResult.fullName} <span className="muted mono">#{lookupResult.id}</span></div>
                  <div className="muted" style={{ fontSize: 12.5 }}>{lookupResult.email} · {label(lookupResult.role)}</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {confirmLogoutAll && (
        <ConfirmDialog
          title="Déconnecter tous les appareils"
          message="Toutes vos sessions actives (y compris celle-ci) seront révoquées. Vous devrez vous reconnecter."
          confirmLabel="Tout déconnecter"
          danger
          busy={logoutAllBusy}
          onConfirm={doLogoutAll}
          onClose={() => setConfirmLogoutAll(false)}
        />
      )}
    </>
  )
}
