const API_BASE = '/api';

class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new ApiError(response.status, body.message || 'Request failed');
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export interface UserResponse {
  id: number;
  email: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
}

export interface TransactionResponse {
  id: number;
  accountId: number;
  categoryId: number | null;
  categoryName: string;
  amountCents: number;
  transactionType: 'CREDIT' | 'DEBIT';
  description: string;
  merchantName: string | null;
  date: string;
  externalId: string | null;
}

export interface TransactionPage {
  content: TransactionResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  categoryId?: number;
  minAmount?: number;
  maxAmount?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface UpdateTransactionRequest {
  categoryId: number | null;
  amountCents: number;
  transactionType: 'CREDIT' | 'DEBIT';
  description: string;
  merchantName: string | null;
  date: string;
}

export interface BulkCategorizeResponse {
  updated: number;
  invalidIds?: number[];
}

export interface CategoryResponse {
  id: number;
  name: string;
  icon: string | null;
  color: string | null;
}

export interface ProviderResponse {
  id: number;
  name: string;
  description: string;
}

export interface AccountResponse {
  id: number;
  providerId: number;
  providerName: string;
  providerAccountId: string;
  accountName: string;
  balanceCents: number;
  lastSyncedAt: string | null;
}

export interface SyncResponse {
  transactionsAdded: number;
  transactionsUpdated: number;
  transactionsSkipped: number;
  accountBalanceCents: number;
  syncedAt: string;
}

export interface CategoryBreakdown {
  categoryId: number | null;
  categoryName: string;
  categoryColor: string;
  amountCents: number;
  percentage: number;
}

export interface DashboardSummaryResponse {
  incomeCents: number;
  expensesCents: number;
  netCents: number;
  byCategory: CategoryBreakdown[];
}

export interface AvailableMonthsResponse {
  months: string[];
}

export const api = {
  register(email: string, password: string): Promise<UserResponse> {
    return request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  login(email: string, password: string): Promise<LoginResponse> {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  logout(): Promise<void> {
    return request('/auth/logout', { method: 'POST' });
  },

  getTransactions(filters: TransactionFilters = {}): Promise<TransactionPage> {
    const params = new URLSearchParams();
    if (filters.startDate) params.set('startDate', filters.startDate);
    if (filters.endDate) params.set('endDate', filters.endDate);
    if (filters.categoryId != null) params.set('categoryId', String(filters.categoryId));
    if (filters.minAmount != null) params.set('minAmount', String(filters.minAmount));
    if (filters.maxAmount != null) params.set('maxAmount', String(filters.maxAmount));
    if (filters.search) params.set('search', filters.search);
    if (filters.page != null) params.set('page', String(filters.page));
    if (filters.size != null) params.set('size', String(filters.size));
    if (filters.sort) params.set('sort', filters.sort);
    const query = params.toString();
    return request(`/transactions${query ? `?${query}` : ''}`);
  },

  updateTransaction(id: number, data: UpdateTransactionRequest): Promise<TransactionResponse> {
    return request(`/transactions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  bulkCategorize(transactionIds: number[], categoryId: number): Promise<BulkCategorizeResponse> {
    return request('/transactions/bulk-categorize', {
      method: 'PUT',
      body: JSON.stringify({ transactionIds, categoryId }),
    });
  },

  getCategories(): Promise<CategoryResponse[]> {
    return request('/categories');
  },

  createCategory(name: string, icon?: string, color?: string): Promise<CategoryResponse> {
    return request('/categories', {
      method: 'POST',
      body: JSON.stringify({ name, icon, color }),
    });
  },

  getProviders(): Promise<ProviderResponse[]> {
    return request('/providers');
  },

  getAccounts(): Promise<AccountResponse[]> {
    return request('/accounts');
  },

  linkAccounts(providerId: number, setupToken?: string): Promise<AccountResponse[]> {
    return request('/accounts/link', {
      method: 'POST',
      body: JSON.stringify({ providerId, setupToken }),
    });
  },

  syncAccount(id: number): Promise<SyncResponse> {
    return request(`/accounts/${id}/sync`, { method: 'POST' });
  },

  deleteAccount(id: number): Promise<void> {
    return request(`/accounts/${id}`, { method: 'DELETE' });
  },

  getDashboardSummary(startDate: string, endDate: string): Promise<DashboardSummaryResponse> {
    return request(`/dashboard/summary?startDate=${startDate}&endDate=${endDate}`);
  },

  getAvailableMonths(): Promise<AvailableMonthsResponse> {
    return request('/dashboard/available-months');
  },
};

export interface SimpleFINStatusResponse {
  hasToken: boolean;
}

export interface MessageResponse {
  message: string;
}

export const settingsApi = {
  changePassword(currentPassword: string, newPassword: string): Promise<MessageResponse> {
    return request('/settings/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
    });
  },

  getSimpleFINStatus(): Promise<SimpleFINStatusResponse> {
    return request('/settings/simplefin-status');
  },

  updateSimpleFINToken(setupToken: string): Promise<MessageResponse> {
    return request('/settings/simplefin-token', {
      method: 'PUT',
      body: JSON.stringify({ setupToken }),
    });
  },

  deleteSimpleFINToken(): Promise<void> {
    return request('/settings/simplefin-token', { method: 'DELETE' });
  },
};

export { ApiError };
