const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "/whatsapp-ai-response-service/v1").replace(/\/$/, "");

export type PromotionStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";

export interface AdminSession {
  userId: number;
  clinicId: number;
  email: string;
  role: "ADMIN" | "EDITOR";
}

export interface Promotion {
  id: number;
  clinicId: number;
  code: string;
  title: string;
  messageBody: string;
  aliases: string[];
  status: PromotionStatus;
  validFrom: string | null;
  validUntil: string | null;
  currentlyActive: boolean;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionPayload {
  code: string;
  title: string;
  messageBody: string;
  aliases: string[];
  validFrom: string | null;
  validUntil: string | null;
}

export interface PromotionPage {
  content: Promotion[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PromotionMatch {
  id: number;
  code: string;
  title: string;
  messageBody: string;
}

interface CsrfToken {
  token: string;
  headerName: string;
  parameterName: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number
  ) {
    super(message);
  }
}

let csrfToken: CsrfToken | null = null;

async function readError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as Record<string, unknown>;
    const message = body.message ?? body.error ?? body.detail ?? body.reason;
    if (typeof message === "string" && message.trim()) {
      return message;
    }
  } catch {
    // The fallback below is used for empty or non-JSON error responses.
  }
  return response.status === 401
    ? "La sesión venció o las credenciales son incorrectas."
    : "No se pudo completar la operación.";
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...init.headers
    }
  });

  if (!response.ok) {
    throw new ApiError(await readError(response), response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function ensureCsrf(force = false): Promise<CsrfToken> {
  if (!csrfToken || force) {
    csrfToken = await request<CsrfToken>("/api/admin/csrf");
  }
  return csrfToken;
}

async function mutation<T>(path: string, init: RequestInit): Promise<T> {
  const csrf = await ensureCsrf();
  return request<T>(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
      ...init.headers
    }
  });
}

export async function getSession(): Promise<AdminSession | null> {
  try {
    return await request<AdminSession>("/api/admin/session");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function login(email: string, password: string): Promise<AdminSession> {
  const csrf = await ensureCsrf(true);
  const body = new URLSearchParams({ username: email, password });
  const session = await request<AdminSession>("/api/admin/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      [csrf.headerName]: csrf.token
    },
    body
  });
  await ensureCsrf(true);
  return session;
}

export async function logout(): Promise<void> {
  const csrf = await ensureCsrf();
  await request<void>("/api/admin/logout", {
    method: "POST",
    headers: { [csrf.headerName]: csrf.token }
  });
  csrfToken = null;
}

export async function listPromotions(
  query: string,
  status: PromotionStatus | "ALL"
): Promise<PromotionPage> {
  const parameters = new URLSearchParams({ page: "0", size: "100", sort: "updatedAt,desc" });
  if (query.trim()) {
    parameters.set("q", query.trim());
  }
  if (status !== "ALL") {
    parameters.set("status", status);
  }
  return request<PromotionPage>(`/api/admin/promotions?${parameters.toString()}`);
}

export function createPromotion(payload: PromotionPayload): Promise<Promotion> {
  return mutation<Promotion>("/api/admin/promotions", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updatePromotion(promotion: Promotion, payload: PromotionPayload): Promise<Promotion> {
  return mutation<Promotion>(`/api/admin/promotions/${promotion.id}`, {
    method: "PUT",
    body: JSON.stringify({ ...payload, version: promotion.version })
  });
}

export function activatePromotion(promotion: Promotion): Promise<Promotion> {
  return mutation<Promotion>(`/api/admin/promotions/${promotion.id}/activate`, {
    method: "POST",
    body: JSON.stringify({ version: promotion.version })
  });
}

export function archivePromotion(promotion: Promotion): Promise<Promotion> {
  return mutation<Promotion>(`/api/admin/promotions/${promotion.id}/archive`, {
    method: "POST",
    body: JSON.stringify({ version: promotion.version })
  });
}

export async function matchPromotions(message: string): Promise<PromotionMatch[]> {
  const response = await mutation<{ matches: PromotionMatch[] }>("/api/admin/promotions/match-preview", {
    method: "POST",
    body: JSON.stringify({ message })
  });
  return response.matches;
}
