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

export interface TransactionSummary {
  categoryId: number | null;
  categoryName: string;
  categoryColor: string | null;
  categoryIcon: string | null;
  totalCents: number;
  transactionCount: number;
}

export interface CategoryResponse {
  id: number;
  name: string;
  icon: string | null;
  color: string | null;
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

  getTransactionSummary(startDate?: string, endDate?: string): Promise<TransactionSummary[]> {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    const query = params.toString();
    return request(`/transactions/summary${query ? `?${query}` : ''}`);
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
};

export { ApiError };
