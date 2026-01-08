import { requestClient } from '#/api/request';

export namespace ProfileApi {
  /** 更新个人信息请求 */
  export interface UpdateProfileRequest {
    realName?: string;
    email?: string;
    phone?: string;
    introduction?: string;
  }

  /** 修改密码请求 */
  export interface ChangePasswordRequest {
    oldPassword: string;
    newPassword: string;
  }

  /** 用户详情返回 */
  export interface UserDetail {
    id: number;
    username: string;
    realName: string;
    email?: string;
    phone?: string;
    avatarUrl?: string;
    departmentId?: number;
    position?: string;
    employeeNo?: string;
    lawyerLicenseNo?: string;
    joinDate?: string;
    compensationType?: string;
    canBeOriginator?: boolean;
    status?: string;
    roleCodes?: string[];
    roleIds?: number[];
    permissions?: string[];
    introduction?: string;
    createdAt?: string;
    updatedAt?: string;
  }
}

/**
 * 获取当前用户详细信息
 */
export async function getProfileInfo() {
  return requestClient.get<ProfileApi.UserDetail>('/profile/info');
}

/**
 * 更新个人信息
 */
export async function updateProfile(data: ProfileApi.UpdateProfileRequest) {
  return requestClient.put<ProfileApi.UserDetail>('/profile/update', data);
}

/**
 * 修改密码
 */
export async function changePassword(data: ProfileApi.ChangePasswordRequest) {
  return requestClient.post('/profile/change-password', data);
}

