// Client HTTP central.
// - Préfixe toutes les requêtes par /api/v1 (relayé vers le Gateway via le proxy Vite).
// - Ajoute l'en-tête Authorization: Bearer <accessToken>.
// - Déballe l'enveloppe ApiResponse { success, message, data } du backend.
// - Sur 401, tente UN rafraîchissement de token puis rejoue la requête.
// - En cas d'échec du refresh, purge la session (déconnexion).

import { tokens } from './tokens.js'

const BASE = '/api/v1'

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

// Un seul refresh à la fois, partagé par toutes les requêtes concurrentes.
let refreshPromise = null

async function doRefresh() {
  const refreshToken = tokens.refresh
  if (!refreshToken) throw new ApiError('Session expirée', 401)

  const res = await fetch(`${BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  const json = await res.json().catch(() => null)
  if (!res.ok || !json?.success) {
    tokens.clear()
    throw new ApiError('Session expirée, reconnectez-vous', 401)
  }
  tokens.setTokens(json.data.accessToken, json.data.refreshToken)
  return json.data.accessToken
}

function refreshOnce() {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

async function raw(method, path, { body, auth = true, query } = {}, _retried = false) {
  let url = BASE + path
  if (query) {
    const qs = new URLSearchParams()
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, v)
    })
    const s = qs.toString()
    if (s) url += `?${s}`
  }

  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth && tokens.access) headers['Authorization'] = `Bearer ${tokens.access}`

  const res = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // Tentative de refresh transparente (une seule fois).
  if (res.status === 401 && auth && !_retried && tokens.refresh) {
    try {
      await refreshOnce()
      return raw(method, path, { body, auth, query }, true)
    } catch {
      throw new ApiError('Session expirée, reconnectez-vous', 401)
    }
  }

  // 204 / corps vide
  const text = await res.text()
  const json = text ? safeParse(text) : null

  if (!res.ok) {
    const msg = json?.message || `Erreur ${res.status}`
    throw new ApiError(msg, res.status, json)
  }
  // Enveloppe ApiResponse : on renvoie directement data.
  if (json && typeof json === 'object' && 'success' in json) {
    if (!json.success) throw new ApiError(json.message || 'Requête échouée', res.status, json)
    return json.data
  }
  return json
}

function safeParse(t) {
  try { return JSON.parse(t) } catch { return null }
}

export const api = {
  get: (path, opts) => raw('GET', path, opts),
  post: (path, body, opts) => raw('POST', path, { ...opts, body }),
  put: (path, body, opts) => raw('PUT', path, { ...opts, body }),
  patch: (path, body, opts) => raw('PATCH', path, { ...opts, body }),
  del: (path, opts) => raw('DELETE', path, opts),
}
