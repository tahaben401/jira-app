import { useState } from 'react'
import { tasksApi } from '../../api/tasks.js'
import { useToast } from '../../context/ToastContext.jsx'
import { Modal, ConfirmDialog } from '../Modal.jsx'
import { Field, Input, Textarea, Select, EnumSelect, Badge, Avatar } from '../ui.jsx'
import { TASK_TYPES, TASK_PRIORITY, TASK_STATUS, label, TYPE_ICON } from '../../lib/constants.js'
import { formatDateTime } from '../../lib/format.js'

// Modal unique pour créer OU éditer une tâche.
// - Création : POST /tasks
// - Édition  : PUT /tasks/:id  (+ actions rapides PATCH status / PATCH assign)
// - Suppression : DELETE /tasks/:id
export default function TaskModal({ projectId, task, members = [], sprints = [], onClose, onSaved }) {
  const toast = useToast()
  const editing = !!task
  const [busy, setBusy] = useState(false)
  const [confirmDel, setConfirmDel] = useState(false)

  const [form, setForm] = useState({
    title: task?.title || '',
    description: task?.description || '',
    type: task?.type || 'TASK',
    priority: task?.priority || 'MEDIUM',
    storyPoints: task?.storyPoints ?? '',
    dueDate: task?.dueDate || '',
    assigneeId: task?.assigneeId ?? '',
    sprintId: task?.sprintId ?? '',
    status: task?.status || 'TODO',
  })
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))

  const num = (v) => (v === '' || v === null ? null : Number(v))
  const str = (v) => (v === '' ? null : v)

  const save = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      if (editing) {
        await tasksApi.update(task.id, {
          title: form.title,
          description: str(form.description),
          type: form.type,
          status: form.status,
          priority: form.priority,
          storyPoints: num(form.storyPoints),
          dueDate: str(form.dueDate),
          assigneeId: num(form.assigneeId),
          sprintId: num(form.sprintId),
        })
        toast.success('Tâche mise à jour')
      } else {
        await tasksApi.create({
          title: form.title,
          description: str(form.description),
          projectId,
          assigneeId: num(form.assigneeId),
          type: form.type,
          priority: form.priority,
          storyPoints: num(form.storyPoints),
          dueDate: str(form.dueDate),
        })
        toast.success('Tâche créée')
      }
      onSaved()
    } catch (err) {
      toast.error(err.message)
      setBusy(false)
    }
  }

  // Action rapide : changement de statut (PATCH /tasks/:id/status)
  const quickStatus = async (status) => {
    set('status', status)
    if (!editing) return
    try { await tasksApi.updateStatus(task.id, status); toast.success('Statut mis à jour'); onSaved(true) }
    catch (err) { toast.error(err.message) }
  }

  // Action rapide : assignation (PATCH /tasks/:id/assign)
  const quickAssign = async (val) => {
    set('assigneeId', val)
    if (!editing) return
    try { await tasksApi.assign(task.id, val === '' ? null : Number(val)); toast.success('Assignation mise à jour'); onSaved(true) }
    catch (err) { toast.error(err.message) }
  }

  const remove = async () => {
    setBusy(true)
    try { await tasksApi.remove(task.id); toast.success('Tâche supprimée'); onSaved() }
    catch (err) { toast.error(err.message); setBusy(false); setConfirmDel(false) }
  }

  return (
    <>
      <Modal
        wide
        title={editing ? <span><span className="mono muted">#{task.id}</span> {TYPE_ICON[form.type]} Modifier la tâche</span> : 'Nouvelle tâche'}
        onClose={onClose}
        footer={
          <>
            {editing && <button className="btn btn-danger right" onClick={() => setConfirmDel(true)} disabled={busy}>Supprimer</button>}
            <button className="btn" onClick={onClose} disabled={busy}>Fermer</button>
            <button className="btn btn-primary" form="task-form" disabled={busy}>
              {busy ? 'Enregistrement…' : editing ? 'Enregistrer' : 'Créer la tâche'}
            </button>
          </>
        }
      >
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 20 }}>
          {/* Colonne principale */}
          <form id="task-form" onSubmit={save}>
            <Field label="Titre">
              <Input required autoFocus minLength={2} maxLength={200} value={form.title}
                onChange={(e) => set('title', e.target.value)} placeholder="Résumé court de la tâche" />
            </Field>
            <Field label="Description" hint="2000 caractères max">
              <Textarea maxLength={2000} value={form.description}
                onChange={(e) => set('description', e.target.value)}
                placeholder="Détails, critères d'acceptation…" style={{ minHeight: 130 }} />
            </Field>
          </form>

          {/* Colonne latérale : métadonnées + actions rapides */}
          <div>
            {editing && (
              <Field label="Statut">
                <Select value={form.status} onChange={(e) => quickStatus(e.target.value)}>
                  {TASK_STATUS.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                </Select>
              </Field>
            )}
            <div className="row">
              <Field label="Type">
                <EnumSelect options={TASK_TYPES} value={form.type} onChange={(v) => set('type', v)} />
              </Field>
              <Field label="Priorité">
                <EnumSelect options={TASK_PRIORITY} value={form.priority} onChange={(v) => set('priority', v)} />
              </Field>
            </div>
            <Field label="Assigné à">
              <Select value={form.assigneeId} onChange={(e) => quickAssign(e.target.value)}>
                <option value="">Non assigné</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>{m.fullName} ({label(m.role)})</option>
                ))}
              </Select>
            </Field>
            <Field label="Sprint" hint={editing ? 'Modifié à l’enregistrement' : 'Disponible après création'}>
              <Select value={form.sprintId} onChange={(e) => set('sprintId', e.target.value)} disabled={!editing}>
                <option value="">Backlog (aucun sprint)</option>
                {sprints.map((s) => <option key={s.id} value={s.id}>{s.name} · {label(s.status)}</option>)}
              </Select>
            </Field>
            <div className="row">
              <Field label="Points">
                <Input type="number" min={1} max={100} value={form.storyPoints}
                  onChange={(e) => set('storyPoints', e.target.value)} placeholder="—" />
              </Field>
              <Field label="Échéance">
                <Input type="date" value={form.dueDate || ''} onChange={(e) => set('dueDate', e.target.value)} />
              </Field>
            </div>

            {editing && (
              <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                {task.reporterName && <div>Créée par {task.reporterName}</div>}
                <div>Le {formatDateTime(task.createdAt)}</div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {confirmDel && (
        <ConfirmDialog
          title="Supprimer la tâche"
          message={`Supprimer « ${task.title} » ? Cette action est irréversible.`}
          confirmLabel="Supprimer" danger busy={busy}
          onConfirm={remove} onClose={() => setConfirmDel(false)}
        />
      )}
    </>
  )
}
