import { useEffect, useState, useCallback } from 'react'
import { notificationsApi } from '../api/notifications.js'
import { useToast } from '../context/ToastContext.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { refreshUnread } from '../components/Layout.jsx'
import { Loading, Empty, Badge, Field, Input, Textarea, EnumSelect } from '../components/ui.jsx'
import { Modal } from '../components/Modal.jsx'
import { NOTIF_TYPES, label, badgeColor } from '../lib/constants.js'
import { relativeTime, formatDateTime } from '../lib/format.js'

export default function NotificationsPage() {
  const toast = useToast()
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('all') // 'all' | 'unread'
  const [creating, setCreating] = useState(false)
  const [detail, setDetail] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = filter === 'unread' ? await notificationsApi.unread() : await notificationsApi.list()
      setItems(data || [])
    } catch (e) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [filter, toast])

  useEffect(() => { load() }, [load])

  const after = () => { load(); refreshUnread() }

  const toggleRead = async (n, e) => {
    e?.stopPropagation()
    try { await notificationsApi.markRead(n.id, !n.read); after() }
    catch (err) { toast.error(err.message) }
  }

  const markAll = async () => {
    try { await notificationsApi.markAllRead(); toast.success('Tout marqué comme lu'); after() }
    catch (err) { toast.error(err.message) }
  }

  const remove = async (n, e) => {
    e?.stopPropagation()
    try { await notificationsApi.remove(n.id); toast.success('Notification supprimée'); after() }
    catch (err) { toast.error(err.message) }
  }

  // Ouvre le détail (GET /:id) et marque comme lue si nécessaire.
  const open = async (n) => {
    try {
      const full = await notificationsApi.get(n.id)
      setDetail(full)
      if (!n.read) { await notificationsApi.markRead(n.id, true); after() }
    } catch (err) { toast.error(err.message) }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Notifications</h1>
          <div className="sub">Vos alertes et messages</div>
        </div>
        <div className="flex gap-10">
          <button className="btn" onClick={markAll}>Tout marquer comme lu</button>
          <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Créer (test)</button>
        </div>
      </div>

      <div className="tabs">
        <div className={`tab ${filter === 'all' ? 'active' : ''}`} onClick={() => setFilter('all')}>Toutes</div>
        <div className={`tab ${filter === 'unread' ? 'active' : ''}`} onClick={() => setFilter('unread')}>Non lues</div>
      </div>

      {loading ? (
        <Loading />
      ) : items.length === 0 ? (
        <Empty icon="🔔" title={filter === 'unread' ? 'Aucune notification non lue' : 'Aucune notification'}>
          Utilisez « Créer (test) » pour générer une notification et tester le service.
        </Empty>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          {items.map((n, i) => (
            <div
              key={n.id}
              onClick={() => open(n)}
              style={{
                display: 'flex', gap: 12, padding: '14px 16px', cursor: 'pointer',
                borderTop: i ? '1px solid var(--border)' : 'none',
                background: n.read ? 'var(--surface)' : 'var(--accent-soft)',
              }}
            >
              <span className={`dot badge badge-${badgeColor(n.type)}`}
                style={{ width: 9, height: 9, padding: 0, borderRadius: '50%', marginTop: 6, flexShrink: 0 }} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="flex items-center gap-8">
                  <strong style={{ fontSize: 14 }}>{n.title}</strong>
                  <Badge value={n.type} />
                  {!n.read && <span className="badge badge-blue" style={{ height: 18 }}>Nouveau</span>}
                </div>
                {n.message && <div className="soft" style={{ fontSize: 13, marginTop: 3 }}>{n.message}</div>}
                <div className="muted" style={{ fontSize: 12, marginTop: 5 }}>{relativeTime(n.createdAt)}</div>
              </div>
              <div className="flex gap-6" style={{ alignItems: 'flex-start' }} onClick={(e) => e.stopPropagation()}>
                <button className="btn btn-sm btn-ghost" onClick={(e) => toggleRead(n, e)}>
                  {n.read ? 'Non lue' : 'Lue'}
                </button>
                <button className="btn btn-sm btn-ghost" style={{ color: 'var(--red)' }} onClick={(e) => remove(n, e)}>✕</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {creating && <CreateNotifModal defaultUserId={user?.id} onClose={() => setCreating(false)} onCreated={() => { setCreating(false); after() }} />}

      {detail && (
        <Modal title={detail.title} onClose={() => setDetail(null)}
          footer={<button className="btn" onClick={() => setDetail(null)}>Fermer</button>}>
          <div className="flex items-center gap-8" style={{ marginBottom: 12 }}>
            <Badge value={detail.type} />
            <span className="muted" style={{ fontSize: 12.5 }}>{formatDateTime(detail.createdAt)}</span>
          </div>
          <p style={{ lineHeight: 1.6, margin: 0 }}>{detail.message || <span className="muted">Aucun message.</span>}</p>
          {detail.link && <div className="mt-16"><a href={detail.link} target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>{detail.link}</a></div>}
        </Modal>
      )}
    </>
  )
}

function CreateNotifModal({ defaultUserId, onClose, onCreated }) {
  const toast = useToast()
  const [form, setForm] = useState({ userId: defaultUserId || '', title: '', message: '', type: 'INFO', link: '' })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await notificationsApi.create({
        userId: Number(form.userId),
        title: form.title,
        message: form.message === '' ? null : form.message,
        type: form.type,
        link: form.link === '' ? null : form.link,
      })
      toast.success('Notification créée')
      onCreated()
    } catch (err) { toast.error(err.message); setBusy(false) }
  }

  return (
    <Modal
      title="Créer une notification (test)"
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={busy}>Annuler</button>
          <button className="btn btn-primary" form="notif-form" disabled={busy}>{busy ? 'Création…' : 'Créer'}</button>
        </>
      }
    >
      <div className="note" style={{ marginBottom: 16 }}>
        Destinataire = votre propre ID par défaut. Changez l'ID pour notifier un autre utilisateur.
      </div>
      <form id="notif-form" onSubmit={submit}>
        <Field label="ID destinataire">
          <Input type="number" min={1} required value={form.userId} onChange={set('userId')} />
        </Field>
        <Field label="Titre">
          <Input required minLength={2} maxLength={200} value={form.title} onChange={set('title')} placeholder="ex. Tâche assignée" />
        </Field>
        <Field label="Message" hint="Optionnel">
          <Textarea maxLength={2000} value={form.message} onChange={set('message')} />
        </Field>
        <div className="row">
          <Field label="Type">
            <EnumSelect options={NOTIF_TYPES} value={form.type} onChange={(v) => setForm((f) => ({ ...f, type: v }))} />
          </Field>
          <Field label="Lien" hint="Optionnel">
            <Input maxLength={500} value={form.link} onChange={set('link')} placeholder="/projects/1" />
          </Field>
        </div>
      </form>
    </Modal>
  )
}
