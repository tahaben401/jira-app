import { api } from './client.js'

// task-service — /api/v1/tasks/**
export const tasksApi = {
  create: (payload) => api.post('/tasks', payload),
  get: (id) => api.get(`/tasks/${id}`),
  listByProject: (projectId, { status, type } = {}) =>
    api.get('/tasks', { query: { projectId, status, type } }),
  backlog: (projectId) => api.get('/tasks/backlog', { query: { projectId } }),
  bySprint: (sprintId) => api.get(`/tasks/sprint/${sprintId}`),
  mine: () => api.get('/tasks/my'),
  update: (id, payload) => api.put(`/tasks/${id}`, payload),
  updateStatus: (id, status) => api.patch(`/tasks/${id}/status`, { status }),
  assign: (id, assigneeId) => api.patch(`/tasks/${id}/assign`, { assigneeId }),
  remove: (id) => api.del(`/tasks/${id}`),
}
