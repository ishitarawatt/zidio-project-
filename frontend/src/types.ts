export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type WorkOrderStatus =
  | 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';

export interface AuthUser {
  token: string;
  email: string;
  role: Role;
  name: string;
  customerId: number | null;
}

export interface WorkOrder {
  id: number;
  code: string;
  title: string;
  description: string;
  priority: Priority;
  status: WorkOrderStatus;
  slaDueAt: string | null;
  slaBreached: boolean;
  customerId: number;
  customerName: string;
  siteId: number;
  siteName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DashboardSummary {
  countsByStatus: Record<string, number>;
  overdueCount: number;
  slaCompliancePercent: number;
  openCountByTechnician: Record<string, number>;
  openCountBySite: Record<string, number>;
}
