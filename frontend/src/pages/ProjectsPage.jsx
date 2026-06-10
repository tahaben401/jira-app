import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectsApi } from '../api/projects.js'
import { useToast } from '../context/ToastContext.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { Loading, Empty, Badge, Field, Input, Textarea } from '../components/ui.jsx'
import { Icon } from '../components/Icons.jsx'
import { Modal } from '../components/Modal.jsx'

export default function ProjectsPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)

  const load = () => {
    setLoading(true)
    projectsApi.list()
      .then((d) => setProjects(d || []))
      .catch((e) => toast.error(e.message))
      .finally(() => setLoading(false))
  }
  useEffect(load, []) // eslint-disable-line

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Projets</h1>
          <div className="sub">Les projets que vous possédez ou dont vous êtes membre</div>
        </div>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Nouveau projet</button>
      </div>

      {loading ? (
        <Loading />
      ) : projects.length === 0 ? (
        <Empty icon={<Icon name="board" size={22} />} title="Aucun projet pour l'instant">
          Créez votre premier projet pour commencer à organiser le travail.
          <div className="mt-16"><button className="btn btn-primary" onClick={() => setCreating(true)}>+ Nouveau projet</button></div>
        </Empty>
      ) : (
        <div className="grid grid-cards">
          {projects.map((p) => (
            <div key={p.id} className="card proj-card" onClick={() => navigate(`/projects/${p.id}`)}>
              <div className="flex items-center gap-8" style={{ justifyContent: 'space-between' }}>
                <h3 className="truncate">{p.name}</h3>
                <Badge value={p.status} />
              </div>
              <p className="desc">{p.description || 'Aucune description.'}</p>
              <div className="meta">
                <span className="flex items-center gap-6"><span className="ic"><Icon name="users" size={14} /></span>{p.memberCount} membre{p.memberCount > 1 ? 's' : ''}</span>
                {p.ownerId === user?.id && <span className="badge badge-slate right">Propriétaire</span>}
              </div>
            </div>
          ))}
        </div>
      )}

      {creating && <CreateProjectModal onClose={() => setCreating(false)} onCreated={() => { setCreating(false); load() }} />}
    </>
  )
}

function CreateProjectModal({ onClose, onCreated }) {
  const toast = useToast()
  const [form, setForm] = useState({ name: '', description: '' })
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await projectsApi.create(form)
      toast.success('Projet créé')
      onCreated()
    } catch (err) {
      toast.error(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal
      title="Nouveau projet"
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={busy}>Annuler</button>
          <button className="btn btn-primary" form="create-project" disabled={busy}>
            {busy ? 'Création…' : 'Créer le projet'}
          </button>
        </>
      }
    >
      <form id="create-project" onSubmit={submit}>
        <Field label="Nom du projet">
          <Input required autoFocus minLength={2} maxLength={100} value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="ex. Plateforme e-commerce" />
        </Field>
        <Field label="Description" hint="Optionnel · 500 caractères max">
          <Textarea maxLength={500} value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            placeholder="À quoi sert ce projet ?" />
        </Field>
      </form>
    </Modal>
  )
}
