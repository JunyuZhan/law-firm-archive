export const ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  ARCHIVE_REVIEWER: 'ARCHIVE_REVIEWER',
  ARCHIVE_MANAGER: 'ARCHIVE_MANAGER',
  USER: 'USER'
}

/** 与后端 UserRoleUtils.normalize 一致：trim + 大小写不敏感，用于路由/UI 与授权对齐 */
export function normalizeUserType(userType) {
  if (userType == null) {
    return userType
  }
  const t = String(userType).trim()
  if (!t) {
    return ROLES.USER
  }
  const u = t.toUpperCase()
  if (u === 'ARCHIVIST') {
    return ROLES.ARCHIVE_MANAGER
  }
  if (u === 'SECURITY_ADMIN' || u === 'AUDIT_ADMIN') {
    return ROLES.SYSTEM_ADMIN
  }
  if (u === 'SYSTEM_ADMIN') {
    return ROLES.SYSTEM_ADMIN
  }
  if (u === 'ARCHIVE_MANAGER') {
    return ROLES.ARCHIVE_MANAGER
  }
  if (u === 'ARCHIVE_REVIEWER') {
    return ROLES.ARCHIVE_REVIEWER
  }
  if (u === 'USER') {
    return ROLES.USER
  }
  return t
}

export const MANAGER_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER]
export const REVIEW_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_REVIEWER]
export const REPORT_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_REVIEWER, ROLES.ARCHIVE_MANAGER]
export const BORROW_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER, ROLES.USER]

export function hasPermission(requiredRoles, userRole) {
  if (!requiredRoles || requiredRoles.length === 0) {
    return true
  }
  if (!userRole) {
    return false
  }
  const effective = normalizeUserType(userRole)
  return requiredRoles.includes(effective)
}
