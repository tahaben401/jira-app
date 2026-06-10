import { api } from './client.js'

// sprint-service — /api/v1/sprints/**
export const sprintsApi = {
  create: (payload) => api.post('/sprints', payload),
  get: (id) => api.get(`/sprints/${id}`),
  listByProject: (projectId) => api.get('/sprints', { query: { projectId } }),
  active: (projectId) => api.get('/sprints/active', { query: { projectId } }),
  update: (id, payload) => api.put(`/sprints/${id}`, payload),
  updateStatus: (id, status) => api.patch(`/sprints/${id}/status`, { status }),
  remove: (id) => api.del(`/sprints/${id}`),
}
