// DTO interfaces mirroring the backend response/request shapes.

export type UserRole = "OWNER" | "ADMIN" | "STAFF";

export interface AuthUser {
  id: string;
  organizationId: string;
  organizationName: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: AuthUser;
}

export interface MessageResponse {
  message: string;
}

export interface SignupRequest {
  organizationName: string;
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface UserResponse {
  id: string;
  organizationId: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  status: "PENDING" | "ACTIVE" | "DISABLED";
  emailVerified: boolean;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type ChannelType = "MARKETPLACE" | "OWN_WEBSITE" | "OFFLINE" | "OTHER";

export interface Channel {
  id: string;
  name: string;
  code: string;
  type: ChannelType;
  isActive: boolean;
}

export interface Sku {
  id: string;
  productId: string;
  sku: string;
  attributes: Record<string, string>;
  costPrice: number;
  sellingPrice: number;
  quantityOnHand: number;
  reorderLevel: number;
}

export interface Product {
  id: string;
  name: string;
  category: string | null;
  brandName: string | null;
  description: string | null;
  imageUrl: string | null;
  status: "ACTIVE" | "ARCHIVED";
  skus: Sku[];
  createdAt: string;
  updatedAt: string;
}

export interface SkuInput {
  id?: string;
  sku: string;
  attributes?: Record<string, string>;
  costPrice?: number;
  sellingPrice?: number;
  quantityOnHand?: number;
  reorderLevel?: number;
}

export interface ProductInput {
  name: string;
  category?: string;
  brandName?: string;
  description?: string;
  imageUrl?: string;
  status?: string;
  skus: SkuInput[];
}

export interface InventoryItem {
  id: string;
  skuId: string;
  sku: string;
  productId: string;
  productName: string;
  quantityOnHand: number;
  reorderLevel: number;
  totalAllocated: number;
  lowStock: boolean;
}

export interface ChannelListing {
  id: string;
  channelId: string;
  channelName: string;
  skuId: string;
  sku: string;
  productName: string;
  channelSku: string | null;
  allocatedQuantity: number;
  channelPrice: number | null;
  status: "ACTIVE" | "PAUSED";
}

export interface Sale {
  id: string;
  channelId: string;
  channelName: string;
  skuId: string;
  sku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  saleDate: string;
  marketplaceOrderId: string | null;
  source: "MANUAL" | "IMPORT";
  status: "COMPLETED" | "RETURNED";
  warnings: string[];
}

export interface ImportBatch {
  id: string;
  kind: "SALES" | "PRODUCTS";
  channelId: string | null;
  fileName: string;
  status: "PROCESSING" | "COMPLETED" | "FAILED";
  rowsTotal: number;
  rowsSuccess: number;
  rowsFailed: number;
  hasErrorReport: boolean;
  createdAt: string;
}

export interface AnalyticsSummary {
  orderCount: number;
  unitsSold: number;
  revenue: number;
  lowStockCount: number;
  revenueChangePct: number;
  orderCountPrevious: number;
  revenuePrevious: number;
  from: string;
  to: string;
}

export interface SalesByChannel {
  channelId: string;
  channelName: string;
  units: number;
  revenue: number;
}

export interface SalesTrendPoint {
  bucket: string;
  units: number;
  revenue: number;
}

export interface TopProduct {
  productId: string;
  productName: string;
  sku: string;
  units: number;
  revenue: number;
}

export interface ChannelComparison {
  channelId: string;
  channelName: string;
  units: number;
  revenue: number;
  unitsPrevious: number;
  revenuePrevious: number;
  revenueChangePct: number;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  plan: string;
  status: string;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: { field: string; message: string }[];
}
