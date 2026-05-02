import request from '@/utils/request'

export function getBackupOverview() {
  return request({
    url: '/backups/overview',
    method: 'get'
  })
}

export function getBackupTargets() {
  return request({
    url: '/backups/targets',
    method: 'get'
  })
}

export function getBackupTarget(id) {
  return request({
    url: `/backups/targets/${id}`,
    method: 'get'
  })
}

export function createBackupTarget(data) {
  return request({
    url: '/backups/targets',
    method: 'post',
    data
  })
}

export function updateBackupTarget(id, data) {
  return request({
    url: `/backups/targets/${id}`,
    method: 'put',
    data
  })
}

export function deleteBackupTarget(id) {
  return request({
    url: `/backups/targets/${id}`,
    method: 'delete'
  })
}

export function verifyBackupTarget(id) {
  return request({
    url: `/backups/targets/${id}/verify`,
    method: 'post'
  })
}

export function getBackupJobs(params) {
  return request({
    url: '/backups/jobs',
    method: 'get',
    params
  })
}

export function getBackupSets(params) {
  return request({
    url: '/backups/sets',
    method: 'get',
    params
  })
}

export function getRestoreBackupSets(params) {
  return request({
    url: '/restores/sets',
    method: 'get',
    params
  })
}

export function getRestoreMaintenanceStatus() {
  return request({
    url: '/restores/maintenance',
    method: 'get'
  })
}

export function updateRestoreMaintenanceStatus(enabled) {
  return request({
    url: '/restores/maintenance',
    method: 'put',
    params: { enabled }
  })
}

export function runBackup(targetId) {
  return request({
    url: '/backups/run',
    method: 'post',
    params: { targetId }
  })
}

export function getRestoreJobs(params) {
  return request({
    url: '/restores/jobs',
    method: 'get',
    params
  })
}

export function runRestore(data) {
  return request({
    url: '/restores/run',
    method: 'post',
    data
  })
}
