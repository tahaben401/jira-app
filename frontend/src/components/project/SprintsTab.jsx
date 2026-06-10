import { useEffect, useState, useCallback } from 'react'
import { sprintsApi } from '../../api/sprints.js'
import { useToast } from '../../context/ToastContext.jsx'
import { Loading, Empty, Badge, Field, Input, Textarea, Select } from '../ui.jsx'
import { Modal, ConfirmDialog } from '../Modal.jsx'
import { SPRINT_STATUS, label } from '../../lib/constants.js'
import { formatDate } from '../../lib/format.js'

export default function SprintsTab({ project, onChange }) {
  const toast = useToast()
  const [sprints, setSprints] = useState([])
  const [active, setActive] = useState(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(null)
  const [delTarget, setDelTarget] = useState(null)
  const [delBusy, setDelBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const list = await sprintsApi.listByProject(project.id)
      setSprints(list || [])
      // Sprint actif (endpoint dédié) — peut ne pas exister.
      try { setActive(await sprintsApi.active(project.id)) } catch { setActive(null) }
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }, [project.id, toast])

  useEffect(() => { load() }, [load])

  const refresh = () => { load(); onChange?.() }

  const changeStatus = async (sprint, status) => {
    try { await sprintsApi.updateStatus(sprint.id, status); toast.success('Statut du sprint mis à jour'); refresh() }
    catch (e) { toast.error(e.message) }
  }

  const remove = async () => {
    setDelBusy(true)
    try { await sprintsApi.remove(delTarget.id); toast.success('Sprint supprimé'); setDelTarget(null); refresh() }
    catch (e) { toast.error(e.message) }
    finally { setDelBusy(false) }
  }

  if (loading) return <Loading />

  return (
    <>
      <div className="flex items-center" style={{ marginBottom: 16 }}>
        <div>
          <h3 style={{ fontSize: 15 }}>Sprints</h3>
          <div className="muted" style={{ fontSize: 13 }}>
            {active ? <>Sprint actif : <strong>{active.name}</strong></> : 'Aucun sprint actif'}
          </div>
        </div>
        <button className="btn btn-primary right" onClick={() => setCreating(true)}>+ Nouveau sprint</button>
      </div>

      {sprints.length === 0 ? (
        <Empty icon="🏃" title="Aucun sprint">Planifiez un sprint pour organiser le travail dans le temps.</Empty>
      ) : (
        <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))' }}>
          {sprints.map((s) => (
            <div key={s.id} className="card card-pad">
              <div className="flex items-center gap-8" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
                <h3 className="truncate" style={{ fontSize: 15 }}>{s.name}</h3>
                <Badge value={s.status} />
              </div>
              <p className="muted" style={{ fontSize: 13, minHeight: 38, marginBottom: 12 }}>
                {s.goal || 'Aucun objectif défini.'}
              </p>
              <div className="detail-row"><span className="k">Période</span><span className="v">{formatDate(s.startDate)} → {formatDate(s.endDate)}</span></div>
              <div className="detail-row"><span className="k">Créé par</span><span className="v">{s.createdByName || `#${s.createdBy}`}</span></div>
              <div className="flex items-center gap-8 mt-16">
                <Select value={s.status} onChange={(e) => changeStatus(s, e.target.value)} style={{ height: 32 }}>
                  {SPRINT_STATUS.map((st) => <option key={st} value={st}>{label(st)}</option>)}
                </Select>
                <button className="btn btn-sm" onClick={() => setEditing(s)}>Modifier</button>
                <button className="btn btn-sm btn-danger" onClick={() => setDelTarget(s)}>Suppr.</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {(creating || editing) && (
        <SprintModal
          projectId={project.id}
          sprint={editing}
          onClose={() => { setCreating(false); setEditing(null) }}
          onSaved={() => { setCreating(false); setEditing(null); refresh() }}
        />
      )}
      {delTarget && (
        <ConfirmDialog
          title="Supprimer le sprint"
          message={`Supprimer le sprint « ${delTarget.name} » ?`}
          confirmLabel="Supprimer" danger busy={delBusy}
          onConfirm={remove} onClose={() => setDelTarget(null)}
        />
      )}
    </>
  )
}

function SprintModal({ projectId, sprint, onClose, onSaved }) {
  const toast = useToast()
  const editing = !!sprint
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    name: sprint?.name || '',
    goal: sprint?.goal || '',
    startDate: sprint?.startDate || '',
    endDate: sprint?.endDate || '',
  })
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const str = (v) => (v === '' ? null : v)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const payload = { name: form.name, goal: str(form.goal), startDate: str(form.startDate), endDate: str(form.endDate) }
      if (editing) await sprintsApi.update(sprint.id, payload)
      else await sprintsApi.create({ projectId, ...payload })
      toast.success(editing ? 'Sprint mis à jour' : 'Sprint créé')
      onSaved()
    } catch (err) {
      toast.error(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal
      title={editing ? 'Modifier le sprint' : 'Nouveau sprint'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={busy}>Annuler</button>
          <button className="btn btn-primary" form="sprint-form" disabled={busy}>
            {busy ? 'Enregistrement…' : editing ? 'Enregistrer' : 'Créer'}
          </button>
        </>
      }
    >
      <form id="sprint-form" onSubmit={submit}>
        <Field label="Nom du sprint">
          <Input required autoFocus minLength={2} maxLength={120} value={form.name}
            onChange={set('name')} placeholder="ex. Sprint 1 — Authentification" />
        </Field>
        <Field label="Objectif" hint="Optionnel">
          <Textarea maxLength={1000} value={form.goal} onChange={set('goal')}
            placeholder="Que veut-on livrer à la fin du sprint ?" />
        </Field>
        <div className="row">
          <Field label="Date de début">
            <Input type="date" value={form.startDate || ''} onChange={set('startDate')} />
          </Field>
          <Field label="Date de fin">
            <Input type="date" value={form.endDate || ''} onChange={set('endDate')} />
          </Field>
        </div>
      </form>
    </Modal>
  )
}
