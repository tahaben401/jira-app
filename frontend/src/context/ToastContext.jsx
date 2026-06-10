import { createContext, useContext, useState, useCallback } from 'react'

const ToastCtx = createContext(null)
let seq = 0

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])

  const remove = useCallback((id) => setItems((x) => x.filter((t) => t.id !== id)), [])

  const push = useCallback((message, kind = 'ok') => {
    const id = ++seq
    setItems((x) => [...x, { id, message, kind }])
    setTimeout(() => remove(id), 4000)
  }, [remove])

  const toast = {
    success: (m) => push(m, 'ok'),
    error: (m) => push(m, 'err'),
    info: (m) => push(m, 'info'),
  }

  return (
    <ToastCtx.Provider value={toast}>
      {children}
      <div className="toasts">
        {items.map((t) => (
          <div key={t.id} className={`toast ${t.kind}`} onClick={() => remove(t.id)}>
            <span className="t-ic">{t.kind === 'ok' ? '✓' : t.kind === 'err' ? '!' : 'i'}</span>
            <span className="t-msg">{t.message}</span>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  )
}

export const useToast = () => useContext(ToastCtx)
