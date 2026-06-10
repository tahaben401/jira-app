import { api } from './client.js'

// notification-service — /api/v1/notifications/**
export const notificationsApi = {
  create: (payload) => api.post('/notifications', payload),
  get: (id) => api.get(`/notifications/${id}`),
  list: () => api.get('/notifications'),
  unread: () => api.get('/notifications/unread'),
  markRead: (id, read = true) => api.patch(`/notifications/${id}/read`, { read }),
  markAllRead: () => api.patch('/notifications/read-all'),
  remove: (id) => api.del(`/notifications/${id}`),
}
