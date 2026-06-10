import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Le navigateur ne peut pas appeler directement http://localhost:8080 (le Gateway
// n'expose aucune en-tête CORS). On passe donc par le proxy de dev de Vite :
// le front appelle "/api/..." sur sa propre origine (5173), et Vite relaie
// la requête vers le Gateway. Aucune modification du backend n'est nécessaire.
//
// Pour cibler un autre hôte/port (ex. backend dans Docker), définir VITE_API_TARGET.
const API_TARGET = process.env.VITE_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
})
