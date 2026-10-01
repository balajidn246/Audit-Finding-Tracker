import type { AuditEvent, Evidence, EventPage, Finding, FindingPage, Stats, User } from './types';

let accessToken: string | null = null;
export const setAccessToken = (token: string | null) => { accessToken = token; };

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' });
  if (response.status === 401 && retry && path !== '/api/auth/login' && path !== '/api/auth/refresh') {
    if (await refreshSession()) return request<T>(path, init, false);
  }
  if (!response.ok) { const body = await response.json().catch(() => null); throw new Error(body?.detail || body?.message || body?.error || `Request failed (${response.status})`); }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export async function login(username: string, password: string) {
  const data = await request<{ accessToken: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false);
  setAccessToken(data.accessToken);
}
export async function refreshSession(): Promise<boolean> {
  try { const data = await request<{ accessToken: string }>('/api/auth/refresh', { method: 'POST', body: '{}' }, false); setAccessToken(data.accessToken); return true; }
  catch { setAccessToken(null); return false; }
}
export async function logout() { try { await request('/api/auth/logout', { method: 'POST' }, false); } finally { setAccessToken(null); } }
export const api = {
  stats: () => request<Stats>('/api/findings/stats'),
  findings: (params: URLSearchParams) => request<FindingPage>(`/api/findings?${params}`),
  finding: (id: string) => request<Finding>(`/api/findings/${id}`),
  create: (data: object) => request<Finding>('/api/findings', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: object) => request<Finding>(`/api/findings/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  status: (id: string, status: string, reason?: string, riskAcceptanceExpiresOn?: string) => request<Finding>(`/api/findings/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, reason, riskAcceptanceExpiresOn }) }),
  history: (id: string) => request<{ content: AuditEvent[] }>(`/api/findings/${id}/history?size=100`),
  evidence: (id: string) => request<Evidence[]>(`/api/findings/${id}/evidence`),
  uploadEvidence: (id: string, file: File) => { const form = new FormData(); form.append('file', file); return request<Evidence>(`/api/findings/${id}/evidence`, { method: 'POST', body: form }); },
  downloadEvidence: async (findingId: string, evidenceId: string, name: string) => {
    const send = () => { const headers = new Headers(); if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`); return fetch(`/api/findings/${findingId}/evidence/${evidenceId}`, { headers, credentials: 'same-origin' }); };
    let response = await send(); if (response.status === 401 && await refreshSession()) response = await send();
    if (!response.ok) throw new Error('Unable to download evidence');
    const url = URL.createObjectURL(await response.blob()); const anchor = document.createElement('a'); anchor.href = url; anchor.download = name; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  },
  downloadReport: async (filters?: { q?: string; status?: string }) => {
    const params = new URLSearchParams(); if (filters?.q) params.set('q', filters.q); if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status);
    const send = () => { const headers = new Headers(); if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`); return fetch(`/api/findings/report.csv?${params}`, { headers }); };
    let response = await send(); if (response.status === 401 && await refreshSession()) response = await send();
    if (!response.ok) throw new Error('Only audit managers can export reports');
    const url = URL.createObjectURL(await response.blob()); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'verityops-findings.csv'; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  },
  users: () => request<User[]>('/api/users/assignees'),
  adminUsers: () => request<User[]>('/api/admin/users'),
  adminAuditEvents: () => request<EventPage>('/api/admin/audit-events?size=50'),
  createUser: (data: object) => request<User>('/api/admin/users', { method: 'POST', body: JSON.stringify(data) }),
  setUserEnabled: (id: number, enabled: boolean) => request(`/api/admin/users/${id}/${enabled ? 'activate' : 'deactivate'}`, { method: 'POST' }),
  me: () => request<User>('/api/auth/me'),
};
