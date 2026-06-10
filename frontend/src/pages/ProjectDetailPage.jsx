import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { projectsApi } from '../api/projects.js'
import { sprintsApi } from '../api/sprints.js'
import { useToast } from '../context/ToastContext.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { Loading, Empty, Badge, Field, Input, Textarea, EnumSelect } from '../components/ui.jsx'
import { Icon } from '../components/Icons.jsx'
import { ConfirmDialog } from '../components/Modal.jsx'
import { PROJECT_STATUS } from '../lib/constants.js'
import BoardTab from '../components/project/BoardTab.jsx'
import BacklogTab from '../components/project/BacklogTab.jsx'
import SprintsTab from '../components/project/SprintsTab.jsx'
import MembersTab from '../components/project/MembersTab.jsx'

const TABS = ['board', 'backlog', 'sprints', 'members', 'settings']
const TAB_LABEL = { board: 'Tableau', backlog: 'Backlog', sprints: 'Sprints', members: 'Membres', settings: 'Paramètres' }

export default function ProjectDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()

  const [project, setProject] = useState(null)
  const [sprints, setSprints] = useState([])
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [tab, setTab] = useState('board')

  const loadProject = useCallback(async () => {
    try {
      const p = await projectsApi.get(id)
      setProject(p)
    } catch (e) {
      if (e.status === 404 || e.status === 403) setNotFound(true)
      else toast.error(e.message)
    }
  }, [id, toast])

  const loadSprints = useCallback(async () => {
    try { setSprints((await sprintsApi.listByProject(id)) || []) } catch { /* non bloquant */ }
  }, [id])

  useEffect(() => {
    setLoading(true)
    Promise.all([loadProject(), loadSprints()]).finally(() => setLoading(false))
  }, [loadProject, loadSprints])

  if (loading) return <Loading />
  if (notFound) {
    return <Empty icon={<Icon name="lock" size={22} />} title="Projet introuvable">
      Ce projet n'existe pas ou vous n'y avez pas accès.
      <div className="mt-16"><Link className="btn" to="/projects">← Retour aux projets</Link></div>
    </Empty>
  }
  if (!project) return null

  const isOwner = project.ownerId === user?.id
  const isScrumMaster = (project.members || []).some((m) => m.userId === user?.id && m.role === 'SCRUM_MASTER')
  const canManageMembers = isOwner || isScrumMaster

  const members = project.members || []

  return (
    <>
      <div className="muted" style={{ fontSize: 13, marginBottom: 8 }}>
        <Link to="/projects" style={{ color: 'var(--accent)' }}>Projets</Link> / {project.name}
      </div>
      <div className="page-head">
        <div>
          <div className="flex items-center gap-10">
            <h1>{project.name}</h1>
            <Badge value={project.status} />
          </div>
          <div className="sub">{project.description || 'Aucune description.'}</div>
        </div>
      </div>

      <div className="tabs">
        {TABS.map((t) => {
          if (t === 'settings' && !isOwner) return null
          return (
            <div key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
              {TAB_LABEL[t]}
            </div>
          )
        })}
      </div>

      {tab === 'board' && <BoardTab project={project} members={members} sprints={sprints} />}
      {tab === 'backlog' && <BacklogTab project={project} members={members} sprints={sprints} />}
      {tab === 'sprints' && <SprintsTab project={project} onChange={loadSprints} />}
      {tab === 'members' && <MembersTab project={project} canManage={canManageMembers} onChange={loadProject} />}
      {tab === 'settings' && isOwner && (
        <SettingsTab project={project} onUpdated={loadProject} onDeleted={() => navigate('/projects')} />
      )}
    </>
  )
}

function SettingsTab({ project, onUpdated, onDeleted }) {
  const toast = useToast()
  const [form, setForm] = useState({ name: project.name, description: project.description || '', status: project.status })
  const [busy, setBusy] = useState(false)
  const [confirmDel, setConfirmDel] = useState(false)
  const [delBusy, setDelBusy] = useState(false)

  const save = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await projectsApi.update(project.id, {
        name: form.name,
        description: form.description === '' ? null : form.description,
        status: form.status,
      })
      toast.success('Projet mis à jour')
      onUpdated()
    } catch (err) { toast.error(err.message) }
    finally { setBusy(false) }
  }

  const remove = async () => {
    setDelBusy(true)
    try { await projectsApi.remove(project.id); toast.success('Projet supprimé'); onDeleted() }
    catch (err) { toast.error(err.message); setDelBusy(false); setConfirmDel(false) }
  }

  return (
    <div style={{ maxWidth: 560 }}>
      <div className="card card-pad">
        <h3 style={{ marginBottom: 16 }}>Détails du projet</h3>
        <form onSubmit={save}>
          <Field label="Nom">
            <Input required minLength={2} maxLength={100} value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </Field>
          <Field label="Description">
            <Textarea maxLength={500} value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
          </Field>
          <Field label="Statut">
            <EnumSelect options={PROJECT_STATUS} value={form.status}
              onChange={(v) => setForm((f) => ({ ...f, status: v }))} />
          </Field>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Enregistrement…' : 'Enregistrer'}</button>
        </form>
      </div>

      <div className="card card-pad mt-24" style={{ borderColor: '#f0caca' }}>
        <h3 style={{ marginBottom: 4, color: 'var(--red)' }}>Zone de danger</h3>
        <p className="muted" style={{ fontSize: 13, marginBottom: 14 }}>
          La suppression du projet est définitive et retire tous ses membres.
        </p>
        <button className="btn btn-danger" onClick={() => setConfirmDel(true)}>Supprimer ce projet</button>
      </div>

      {confirmDel && (
        <ConfirmDialog
          title="Supprimer le projet"
          message={`Supprimer définitivement « ${project.name} » ?`}
          confirmLabel="Supprimer" danger busy={delBusy}
          onConfirm={remove} onClose={() => setConfirmDel(false)}
        />
      )}
    </div>
  )
}
