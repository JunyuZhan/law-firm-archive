export const ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  SECURITY_ADMIN: 'SECURITY_ADMIN',
  AUDIT_ADMIN: 'AUDIT_ADMIN',
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
  if (u === 'ADMIN') {
    return ROLES.SYSTEM_ADMIN
  }
  if (u === 'ARCHIVIST') {
    return ROLES.ARCHIVE_MANAGER
  }
  if (u === 'SECURITY_ADMIN') {
    return ROLES.SECURITY_ADMIN
  }
  if (u === 'AUDIT_ADMIN') {
    return ROLES.AUDIT_ADMIN
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

/** 与后端 UserRoleUtils.resolveGrantedRoles 一致：保留原角色，同时补齐兼容授权角色 */
export function resolveGrantedRoles(userType) {
  if (userType == null) {
    return []
  }
  const trimmed = String(userType).trim()
  if (!trimmed) {
    return [ROLES.USER]
  }
  const roles = new Set([trimmed])
  const normalized = normalizeUserType(trimmed)
  if (normalized && normalized !== trimmed) {
    roles.add(normalized)
  }
  return Array.from(roles)
}

/** 管理类角色：系统管理员、安全保密员、安全审计员均视为管理权限 */
export const MANAGER_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN, ROLES.AUDIT_ADMIN, ROLES.ARCHIVE_MANAGER]
export const REVIEW_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN, ROLES.AUDIT_ADMIN, ROLES.ARCHIVE_REVIEWER]
export const REPORT_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN, ROLES.AUDIT_ADMIN, ROLES.ARCHIVE_REVIEWER, ROLES.ARCHIVE_MANAGER]
export const BORROW_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN, ROLES.AUDIT_ADMIN, ROLES.ARCHIVE_MANAGER, ROLES.USER]

export function hasPermission(requiredRoles, userRole) {
  if (!requiredRoles || requiredRoles.length === 0) {
    return true
  }
  if (!userRole) {
    return false
  }
  return resolveGrantedRoles(userRole).some(role => requiredRoles.includes(role))
}
