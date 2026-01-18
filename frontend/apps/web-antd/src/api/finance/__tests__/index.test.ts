import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  CommissionQuery,
  ContractQuery,
  CreateContractCommand,
  CreateExpenseCommand,
  CreateFeeCommand,
  CreateInvoiceCommand,
  CreatePaymentCommand,
  CreatePaymentScheduleCommand,
  ExpenseQuery,
  FeeQuery,
  InvoiceQuery,
  UpdateContractCommand,
  UpdatePaymentScheduleCommand,
} from '../types';
import {
  approveCommission,
  approveContract,
  approveExpense,
  applyInvoice,
  batchApproveCommission,
  batchIssueCommission,
  calculateCommission,
  cancelInvoice,
  cancelPayment,
  completeContract,
  confirmPayment,
  createContract,
  createContractFromTemplate,
  createContractParticipant,
  createExpense,
  createFee,
  createPayment,
  createPaymentSchedule,
  deleteContract,
  deleteContractParticipant,
  deleteExpense,
  deleteFee,
  deletePaymentSchedule,
  getCommissionDetail,
  getCommissionList,
  getCommissionReport,
  getCommissionSummary,
  getContractDetail,
  getContractList,
  getContractParticipants,
  getContractPaymentSchedules,
  getContractStatistics,
  getExpenseDetail,
  getExpenseList,
  getFeeDetail,
  getFeeList,
  getInvoiceDetail,
  getInvoiceList,
  getInvoiceStatistics,
  getMyContracts,
  getPendingCommissionPayments,
  getUserCommissionTotal,
  issueCommission,
  issueInvoice,
  manualCalculateCommission,
  payExpense,
  previewTemplateContent,
  rejectContract,
  submitContract,
  terminateContract,
  updateContract,
  updateContractParticipant,
  updateFee,
  updatePaymentSchedule,
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

describe('Finance API', () => {
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

  describe('Contract Management', () => {
    it('should get contract list', async () => {
      const params: ContractQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/contract/list', { params });
    });

    it('should get my contracts', async () => {
      const params: ContractQuery = { pageNum: 1 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyContracts(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/contract/my', { params });
    });

    it('should get contract detail', async () => {
      const contractId = 1;
      const mockResponse = { id: contractId, name: 'Test Contract' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractDetail(contractId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/finance/contract/${contractId}`);
    });

    it('should create contract', async () => {
      const data: CreateContractCommand = {
        name: 'New Contract',
        clientId: 1,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createContract(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/finance/contract', data);
    });

    it('should update contract', async () => {
      const data: UpdateContractCommand = {
        id: 1,
        name: 'Updated Contract',
      } as any;
      const mockResponse = { ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateContract(data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith('/finance/contract', data);
    });

    it('should delete contract', async () => {
      const contractId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteContract(contractId);

      expect(mockDelete).toHaveBeenCalledWith(`/finance/contract/${contractId}`);
    });

    it('should submit contract', async () => {
      const contractId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await submitContract(contractId);

      expect(mockPost).toHaveBeenCalledWith(`/finance/contract/${contractId}/submit`);
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

    it('should terminate contract', async () => {
      const contractId = 1;
      const reason = 'Termination reason';

      mockPost.mockResolvedValueOnce(undefined);

      await terminateContract(contractId, reason);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/contract/${contractId}/terminate`,
        { reason },
      );
    });

    it('should complete contract', async () => {
      const contractId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await completeContract(contractId);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/contract/${contractId}/complete`,
      );
    });

    it('should get contract payment schedules', async () => {
      const contractId = 1;
      const mockResponse = [
        {
          id: 1,
          contractId,
          amount: 10000,
          dueDate: '2024-01-01',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractPaymentSchedules(contractId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/payment-schedules`,
      );
    });

    it('should create payment schedule', async () => {
      const contractId = 1;
      const data: CreatePaymentScheduleCommand = {
        amount: 10000,
        dueDate: '2024-01-01',
      } as any;
      const mockResponse = { id: 1, contractId, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createPaymentSchedule(contractId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/payment-schedules`,
        data,
      );
    });

    it('should update payment schedule', async () => {
      const contractId = 1;
      const scheduleId = 1;
      const data: UpdatePaymentScheduleCommand = {
        amount: 15000,
      } as any;
      const mockResponse = { id: scheduleId, contractId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updatePaymentSchedule(contractId, scheduleId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/payment-schedules/${scheduleId}`,
        data,
      );
    });

    it('should delete payment schedule', async () => {
      const contractId = 1;
      const scheduleId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deletePaymentSchedule(contractId, scheduleId);

      expect(mockDelete).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/payment-schedules/${scheduleId}`,
      );
    });

    it('should get contract participants', async () => {
      const contractId = 1;
      const mockResponse = [
        {
          id: 1,
          contractId,
          userId: 2,
          role: 'LEAD_LAWYER',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractParticipants(contractId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/participants`,
      );
    });

    it('should create contract participant', async () => {
      const contractId = 1;
      const data = {
        userId: 2,
        role: 'ASSISTANT',
        commissionRate: 0.3,
      };
      const mockResponse = { id: 1, contractId, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createContractParticipant(contractId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/participants`,
        data,
      );
    });

    it('should update contract participant', async () => {
      const contractId = 1;
      const participantId = 1;
      const data = { commissionRate: 0.4 };
      const mockResponse = { id: participantId, contractId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateContractParticipant(contractId, participantId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/participants/${participantId}`,
        data,
      );
    });

    it('should delete contract participant', async () => {
      const contractId = 1;
      const participantId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteContractParticipant(contractId, participantId);

      expect(mockDelete).toHaveBeenCalledWith(
        `/matter/contract/${contractId}/participants/${participantId}`,
      );
    });

    it('should get contract statistics', async () => {
      const params = { startDate: '2024-01-01', endDate: '2024-12-31' };
      const mockResponse = {
        totalContracts: 100,
        totalAmount: 1000000,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getContractStatistics(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/matter/contract/statistics', {
        params,
      });
    });

    it('should create contract from template', async () => {
      const templateId = 1;
      const data: CreateContractCommand = {
        name: 'Contract from Template',
        clientId: 1,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createContractFromTemplate(templateId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/matter/contract/from-template/${templateId}`,
        data,
      );
    });

    it('should preview template content', async () => {
      const templateId = 1;
      const data: CreateContractCommand = {
        name: 'Test Contract',
        clientId: 1,
      } as any;
      const mockResponse = '<html>Preview content</html>';

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await previewTemplateContent(templateId, data);

      expect(result).toBe(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/matter/contract/template/${templateId}/preview`,
        data,
      );
    });
  });

  describe('Fee Management', () => {
    it('should get fee list', async () => {
      const params: FeeQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getFeeList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/fee/list', { params });
    });

    it('should get fee detail', async () => {
      const feeId = 1;
      const mockResponse = { id: feeId, amount: 10000 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getFeeDetail(feeId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/finance/fee/${feeId}`);
    });

    it('should create fee', async () => {
      const data: CreateFeeCommand = {
        matterId: 1,
        amount: 10000,
        feeType: 'FIXED',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createFee(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/finance/fee', data);
    });

    it('should update fee', async () => {
      const feeId = 1;
      const data = { amount: 15000 };

      mockPut.mockResolvedValueOnce({ id: feeId, ...data });

      await updateFee(feeId, data);

      expect(mockPut).toHaveBeenCalledWith(`/finance/fee/${feeId}`, data);
    });

    it('should delete fee', async () => {
      const feeId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteFee(feeId);

      expect(mockDelete).toHaveBeenCalledWith(`/finance/fee/${feeId}`);
    });
  });

  describe('Payment Management', () => {
    it('should create payment', async () => {
      const data: CreatePaymentCommand = {
        feeId: 1,
        amount: 10000,
        paymentMethod: 'BANK_TRANSFER',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createPayment(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/finance/fee/payment', data);
    });

    it('should confirm payment', async () => {
      const paymentId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await confirmPayment(paymentId);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/fee/payment/${paymentId}/confirm`,
      );
    });

    it('should cancel payment', async () => {
      const paymentId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await cancelPayment(paymentId);

      expect(mockPost).toHaveBeenCalledWith(
        `/finance/fee/payment/${paymentId}/cancel`,
      );
    });
  });

  describe('Invoice Management', () => {
    it('should get invoice list', async () => {
      const params: InvoiceQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getInvoiceList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/invoice/list', { params });
    });

    it('should get invoice detail', async () => {
      const invoiceId = 1;
      const mockResponse = { id: invoiceId, invoiceNo: 'INV-001' };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getInvoiceDetail(invoiceId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/finance/invoice/${invoiceId}`);
    });

    it('should apply invoice', async () => {
      const data: CreateInvoiceCommand = {
        feeId: 1,
        amount: 10000,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await applyInvoice(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/finance/invoice/apply', data);
    });

    it('should issue invoice', async () => {
      const invoiceId = 1;
      const invoiceNo = 'INV-001';

      mockPost.mockResolvedValueOnce(undefined);

      await issueInvoice(invoiceId, invoiceNo);

      expect(mockPost).toHaveBeenCalledWith(`/finance/invoice/${invoiceId}/issue`, {
        invoiceNo,
      });
    });

    it('should cancel invoice', async () => {
      const invoiceId = 1;
      const reason = 'Cancelled by client';

      mockPost.mockResolvedValueOnce(undefined);

      await cancelInvoice(invoiceId, reason);

      expect(mockPost).toHaveBeenCalledWith(`/finance/invoice/${invoiceId}/cancel`, {
        reason,
      });
    });

    it('should get invoice statistics', async () => {
      const mockResponse = {
        totalAmount: 100000,
        totalCount: 50,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getInvoiceStatistics();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/invoice/statistics');
    });
  });

  describe('Commission Management', () => {
    it('should get commission list', async () => {
      const params: CommissionQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCommissionList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/commission', { params });
    });

    it('should get commission detail', async () => {
      const commissionId = 1;
      const mockResponse = { id: commissionId, amount: 5000 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCommissionDetail(commissionId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/finance/commission/detail/${commissionId}`,
      );
    });

    it('should get pending commission payments', async () => {
      const mockResponse = [
        {
          id: 1,
          amount: 10000,
          status: 'PENDING',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getPendingCommissionPayments();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        '/finance/commission/pending-payments',
      );
    });

    it('should calculate commission', async () => {
      const paymentId = 1;
      const mockResponse = [
        {
          id: 1,
          paymentId,
          userId: 2,
          amount: 5000,
        },
      ];

      // Clear any previous mocks
      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await calculateCommission(paymentId);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/finance/commission/calculate/${paymentId}`,
      );
    });

    it('should manual calculate commission', async () => {
      const data = {
        paymentId: 1,
        participants: [
          {
            userId: 2,
            participantId: 1,
            commissionRate: 0.5,
            commissionAmount: 5000,
          },
        ],
      };
      const mockResponse = [
        {
          id: 1,
          paymentId: 1,
          userId: 2,
          amount: 5000,
        },
      ];

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await manualCalculateCommission(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/finance/commission/manual-calculate',
        data,
      );
    });

    it('should approve commission', async () => {
      const commissionId = 1;
      const approved = true;
      const comment = 'Approved';
      const mockResponse = { id: commissionId, status: 'APPROVED' };

      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await approveCommission(commissionId, approved, comment);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/finance/commission/${commissionId}/approve`,
        null,
        { params: { approved, comment } },
      );
    });

    it('should batch approve commission', async () => {
      const ids = [1, 2, 3];
      const approved = true;
      const comment = 'Batch approved';

      mockPost.mockResolvedValueOnce(undefined);

      await batchApproveCommission(ids, approved, comment);

      expect(mockPost).toHaveBeenCalledWith(
        '/finance/commission/batch-approve',
        ids,
        { params: { approved, comment } },
      );
    });

    it('should issue commission', async () => {
      const commissionId = 1;
      const mockResponse = { id: commissionId, status: 'ISSUED' };

      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await issueCommission(commissionId);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/finance/commission/${commissionId}/issue`,
      );
    });

    it('should batch issue commission', async () => {
      const ids = [1, 2, 3];

      mockPost.mockResolvedValueOnce(undefined);

      await batchIssueCommission(ids);

      expect(mockPost).toHaveBeenCalledWith('/finance/commission/batch-issue', ids);
    });

    it('should get user commission total', async () => {
      const userId = 1;
      const mockResponse = 50000;

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getUserCommissionTotal(userId);

      expect(result).toBe(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/finance/commission/users/${userId}/total`,
      );
    });

    it('should get commission summary', async () => {
      const startDate = '2024-01-01';
      const endDate = '2024-12-31';
      const mockResponse = {
        totalAmount: 100000,
        totalCount: 20,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCommissionSummary(startDate, endDate);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/commission/summary', {
        params: { startDate, endDate },
      });
    });

    it('should get commission report', async () => {
      const startDate = '2024-01-01';
      const endDate = '2024-12-31';
      const userId = 1;
      const mockResponse = {
        report: 'Commission report data',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCommissionReport(startDate, endDate, userId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/commission/report', {
        params: { startDate, endDate, userId },
      });
    });
  });

  describe('Expense Management', () => {
    it('should get expense list', async () => {
      const params: ExpenseQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getExpenseList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/finance/expense', { params });
    });

    it('should get expense detail', async () => {
      const expenseId = 1;
      const mockResponse = { id: expenseId, amount: 1000 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getExpenseDetail(expenseId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/finance/expense/${expenseId}`);
    });

    it('should create expense', async () => {
      const data: CreateExpenseCommand = {
        amount: 1000,
        expenseType: 'TRAVEL',
        description: 'Travel expense',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createExpense(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/finance/expense', data);
    });

    it('should approve expense', async () => {
      const expenseId = 1;
      const data = { approved: true, comment: 'Approved' };
      const mockResponse = { id: expenseId, status: 'APPROVED' };

      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await approveExpense(expenseId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/finance/expense/${expenseId}/approve`,
        data,
      );
    });

    it('should pay expense', async () => {
      const expenseId = 1;
      const paymentMethod = 'BANK_TRANSFER';
      const mockResponse = { id: expenseId, paymentStatus: 'PAID' };

      mockPost.mockClear();
      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await payExpense(expenseId, paymentMethod);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/finance/expense/${expenseId}/pay`,
        null,
        { params: { paymentMethod } },
      );
    });

    it('should delete expense', async () => {
      const expenseId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteExpense(expenseId);

      expect(mockDelete).toHaveBeenCalledWith(`/finance/expense/${expenseId}`);
    });
  });
});
