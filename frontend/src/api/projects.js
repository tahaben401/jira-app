import { api } from './client.js'

// project-service — /api/v1/projects/**
export const projectsApi = {
  list: () => api.get('/projects'),
  get: (id) => api.get(`/projects/${id}`),
  create: (payload) => api.post('/projects', payload),
  update: (id, payload) => api.put(`/projects/${id}`, payload),
  remove: (id) => api.del(`/projects/${id}`),

  inviteMember: (id, payload) => api.post(`/projects/${id}/members`, payload),
  // NB : le backend attend le userId dans le segment {memberId} (et non l'id d'adhésion).
  removeMember: (id, userId) => api.del(`/projects/${id}/members/${userId}`),
  updateMemberRole: (id, userId, role) =>
    api.put(`/projects/${id}/members/${userId}/role`, { role }),
}
