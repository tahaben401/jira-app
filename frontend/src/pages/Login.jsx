import { useState } from 'react'
import { Link, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { Field, Input } from '../components/ui.jsx'

export default function Login() {
  const { login, isAuthenticated } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (isAuthenticated) return <Navigate to="/projects" replace />

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await login(email, password)
      toast.success('Connexion réussie')
      navigate(location.state?.from || '/projects', { replace: true })
    } catch (err) {
      setError(err.message || 'Identifiants invalides')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="brand-mark">A</span> Atlas
        </div>
        <div className="card card-pad">
          <h1>Content de vous revoir</h1>
          <p className="sub">Connectez-vous pour accéder à vos projets</p>

          {error && <div className="note" style={{ background: 'var(--red-soft)', borderColor: '#f0caca', color: 'var(--red)', marginBottom: 16 }}>{error}</div>}

          <form onSubmit={submit}>
            <Field label="Adresse e-mail">
              <Input type="email" required autoFocus value={email}
                onChange={(e) => setEmail(e.target.value)} placeholder="vous@exemple.com" />
            </Field>
            <Field label="Mot de passe">
              <Input type="password" required value={password}
                onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
            </Field>
            <button className="btn btn-primary btn-block" disabled={busy} style={{ marginTop: 6 }}>
              {busy ? 'Connexion…' : 'Se connecter'}
            </button>
          </form>
        </div>
        <p className="auth-foot">
          Pas encore de compte ? <Link to="/register">Créer un compte</Link>
        </p>
      </div>
    </div>
  )
}
