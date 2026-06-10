import { useState } from 'react'
import { projectsApi } from '../../api/projects.js'
import { authApi } from '../../api/auth.js'
import { useToast } from '../../context/ToastContext.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { Empty, Badge, Avatar, Field, Input, EnumSelect, Select } from '../ui.jsx'
import { Modal, ConfirmDialog } from '../Modal.jsx'
import { MEMBER_ROLES, label } from '../../lib/constants.js'
import { formatDate } from '../../lib/format.js'

// Gestion des membres : invitation (POST), changement de rôle (PUT), retrait (DELETE).
// Le backend identifie un membre par son userId dans l'URL.
export default function MembersTab({ project, canManage, onChange }) {
  const toast = useToast()
  const { user } = useAuth()
  const [inviting, setInviting] = useState(false)
  const [delTarget, setDelTarget] = useState(null)
  const [delBusy, setDelBusy] = useState(false)
  const members = project.members || []

  const changeRole = async (m, role) => {
    try { await projectsApi.updateMemberRole(project.id, m.userId, role); toast.success('Rôle mis à jour'); onChange() }
    catch (e) { toast.error(e.message) }
  }

  const remove = async () => {
    setDelBusy(true)
    try { await projectsApi.removeMember(project.id, delTarget.userId); toast.success('Membre retiré'); setDelTarget(null); onChange() }
    catch (e) { toast.error(e.message) }
    finally { setDelBusy(false) }
  }

  return (
    <>
      <div className="flex items-center" style={{ marginBottom: 16 }}>
        <div>
          <h3 style={{ fontSize: 15 }}>Membres</h3>
          <div className="muted" style={{ fontSize: 13 }}>{members.length} membre{members.length > 1 ? 's' : ''} · propriétaire #{project.ownerId}</div>
        </div>
        {canManage && <button className="btn btn-primary right" onClick={() => setInviting(true)}>+ Inviter un membre</button>}
      </div>

      {members.length === 0 ? (
        <Empty icon="👥" title="Aucun membre">
          {canManage ? 'Invitez des collaborateurs pour partager ce projet.' : 'Le propriétaire n’a invité personne pour le moment.'}
        </Empty>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Membre</th>
                <th style={{ width: 80 }}>User ID</th>
                <th style={{ width: 200 }}>Rôle</th>
                <th style={{ width: 140 }}>Depuis</th>
                {canManage && <th style={{ width: 80 }} />}
              </tr>
            </thead>
            <tbody>
              {members.map((m) => (
                <tr key={m.userId}>
                  <td>
                    <div className="flex items-center gap-10">
                      <Avatar name={m.fullName} />
                      <div>
                        <div style={{ fontWeight: 600 }}>{m.fullName || 'Inconnu'} {m.userId === user?.id && <span className="muted">(vous)</span>}</div>
                        <div className="muted" style={{ fontSize: 12.5 }}>{m.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="mono muted">{m.userId}</td>
                  <td>
                    {canManage ? (
                      <Select value={m.role} onChange={(e) => changeRole(m, e.target.value)} style={{ height: 32 }}>
                        {MEMBER_ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
                      </Select>
                    ) : <Badge value={m.role} />}
                  </td>
                  <td className="muted">{formatDate(m.joinedAt)}</td>
                  {canManage && (
                    <td><button className="btn btn-sm btn-danger" onClick={() => setDelTarget(m)}>Retirer</button></td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {inviting && <InviteModal projectId={project.id} onClose={() => setInviting(false)} onInvited={() => { setInviting(false); onChange() }} />}
      {delTarget && (
        <ConfirmDialog
          title="Retirer le membre"
          message={`Retirer ${delTarget.fullName || `l'utilisateur #${delTarget.userId}`} du projet ?`}
          confirmLabel="Retirer" danger busy={delBusy}
          onConfirm={remove} onClose={() => setDelTarget(null)}
        />
      )}
    </>
  )
}

function InviteModal({ projectId, onClose, onInvited }) {
  const toast = useToast()
  const [form, setForm] = useState({ userId: '', role: 'DEVELOPER' })
  const [busy, setBusy] = useState(false)
  const [preview, setPreview] = useState(null)

  // Recherche l'utilisateur pour confirmer son identité avant l'invitation.
  const lookup = async () => {
    if (!form.userId) return
    setPreview(null)
    try { setPreview(await authApi.getUser(form.userId)) }
    catch { setPreview({ notFound: true }) }
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await projectsApi.inviteMember(projectId, { userId: Number(form.userId), role: form.role })
      toast.success('Membre invité')
      onInvited()
    } catch (err) {
      toast.error(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal
      title="Inviter un membre"
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={busy}>Annuler</button>
          <button className="btn btn-primary" form="invite-form" disabled={busy}>{busy ? 'Invitation…' : 'Inviter'}</button>
        </>
      }
    >
      <div className="note" style={{ marginBottom: 16 }}>
        Indiquez l'<strong>ID utilisateur</strong> de la personne à inviter. Vous pouvez le retrouver via « Profil → Rechercher un utilisateur ».
      </div>
      <form id="invite-form" onSubmit={submit}>
        <Field label="ID utilisateur">
          <div className="flex gap-8">
            <Input type="number" min={1} required value={form.userId}
              onChange={(e) => { setForm((f) => ({ ...f, userId: e.target.value })); setPreview(null) }}
              placeholder="ex. 2" />
            <button type="button" className="btn" onClick={lookup}>Vérifier</button>
          </div>
        </Field>
        {preview && (preview.notFound
          ? <div className="err" style={{ marginTop: -8, marginBottom: 12 }}>Aucun utilisateur avec cet ID.</div>
          : <div className="flex items-center gap-10" style={{ marginBottom: 16, padding: 10, background: 'var(--surface-2)', borderRadius: 6, border: '1px solid var(--border)' }}>
              <Avatar name={preview.fullName} />
              <div><div style={{ fontWeight: 600 }}>{preview.fullName}</div><div className="muted" style={{ fontSize: 12.5 }}>{preview.email}</div></div>
            </div>
        )}
        <Field label="Rôle dans le projet">
          <EnumSelect options={MEMBER_ROLES} value={form.role} onChange={(v) => setForm((f) => ({ ...f, role: v }))} />
        </Field>
      </form>
    </Modal>
  )
}
