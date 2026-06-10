// Source unique de vérité pour les tokens + user, persistés dans localStorage.
// Un petit système d'abonnement permet à l'AuthContext de réagir aux changements
// (ex. refresh échoué -> déconnexion automatique).

const ACCESS = 'atlas.accessToken'
const REFRESH = 'atlas.refreshToken'
const USER = 'atlas.user'

const listeners = new Set()
function emit() { listeners.forEach((fn) => fn()) }

export function subscribe(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

export const tokens = {
  get access() { return localStorage.getItem(ACCESS) },
  get refresh() { return localStorage.getItem(REFRESH) },
  get user() {
    const raw = localStorage.getItem(USER)
    return raw ? JSON.parse(raw) : null
  },

  // Stocke la réponse d'auth (login / register / refresh).
  setSession(auth) {
    localStorage.setItem(ACCESS, auth.accessToken)
    localStorage.setItem(REFRESH, auth.refreshToken)
    localStorage.setItem(USER, JSON.stringify({
      id: auth.userId,
      email: auth.email,
      fullName: auth.fullName,
      role: auth.role,
    }))
    emit()
  },

  // Met à jour uniquement les tokens (utilisé après un refresh silencieux).
  setTokens(accessToken, refreshToken) {
    localStorage.setItem(ACCESS, accessToken)
    localStorage.setItem(REFRESH, refreshToken)
    emit()
  },

  clear() {
    localStorage.removeItem(ACCESS)
    localStorage.removeItem(REFRESH)
    localStorage.removeItem(USER)
    emit()
  },
}
