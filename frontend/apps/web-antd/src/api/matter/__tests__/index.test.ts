import type {
  CreateMatterCommand,
  CreateTaskCommand,
  CreateTimesheetCommand,
  MatterQuery,
  UpdateMatterCommand,
} from '../types';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  addParticipant,
  applyCloseMatter,
  approveCloseMatter,
  approveContract,
  approveTask,
  approveTimesheet,
  changeMatterStatus,
  changeTaskStatus,
  createContract,
  createMatter,
  createTask,
  createTimesheet,
  deleteMatter,
  deleteTask,
  deleteTimesheet,
  getContractDetail,
  getMatterCloseApprovers,
  getMatterDetail,
  getMatterList,
  getMatterSelectOptions,
  getMatterTaskStats,
  getMatterTimeline,
  getMyContracts,
  getMyMatters,
  getMyTodoTasks,
  getTaskDetail,
  getTaskList,
  getTimesheetDetail,
  getTimesheetList,
  getTimesheetStats,
  rejectContract,
  rejectTask,
  rejectTimesheet,
  removeParticipant,
  reviewTimesheet,
  submitContract,
  submitTimesheet,
  updateMatter,
  updateTask,
  updateTaskProgress,
  updateTimesheet,
} from '../index';

// Mock requestClient
vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('matter API', () => {
  let mockGet: ReturnType<typeof vi.fn>;
  let mockPost: ReturnType<typeof vi.fn>;
  let mockPut: ReturnType<typeof vi.fn>;
  let mockDelete: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    const { requestClient } = await import('#/api/request');
    mockGet = vi.mocked(requestClient.get);
    mockPost = vi.mocked(requestClient.post);
    mockPut = vi.mocked(requestClient.put);
    mockDelete = vi.mocked(requestClient.delete);
    vi.clearAllMocks();
  });

  describe('matter Management', () => {
    it('should get matter list', async () => {
      const params: MatterQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/list', { params });
    });

    it('should get matter select options', async () => {
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterSelectOptions();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/select-options', {
        params: undefined,
      });
    });

    it('should get my matters', async () => {
      const params: MatterQuery = { pageNum: 1 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyMatters(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/my', { params });
    });

    it('should get matter detail', async () => {
      const matterId = 1;
      const mockResponse = { id: matterId, name: 'Test Matter' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterDetail(matterId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/matter/${matterId}`);
    });

    it('should create matter', async () => {
      const data: CreateMatterCommand = {
        name: 'New Matter',
        clientId: 1,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createMatter(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/matter', data);
    });

    it('should update matter', async () => {
      const data: UpdateMatterCommand = {
        id: 1,
        name: 'Updated Matter',
      } as any;
      const mockResponse = { ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateMatter(data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith('/matter', data);
    });

    it('should delete matter', async () => {
      const matterId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteMatter(matterId);

      expect(mockDelete).toHaveBeenCalledWith(`/matter/${matterId}`);
    });

    it('should change matter status', async () => {
      const matterId = 1;
      const status = 'ACTIVE';

      mockPut.mockResolvedValueOnce(undefined);

      await changeMatterStatus(matterId, status);

      expect(mockPut).toHaveBeenCalledWith(`/matter/${matterId}/status`, {
        status,
      });
    });

    it('should add participant', async () => {
      const matterId = 1;
      const data = {
        userId: 2,
        role: 'ASSISTANT',
        commissionRate: 0.3,
      };

      mockPost.mockResolvedValueOnce(undefined);

      await addParticipant(matterId, data);

      expect(mockPost).toHaveBeenCalledWith(
        `/matter/${matterId}/participant`,
        data,
      );
    });

    it('should remove participant', async () => {
      const matterId = 1;
      const userId = 2;

      mockDelete.mockResolvedValueOnce(undefined);

      await removeParticipant(matterId, userId);

      expect(mockDelete).toHaveBeenCalledWith(
        `/matter/${matterId}/participant/${userId}`,
      );
    });

    it('should get matter close approvers', async () => {
      const mockResponse = [
        {
          id: 1,
          realName: 'Approver',
          departmentName: 'Legal',
          position: 'Partner',
          recommended: true,
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterCloseApprovers();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/close/approvers');
    });

    it('should apply close matter', async () => {
      const matterId = 1;
      const data = {
        closingDate: '2024-01-01',
        closingReason: 'WIN',
        summary: 'Case won',
      };

      mockPost.mockResolvedValueOnce(undefined);

      await applyCloseMatter(matterId, data);

      expect(mockPost).toHaveBeenCalledWith(
        `/matter/${matterId}/close/apply`,
        data,
      );
    });

    it('should approve close matter', async () => {
      const matterId = 1;
      const data = { approved: true, comment: 'Approved' };

      mockPost.mockResolvedValueOnce(undefined);

      await approveCloseMatter(matterId, data);

      expect(mockPost).toHaveBeenCalledWith(
        `/matter/${matterId}/close/approve`,
        data,
      );
    });

    it('should get matter timeline', async () => {
      const matterId = 1;
      const mockResponse = [
        {
          id: 1,
          type: 'CREATED',
          description: 'Matter created',
          createdAt: '2024-01-01',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterTimeline(matterId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/matter/${matterId}/timeline`);
    });
  });

  describe('contract Management', () => {
    it('should create contract', async () => {
      const data = { name: 'Test Contract', clientId: 1 };
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createContract(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/matter/contract', data);
    });

    it('should get contract detail', async () => {
      const contractId = 1;
      const mockResponse = { id: contractId, name: 'Test Contract' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractDetail(contractId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/matter/contract/${contractId}`);
    });

    it('should get my contracts', async () => {
      const params = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyContracts(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/contract/my', { params });
    });

    it('should submit contract', async () => {
      const contractId = 1;
      const approverId = 2;

      mockPost.mockResolvedValueOnce(undefined);

      await submitContract(contractId, approverId);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/contract/${contractId}/submit`,
        null,
        { params: { approverId } },
      );
    });

    it('should approve contract', async () => {
      const contractId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await approveContract(contractId);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/contract/${contractId}/approve`,
      );
    });

    it('should reject contract', async () => {
      const contractId = 1;
      const reason = 'Invalid terms';

      mockPost.mockResolvedValueOnce(undefined);

      await rejectContract(contractId, reason);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/contract/${contractId}/reject`,
        { reason },
      );
    });
  });

  describe('timesheet Management', () => {
    it('should get timesheet list', async () => {
      const params = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getTimesheetList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/timesheets', { params });
    });

    it('should get timesheet detail', async () => {
      const timesheetId = 1;
      const mockResponse = { id: timesheetId, hours: 8 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getTimesheetDetail(timesheetId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/timesheets/${timesheetId}`);
    });

    it('should create timesheet', async () => {
      const data: CreateTimesheetCommand = {
        matterId: 1,
        hours: 8,
        date: '2024-01-01',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createTimesheet(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/timesheets', data);
    });

    it('should update timesheet', async () => {
      const timesheetId = 1;
      const data = { hours: 6 };

      mockPut.mockResolvedValueOnce({ id: timesheetId, ...data });

      await updateTimesheet(timesheetId, data);

      expect(mockPut).toHaveBeenCalledWith(`/timesheets/${timesheetId}`, data);
    });

    it('should delete timesheet', async () => {
      const timesheetId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteTimesheet(timesheetId);

      expect(mockDelete).toHaveBeenCalledWith(`/timesheets/${timesheetId}`);
    });

    it('should submit timesheet', async () => {
      const timesheetId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await submitTimesheet(timesheetId);

      expect(mockPost).toHaveBeenCalledWith(
        `/timesheets/${timesheetId}/submit`,
      );
    });

    it('should approve timesheet', async () => {
      const timesheetId = 1;
      const comment = 'Approved';
      const mockResponse = { id: timesheetId, status: 'APPROVED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await approveTimesheet(timesheetId, comment);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/timesheets/${timesheetId}/approve`,
        null,
        { params: { comment } },
      );
    });

    it('should reject timesheet', async () => {
      const timesheetId = 1;
      const comment = 'Rejected';
      const mockResponse = { id: timesheetId, status: 'REJECTED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await rejectTimesheet(timesheetId, comment);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/timesheets/${timesheetId}/reject`,
        null,
        { params: { comment } },
      );
    });

    it('should review timesheet (approve)', async () => {
      const timesheetId = 1;
      const data = { approved: true, comment: 'OK' };
      const mockResponse = { id: timesheetId, status: 'APPROVED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await reviewTimesheet(timesheetId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/timesheets/${timesheetId}/approve`,
        null,
        { params: { comment: 'OK' } },
      );
    });

    it('should get timesheet stats', async () => {
      const params = { startDate: '2024-01-01', endDate: '2024-01-31' };
      const mockResponse = { totalHours: 160 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getTimesheetStats(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/timesheets/stats', { params });
    });
  });

  describe('task Management', () => {
    it('should get task list', async () => {
      const params = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getTaskList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/tasks', { params });
    });

    it('should get my todo tasks', async () => {
      const mockResponse = [{ id: 1, title: 'Task 1' }];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyTodoTasks();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/tasks/my/todo');
    });

    it('should get task detail', async () => {
      const taskId = 1;
      const mockResponse = { id: taskId, title: 'Test Task' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getTaskDetail(taskId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/tasks/${taskId}`);
    });

    it('should create task', async () => {
      const data: CreateTaskCommand = {
        matterId: 1,
        title: 'New Task',
        dueDate: '2024-01-01',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createTask(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/tasks', data);
    });

    it('should update task', async () => {
      const taskId = 1;
      const data = { title: 'Updated Task' };

      mockPut.mockResolvedValueOnce({ id: taskId, ...data });

      await updateTask(taskId, data);

      expect(mockPut).toHaveBeenCalledWith(`/tasks/${taskId}`, data);
    });

    it('should delete task', async () => {
      const taskId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteTask(taskId);

      expect(mockDelete).toHaveBeenCalledWith(`/tasks/${taskId}`);
    });

    it('should change task status', async () => {
      const taskId = 1;
      const status = 'IN_PROGRESS';
      const mockResponse = { id: taskId, status };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await changeTaskStatus(taskId, status);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/tasks/${taskId}/status`, null, {
        params: { status },
      });
    });

    it('should update task progress', async () => {
      const taskId = 1;
      const progress = 50;
      const mockResponse = { id: taskId, progress };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateTaskProgress(taskId, progress);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/tasks/${taskId}/progress`, null, {
        params: { progress },
      });
    });

    it('should approve task', async () => {
      const taskId = 1;
      const mockResponse = { id: taskId, status: 'APPROVED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await approveTask(taskId);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(`/tasks/${taskId}/review/approve`);
    });

    it('should reject task', async () => {
      const taskId = 1;
      const comment = 'Needs revision';
      const mockResponse = { id: taskId, status: 'REJECTED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await rejectTask(taskId, comment);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/tasks/${taskId}/review/reject`,
        null,
        { params: { comment } },
      );
    });

    it('should get matter task stats', async () => {
      const matterId = 1;
      const mockResponse = [10, 5, 2]; // [total, completed, pending]

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMatterTaskStats(matterId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/tasks/stats/matter/${matterId}`);
    });
  });
});
