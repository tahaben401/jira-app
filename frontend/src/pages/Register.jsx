import { useState } from 'react'
import { Link, useNavigate, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { Field, Input, EnumSelect } from '../components/ui.jsx'
import { Logo } from '../components/Icons.jsx'
import { USER_ROLES } from '../lib/constants.js'

export default function Register() {
  const { register, isAuthenticated } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'DEVELOPER' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (isAuthenticated) return <Navigate to="/projects" replace />

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target ? e.target.value : e }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await register(form)
      toast.success('Compte créé avec succès')
      navigate('/projects', { replace: true })
    } catch (err) {
      setError(err.message || 'Échec de la création du compte')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand">
          <Logo size={34} />
          <span className="wordmark">Atlas</span>
        </div>
        <div className="card card-pad">
          <h1>Créer un compte</h1>
          <p className="sub">Quelques informations pour démarrer</p>

          {error && <div className="note" style={{ background: 'var(--red-soft)', borderColor: '#f0caca', color: 'var(--red)', marginBottom: 16 }}>{error}</div>}

          <form onSubmit={submit}>
            <Field label="Nom complet">
              <Input required autoFocus value={form.fullName}
                onChange={set('fullName')} placeholder="Marie Dupont" minLength={2} />
            </Field>
            <Field label="Adresse e-mail">
              <Input type="email" required value={form.email}
                onChange={set('email')} placeholder="vous@exemple.com" />
            </Field>
            <Field label="Mot de passe" hint="8 caractères minimum">
              <Input type="password" required minLength={8} value={form.password}
                onChange={set('password')} placeholder="••••••••" />
            </Field>
            <Field label="Rôle">
              <EnumSelect options={USER_ROLES} value={form.role}
                onChange={(v) => setForm((f) => ({ ...f, role: v }))} />
            </Field>
            <button className="btn btn-primary btn-block" disabled={busy} style={{ marginTop: 6 }}>
              {busy ? 'Création…' : 'Créer mon compte'}
            </button>
          </form>
        </div>
        <p className="auth-foot">
          Déjà inscrit ? <Link to="/login">Se connecter</Link>
        </p>
      </div>
    </div>
  )
}
