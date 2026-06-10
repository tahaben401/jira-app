// Enums miroir du backend (entités JPA). Source de vérité côté serveur.

export const USER_ROLES = ['DEVELOPER', 'QA', 'SCRUM_MASTER', 'PRODUCT_OWNER', 'ADMIN']
export const MEMBER_ROLES = ['DEVELOPER', 'QA', 'SCRUM_MASTER', 'PRODUCT_OWNER']

export const PROJECT_STATUS = ['ACTIVE', 'ARCHIVED', 'COMPLETED']

export const TASK_TYPES = ['TASK', 'BUG', 'STORY', 'EPIC']
export const TASK_STATUS = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']
export const TASK_PRIORITY = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export const SPRINT_STATUS = ['PLANNED', 'ACTIVE', 'COMPLETED']

export const NOTIF_TYPES = ['INFO', 'WARNING', 'ERROR']

// Libellés lisibles
export const LABELS = {
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  IN_REVIEW: 'En revue',
  DONE: 'Terminé',
  PLANNED: 'Planifié',
  ACTIVE: 'Actif',
  COMPLETED: 'Terminé',
  ARCHIVED: 'Archivé',
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique',
  TASK: 'Tâche',
  BUG: 'Bug',
  STORY: 'Story',
  EPIC: 'Epic',
  INFO: 'Info',
  WARNING: 'Alerte',
  ERROR: 'Erreur',
  DEVELOPER: 'Développeur',
  QA: 'QA',
  SCRUM_MASTER: 'Scrum Master',
  PRODUCT_OWNER: 'Product Owner',
  ADMIN: 'Admin',
}

export const label = (v) => LABELS[v] || v

// Mapping vers les classes de badge (.badge-*)
const BADGE = {
  // statut tâche
  TODO: 'slate', IN_PROGRESS: 'blue', IN_REVIEW: 'amber', DONE: 'green',
  // priorité
  LOW: 'slate', MEDIUM: 'blue', HIGH: 'amber', CRITICAL: 'red',
  // type
  TASK: 'blue', BUG: 'red', STORY: 'green', EPIC: 'purple',
  // statut projet / sprint
  ACTIVE: 'green', ARCHIVED: 'slate', COMPLETED: 'purple', PLANNED: 'amber',
  // notif
  INFO: 'blue', WARNING: 'amber', ERROR: 'red',
  // rôles
  DEVELOPER: 'blue', QA: 'amber', SCRUM_MASTER: 'purple', PRODUCT_OWNER: 'green', ADMIN: 'red',
}
export const badgeColor = (v) => BADGE[v] || 'slate'

// Icône par type de tâche (glyphes simples, pas de lib d'icônes)
export const TYPE_ICON = { TASK: '◼', BUG: '●', STORY: '◆', EPIC: '⬢' }
