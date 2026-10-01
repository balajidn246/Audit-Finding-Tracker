export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type FindingStatus = 'OPEN' | 'IN_PROGRESS' | 'PENDING_VALIDATION' | 'REWORK_REQUIRED' | 'RISK_ACCEPTED' | 'CLOSED';
export interface Finding {
  uuid: string; title: string; description: string; severity: Severity; status: FindingStatus;
  rootCause?: string; recommendation?: string; managementResponse?: string; sourceAudit?: string;
  businessUnit?: string; ownerId?: number; createdById?: number; dueDate?: string; closedAt?: string;
  riskAcceptanceExpiresOn?: string;
  createdAt: string; updatedAt: string; version: number;
}
export interface FindingPage { content: Finding[]; totalElements: number; totalPages: number; number: number; }
export interface AuditEvent { eventId: string; actorId?: number; entityType: string; action: string; oldJson?: string; newJson?: string; createdAt: string; }
export interface User { id: number; username: string; email: string; roles: string[]; enabled: boolean; createdAt?: string; }
export interface Evidence { id: string; originalName: string; mediaType: string; sizeBytes: number; sha256: string; createdAt: string; }
export interface EventPage { content: AuditEvent[]; totalElements: number; }
export interface Stats { total: number; open: number; in_progress: number; pending_validation: number; rework_required: number; risk_accepted: number; closed: number; }
