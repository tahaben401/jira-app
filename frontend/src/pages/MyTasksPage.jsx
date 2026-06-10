import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { tasksApi } from '../api/tasks.js'
import { useToast } from '../context/ToastContext.jsx'
import { Loading, Empty, Badge, Select } from '../components/ui.jsx'
import { TASK_STATUS, label, TYPE_ICON } from '../lib/constants.js'
import { formatDate } from '../lib/format.js'

export default function MyTasksPage() {
  const toast = useToast()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    tasksApi.mine()
      .then((d) => setTasks(d || []))
      .catch((e) => toast.error(e.message))
      .finally(() => setLoading(false))
  }
  useEffect(load, []) // eslint-disable-line

  const changeStatus = async (t, status) => {
    setTasks((prev) => prev.map((x) => (x.id === t.id ? { ...x, status } : x)))
    try { await tasksApi.updateStatus(t.id, status); toast.success('Statut mis à jour') }
    catch (e) { toast.error(e.message); load() }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Mes tâches</h1>
          <div className="sub">Toutes les tâches qui vous sont assignées, tous projets confondus</div>
        </div>
      </div>

      {loading ? (
        <Loading />
      ) : tasks.length === 0 ? (
        <Empty icon="☑" title="Rien ne vous est assigné">Les tâches qui vous seront attribuées apparaîtront ici.</Empty>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: 50 }}>#</th>
                <th>Titre</th>
                <th style={{ width: 90 }}>Type</th>
                <th style={{ width: 110 }}>Priorité</th>
                <th style={{ width: 120 }}>Échéance</th>
                <th style={{ width: 170 }}>Statut</th>
                <th style={{ width: 90 }}>Projet</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((t) => (
                <tr key={t.id}>
                  <td className="mono muted">{t.id}</td>
                  <td>{TYPE_ICON[t.type]} {t.title}</td>
                  <td><Badge value={t.type} /></td>
                  <td><Badge value={t.priority} /></td>
                  <td className="muted">{t.dueDate ? formatDate(t.dueDate) : '—'}</td>
                  <td>
                    <Select value={t.status} onChange={(e) => changeStatus(t, e.target.value)} style={{ height: 32 }}>
                      {TASK_STATUS.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                    </Select>
                  </td>
                  <td><Link to={`/projects/${t.projectId}`} style={{ color: 'var(--accent)' }}>#{t.projectId}</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
