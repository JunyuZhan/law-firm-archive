import { ElMessage } from 'element-plus'

export const LOGO_ACCEPT_TYPES = 'image/png,image/jpeg,image/jpg,image/svg+xml,image/webp,image/gif'
export const LOGO_ALLOWED_EXTENSIONS = ['PNG', 'JPG', 'JPEG', 'SVG', 'WEBP', 'GIF']
export const LOGO_MAX_SIZE = 2 * 1024 * 1024
export const LOGO_RECOMMENDED_SIZE = '256×256 或 512×512'
export const LOGO_UPLOAD_HINT = `支持 ${LOGO_ALLOWED_EXTENSIONS.join(' / ')}，文件不超过 2MB；建议使用透明背景 PNG 或 SVG，推荐尺寸 ${LOGO_RECOMMENDED_SIZE}。`

export function validateLogoFile(file) {
  if (!file) {
    ElMessage.warning('请选择要上传的 Logo 文件')
    return false
  }

  const contentType = file.type || ''
  if (!contentType.startsWith('image/')) {
    ElMessage.warning('Logo 仅支持图片文件')
    return false
  }

  if (file.size > LOGO_MAX_SIZE) {
    ElMessage.warning('Logo 文件大小不能超过 2MB')
    return false
  }

  return true
}
