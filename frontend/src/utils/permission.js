export const ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  ARCHIVE_REVIEWER: 'ARCHIVE_REVIEWER',
  ARCHIVE_MANAGER: 'ARCHIVE_MANAGER',
  USER: 'USER'
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
  return requiredRoles.includes(userRole)
}
