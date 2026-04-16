export const ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  ARCHIVE_REVIEWER: 'ARCHIVE_REVIEWER',
  ARCHIVE_MANAGER: 'ARCHIVE_MANAGER',
  USER: 'USER'
}

/** 与后端 UserRoleUtils.normalize 一致，用于路由/UI 与 Spring 授权对齐 */
export function normalizeUserType(userType) {
  if (userType == null || userType === '') {
    return userType
  }
  if (userType === 'ARCHIVIST') {
    return ROLES.ARCHIVE_MANAGER
  }
  if (userType === 'SECURITY_ADMIN' || userType === 'AUDIT_ADMIN') {
    return ROLES.SYSTEM_ADMIN
  }
  return userType
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
