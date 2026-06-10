import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { tokens, subscribe } from '../api/tokens.js'
import { authApi } from '../api/auth.js'

const AuthCtx = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokens.user)

  // Reste synchronisé avec le store (notamment lors d'un refresh échoué -> clear).
  useEffect(() => subscribe(() => setUser(tokens.user)), [])

  const login = useCallback(async (email, password) => {
    const auth = await authApi.login({ email, password })
    tokens.setSession(auth)
    return auth
  }, [])

  const register = useCallback(async (payload) => {
    const auth = await authApi.register(payload)
    tokens.setSession(auth)
    return auth
  }, [])

  // Révoque le refresh token côté serveur quand c'est possible, puis purge la session.
  const logout = useCallback(async () => {
    const rt = tokens.refresh
    if (rt) { try { await authApi.logout(rt) } catch { /* on purge quand même */ } }
    tokens.clear()
  }, [])

  return (
    <AuthCtx.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthCtx.Provider>
  )
}

export const useAuth = () => useContext(AuthCtx)
