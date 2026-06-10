# Atlas — Frontend de test

Interface fonctionnelle (type Jira) pour tester **toutes les routes** du backend
microservices via l'API Gateway. Construit avec **React + Vite**, sans librairie
de composants : le CSS est écrit à la main.

## Démarrer

> Prérequis : le backend doit tourner (au minimum **Eureka**, **Gateway**,
> **auth-service**, et les services que vous testez), accessible sur
> `http://localhost:8080`.

```bash
cd frontend
npm install
npm run dev
```

Ouvrez ensuite **http://localhost:5173**.

### Pourquoi un proxy ?

Le Gateway n'expose aucune en-tête CORS. Le navigateur ne peut donc pas appeler
`http://localhost:8080` directement. Vite contourne ça : le front appelle
`/api/...` sur sa propre origine (`5173`) et **relaie** vers le Gateway.
Aucune modification du backend n'est nécessaire.

Pour cibler un autre hôte (ex. backend dockerisé) :

```bash
# PowerShell
$env:VITE_API_TARGET="http://localhost:8080"; npm run dev
```

## Premiers pas

1. **Créer un compte** sur l'écran d'inscription (choisissez un rôle).
2. Créez un **projet**, puis ouvrez-le.
3. Onglets du projet : **Tableau** (Kanban), **Backlog**, **Sprints**,
   **Membres**, **Paramètres**.
4. Pour inviter un membre : créez un 2ᵉ compte, récupérez son **ID** via
   *Profil → Rechercher un utilisateur*, puis invitez-le.

## Couverture des routes (40)

| Service | Routes couvertes | Où dans l'UI |
|---|---|---|
| **auth** | register, login, refresh, logout, logout-all, change-password, me, users/{id} | Login, Register, Profil, menu utilisateur |
| **projects** | create, list, get, update, delete, invite, remove-member, update-role | Projets, détail projet (Membres / Paramètres) |
| **tasks** | create, get, list(+filtres), backlog, by-sprint, my, update, status, assign, delete | Tableau, Backlog, Mes tâches |
| **sprints** | create, get, list, active, update, status, delete | Onglet Sprints |
| **notifications** | create, get, list, unread, read, read-all, delete | Notifications |

> Non exposées (volontairement) : `/auth/validate` et `/auth/users/batch` sont
> des routes internes (Gateway / token de service), pas destinées au client.

## Structure

```
src/
├── api/        # client HTTP (refresh auto) + 1 module par service
├── components/ # UI partagée + composants par domaine (project/)
├── context/    # AuthContext, ToastContext
├── lib/        # enums miroir du backend, helpers de format
├── pages/      # écrans (routes)
└── styles/     # design system (global.css)
```
