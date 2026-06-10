import { useEffect, useState, useCallback } from 'react'
import { tasksApi } from '../../api/tasks.js'
import { useToast } from '../../context/ToastContext.jsx'
import { Loading, Empty, Badge, Select } from '../ui.jsx'
import { Icon } from '../Icons.jsx'
import TaskModal from './TaskModal.jsx'
import { label, TYPE_ICON } from '../../lib/constants.js'

// Backlog = tâches sans sprint (GET /tasks/backlog).
// On peut planifier une tâche dans un sprint (PUT /tasks/:id { sprintId }).
export default function BacklogTab({ project, members, sprints }) {
  const toast = useToast()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [editTask, setEditTask] = useState(null)
  const [creating, setCreating] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try { setTasks((await tasksApi.backlog(project.id)) || []) }
    catch (e) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [project.id, toast])

  useEffect(() => { load() }, [load])

  const moveToSprint = async (task, sprintId) => {
    try {
      await tasksApi.update(task.id, { sprintId: sprintId === '' ? null : Number(sprintId) })
      toast.success(sprintId ? 'Tâche planifiée dans le sprint' : 'Tâche conservée au backlog')
      load()
    } catch (e) { toast.error(e.message) }
  }

  if (loading) return <Loading />

  return (
    <>
      <div className="flex items-center" style={{ marginBottom: 16 }}>
        <div>
          <h3 style={{ fontSize: 15 }}>Backlog</h3>
          <div className="muted" style={{ fontSize: 13 }}>{tasks.length} tâche{tasks.length > 1 ? 's' : ''} non planifiée{tasks.length > 1 ? 's' : ''}</div>
        </div>
        <button className="btn btn-primary right" onClick={() => setCreating(true)}>+ Nouvelle tâche</button>
      </div>

      {tasks.length === 0 ? (
        <Empty icon={<Icon name="list" size={22} />} title="Backlog vide">Toutes les tâches sont déjà planifiées dans un sprint.</Empty>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: 50 }}>#</th>
                <th>Titre</th>
                <th style={{ width: 90 }}>Type</th>
                <th style={{ width: 110 }}>Priorité</th>
                <th style={{ width: 160 }}>Assigné</th>
                <th style={{ width: 220 }}>Planifier dans</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((t) => (
                <tr key={t.id} style={{ cursor: 'pointer' }}>
                  <td className="mono muted" onClick={() => setEditTask(t)}>{t.id}</td>
                  <td onClick={() => setEditTask(t)}>{TYPE_ICON[t.type]} {t.title}</td>
                  <td onClick={() => setEditTask(t)}><Badge value={t.type} /></td>
                  <td onClick={() => setEditTask(t)}><Badge value={t.priority} /></td>
                  <td onClick={() => setEditTask(t)}>{t.assigneeName || <span className="muted">—</span>}</td>
                  <td>
                    <Select value="" onChange={(e) => moveToSprint(t, e.target.value)} disabled={sprints.length === 0}>
                      <option value="">{sprints.length ? 'Choisir un sprint…' : 'Aucun sprint'}</option>
                      {sprints.map((s) => <option key={s.id} value={s.id}>{s.name} · {label(s.status)}</option>)}
                    </Select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {creating && (
        <TaskModal projectId={project.id} members={members} sprints={sprints}
          onClose={() => setCreating(false)} onSaved={() => { setCreating(false); load() }} />
      )}
      {editTask && (
        <TaskModal projectId={project.id} task={editTask} members={members} sprints={sprints}
          onClose={() => setEditTask(null)} onSaved={(silent) => { if (!silent) setEditTask(null); load() }} />
      )}
    </>
  )
}
