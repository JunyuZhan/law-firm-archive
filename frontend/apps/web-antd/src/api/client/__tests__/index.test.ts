import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  ApplyConflictCheckCommand,
  ClientQuery,
  CreateClientCommand,
  CreateLeadCommand,
  LeadQuery,
  UpdateClientCommand,
} from '../types';
import {
  approveConflictCheck,
  approveExemption,
  applyConflictCheck,
  applyExemption,
  batchDeleteClients,
  changeClientStatus,
  convertLeadToClient,
  convertToFormal,
  createClient,
  createLead,
  deleteClient,
  deleteLead,
  exportClients,
  followUpLead,
  getClientDetail,
  getClientList,
  getClientSelectOptions,
  getConflictCheckDetail,
  getConflictCheckList,
  getLeadDetail,
  getLeadList,
  importClients,
  quickConflictCheck,
  rejectConflictCheck,
  rejectExemption,
  reviewConflictCheck,
  updateClient,
  updateLead,
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

describe('Client API', () => {
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

  describe('Client Management', () => {
    it('should get client list', async () => {
      const params: ClientQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getClientList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/client/list', { params });
    });

    it('should get client select options', async () => {
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getClientSelectOptions();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/client/select-options', {
        params: undefined,
      });
    });

    it('should get client detail', async () => {
      const clientId = 1;
      const mockResponse = { id: clientId, name: 'Test Client' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getClientDetail(clientId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/client/${clientId}`);
    });

    it('should create client', async () => {
      const data: CreateClientCommand = {
        name: 'New Client',
        clientType: 'INDIVIDUAL',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createClient(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/client', data);
    });

    it('should update client', async () => {
      const data: UpdateClientCommand = {
        id: 1,
        name: 'Updated Client',
      } as any;
      const mockResponse = { ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateClient(data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith('/client', data);
    });

    it('should delete client', async () => {
      const clientId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteClient(clientId);

      expect(mockDelete).toHaveBeenCalledWith(`/client/${clientId}`);
    });

    it('should change client status', async () => {
      const clientId = 1;
      const status = 'ACTIVE';

      mockPut.mockResolvedValueOnce(undefined);

      await changeClientStatus(clientId, status);

      expect(mockPut).toHaveBeenCalledWith(`/client/${clientId}/status`, {
        status,
      });
    });

    it('should convert to formal client', async () => {
      const clientId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await convertToFormal(clientId);

      expect(mockPost).toHaveBeenCalledWith(`/client/${clientId}/convert`);
    });

    it('should export clients', async () => {
      const params: ClientQuery = { pageNum: 1 };
      const mockBlob = new Blob(['export data'], { type: 'application/vnd.ms-excel' });

      mockGet.mockResolvedValueOnce(mockBlob);

      const result = await exportClients(params);

      expect(result).toEqual(mockBlob);
      expect(mockGet).toHaveBeenCalledWith('/client/export', {
        params,
        responseType: 'blob',
      });
    });

    it('should import clients', async () => {
      const file = new File(['import data'], 'clients.xlsx', {
        type: 'application/vnd.ms-excel',
      });
      const mockResponse = {
        success: 10,
        failure: 2,
        errors: ['Error 1', 'Error 2'],
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await importClients(file);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/client/import',
        expect.any(FormData),
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        },
      );
    });

    it('should batch delete clients', async () => {
      const ids = [1, 2, 3];

      mockDelete.mockResolvedValueOnce(undefined);

      await batchDeleteClients(ids);

      expect(mockDelete).toHaveBeenCalledWith('/client/batch', { data: { ids } });
    });
  });

  describe('Conflict Check', () => {
    it('should get conflict check list', async () => {
      const params = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getConflictCheckList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/client/conflict-check/list', {
        params,
      });
    });

    it('should get conflict check detail', async () => {
      const conflictCheckId = 1;
      const mockResponse = {
        id: conflictCheckId,
        status: 'PENDING',
        clientName: 'Test Client',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getConflictCheckDetail(conflictCheckId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/client/conflict-check/${conflictCheckId}`,
      );
    });

    it('should apply conflict check', async () => {
      const data: ApplyConflictCheckCommand = {
        clientName: 'Test Client',
        opposingParty: 'Opposing Party',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await applyConflictCheck(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/client/conflict-check/apply',
        data,
      );
    });

    it('should approve conflict check', async () => {
      const conflictCheckId = 1;
      const comment = 'No conflict';

      mockPost.mockResolvedValueOnce(undefined);

      await approveConflictCheck(conflictCheckId, comment);

      expect(mockPost).toHaveBeenCalledWith(
        `/client/conflict-check/${conflictCheckId}/approve`,
        { comment },
      );
    });

    it('should reject conflict check', async () => {
      const conflictCheckId = 1;
      const comment = 'Conflict found';

      mockPost.mockResolvedValueOnce(undefined);

      await rejectConflictCheck(conflictCheckId, comment);

      expect(mockPost).toHaveBeenCalledWith(
        `/client/conflict-check/${conflictCheckId}/reject`,
        { comment },
      );
    });

    it('should review conflict check (approve)', async () => {
      const conflictCheckId = 1;
      const data = { approved: true, comment: 'OK' };

      mockPost.mockResolvedValueOnce(undefined);

      await reviewConflictCheck(conflictCheckId, data);

      expect(mockPost).toHaveBeenCalledWith(
        `/client/conflict-check/${conflictCheckId}/approve`,
        { comment: 'OK' },
      );
    });

    it('should quick conflict check', async () => {
      const data = {
        clientName: 'Test Client',
        opposingParty: 'Opposing Party',
      };
      const mockResponse = {
        hasConflict: false,
        candidates: [],
        riskLevel: 'NONE' as const,
        riskSummary: 'No conflicts found',
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await quickConflictCheck(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/client/conflict-check/quick',
        data,
      );
    });

    it('should apply exemption', async () => {
      const data = {
        conflictCheckId: 1,
        exemptionReason: 'Special circumstances',
        exemptionDescription: 'Details',
      };
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await applyExemption(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/client/conflict-check/exemption/apply',
        data,
      );
    });

    it('should approve exemption', async () => {
      const exemptionId = 1;
      const comment = 'Approved';

      mockPost.mockResolvedValueOnce(undefined);

      await approveExemption(exemptionId, comment);

      expect(mockPost).toHaveBeenCalledWith(
        `/client/conflict-check/exemption/${exemptionId}/approve`,
        { comment },
      );
    });

    it('should reject exemption', async () => {
      const exemptionId = 1;
      const comment = 'Rejected';

      mockPost.mockResolvedValueOnce(undefined);

      await rejectExemption(exemptionId, comment);

      expect(mockPost).toHaveBeenCalledWith(
        `/client/conflict-check/exemption/${exemptionId}/reject`,
        { comment },
      );
    });
  });

  describe('Lead Management', () => {
    it('should get lead list', async () => {
      const params: LeadQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getLeadList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/client/lead', { params });
    });

    it('should get lead detail', async () => {
      const leadId = 1;
      const mockResponse = { id: leadId, name: 'Test Lead' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getLeadDetail(leadId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/client/lead/${leadId}`);
    });

    it('should create lead', async () => {
      const data: CreateLeadCommand = {
        name: 'New Lead',
        source: 'WEBSITE',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createLead(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/client/lead', data);
    });

    it('should update lead', async () => {
      const leadId = 1;
      const data = { name: 'Updated Lead' };
      const mockResponse = { id: leadId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateLead(leadId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/client/lead/${leadId}`, data);
    });

    it('should delete lead', async () => {
      const leadId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteLead(leadId);

      expect(mockDelete).toHaveBeenCalledWith(`/client/lead/${leadId}`);
    });

    it('should convert lead to client', async () => {
      const leadId = 1;
      const data = { clientId: 2, matterId: 3 };
      const mockResponse = { id: leadId, status: 'CONVERTED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await convertLeadToClient(leadId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(`/client/lead/${leadId}/convert`, data);
    });

    it('should convert lead to client without data', async () => {
      const leadId = 1;
      const mockResponse = { id: leadId, status: 'CONVERTED' };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await convertLeadToClient(leadId);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(`/client/lead/${leadId}/convert`, {});
    });

    it('should follow up lead', async () => {
      const leadId = 1;
      const data = {
        content: 'Follow up note',
        nextFollowUpTime: '2024-01-15',
      };

      mockPost.mockResolvedValueOnce(undefined);

      await followUpLead(leadId, data);

      expect(mockPost).toHaveBeenCalledWith(`/client/lead/${leadId}/follow-up`, data);
    });
  });
});
