import { api } from './client.js'

// auth-service — /api/v1/auth/**
export const authApi = {
  register: (payload) => api.post('/auth/register', payload, { auth: false }),
  login: (payload) => api.post('/auth/login', payload, { auth: false }),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }, { auth: false }),
  logout: (refreshToken) => api.post('/auth/logout', { refreshToken }),
  logoutAll: () => api.post('/auth/logout-all'),
  changePassword: (payload) => api.put('/auth/change-password', payload),
  me: () => api.get('/auth/me'),
  getUser: (id) => api.get(`/auth/users/${id}`),
}
