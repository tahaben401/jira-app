import { useEffect, useState, useCallback } from 'react'
import { tasksApi } from '../../api/tasks.js'
import { useToast } from '../../context/ToastContext.jsx'
import { Loading, Empty, Badge, Avatar, EnumSelect, Select } from '../ui.jsx'
import TaskModal from './TaskModal.jsx'
import { TASK_STATUS, TASK_TYPES, label, badgeColor, TYPE_ICON } from '../../lib/constants.js'

export default function BoardTab({ project, members, sprints }) {
  const toast = useToast()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [typeFilter, setTypeFilter] = useState('')
  const [view, setView] = useState('all') // 'all' | 'backlog' | sprintId
  const [editTask, setEditTask] = useState(null)
  const [creating, setCreating] = useState(false)
  const [dragId, setDragId] = useState(null)
  const [dropCol, setDropCol] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      let data
      if (view === 'backlog') data = await tasksApi.backlog(project.id)
      else if (view === 'all') data = await tasksApi.listByProject(project.id, { type: typeFilter })
      else data = await tasksApi.bySprint(Number(view))
      // Le filtre type côté serveur ne s'applique qu'en vue "projet".
      if (view !== 'all' && typeFilter) data = data.filter((t) => t.type === typeFilter)
      setTasks(data || [])
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }, [project.id, view, typeFilter, toast])

  useEffect(() => { load() }, [load])

  const onSaved = (silent) => { if (!silent) { setEditTask(null); setCreating(false) } load() }

  // ── Drag & drop : changement de statut ──────────────────────────────────
  const onDrop = async (status) => {
    setDropCol(null)
    const t = tasks.find((x) => x.id === dragId)
    setDragId(null)
    if (!t || t.status === status) return
    setTasks((prev) => prev.map((x) => (x.id === t.id ? { ...x, status } : x))) // optimiste
    try {
      await tasksApi.updateStatus(t.id, status)
    } catch (e) {
      toast.error(e.message)
      load()
    }
  }

  if (loading) return <Loading />

  return (
    <>
      <div className="flex items-center gap-10" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
        <div style={{ width: 200 }}>
          <Select value={view} onChange={(e) => setView(e.target.value)}>
            <option value="all">Toutes les tâches</option>
            <option value="backlog">Backlog uniquement</option>
            {sprints.map((s) => <option key={s.id} value={s.id}>Sprint : {s.name}</option>)}
          </Select>
        </div>
        <div style={{ width: 170 }}>
          <Select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="">Tous les types</option>
            {TASK_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
          </Select>
        </div>
        <span className="muted" style={{ fontSize: 13 }}>{tasks.length} tâche{tasks.length > 1 ? 's' : ''}</span>
        <button className="btn btn-primary right" onClick={() => setCreating(true)}>+ Nouvelle tâche</button>
      </div>

      <div className="board">
        {TASK_STATUS.map((status) => {
          const items = tasks.filter((t) => t.status === status)
          return (
            <div
              key={status}
              className={`col ${dropCol === status ? 'drop-over' : ''}`}
              onDragOver={(e) => { e.preventDefault(); setDropCol(status) }}
              onDragLeave={() => setDropCol((c) => (c === status ? null : c))}
              onDrop={() => onDrop(status)}
            >
              <div className="col-head">
                <span className={`dot badge badge-${badgeColor(status)}`} style={{ width: 8, height: 8, padding: 0, borderRadius: '50%' }} />
                {label(status)}
                <span className="n">{items.length}</span>
              </div>
              <div className="col-body">
                {items.map((t) => (
                  <div
                    key={t.id}
                    className={`task-card ${dragId === t.id ? 'dragging' : ''}`}
                    draggable
                    onDragStart={() => setDragId(t.id)}
                    onDragEnd={() => { setDragId(null); setDropCol(null) }}
                    onClick={() => setEditTask(t)}
                  >
                    <div className="tc-top">
                      <span title={label(t.type)}>{TYPE_ICON[t.type]}</span>
                      <Badge value={t.priority} text={label(t.priority)} />
                      {t.storyPoints != null && <span className="badge badge-slate" style={{ height: 20 }}>{t.storyPoints} pts</span>}
                    </div>
                    <div className="tc-title">{t.title}</div>
                    <div className="tc-foot">
                      <span className="tc-key">#{t.id}</span>
                      <span className="right" />
                      {t.assigneeName ? <Avatar name={t.assigneeName} /> : <span className="muted" style={{ fontSize: 11.5 }}>Non assigné</span>}
                    </div>
                  </div>
                ))}
                {items.length === 0 && <div className="muted" style={{ fontSize: 12.5, padding: '6px 4px' }}>Déposez une tâche ici</div>}
              </div>
            </div>
          )
        })}
      </div>

      {tasks.length === 0 && (
        <Empty icon="☑" title="Aucune tâche dans cette vue">Créez une tâche pour la voir apparaître sur le tableau.</Empty>
      )}

      {creating && (
        <TaskModal projectId={project.id} members={members} sprints={sprints}
          onClose={() => setCreating(false)} onSaved={onSaved} />
      )}
      {editTask && (
        <TaskModal projectId={project.id} task={editTask} members={members} sprints={sprints}
          onClose={() => setEditTask(null)} onSaved={onSaved} />
      )}
    </>
  )
}
