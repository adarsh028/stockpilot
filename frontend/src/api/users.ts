import client from "./client";
import { Organization, Page, UserResponse } from "@/types/api";

export interface InviteUserInput {
  fullName: string;
  email: string;
  phone: string;
  role: "ADMIN" | "STAFF";
  password?: string;
}

export interface UpdateUserInput {
  fullName?: string;
  role?: string;
  status?: string;
}

export const usersApi = {
  list: (page = 0, size = 20) =>
    client.get<Page<UserResponse>>("/users", { params: { page, size } }).then((r) => r.data),
  invite: (body: InviteUserInput) => client.post<UserResponse>("/users", body).then((r) => r.data),
  update: (id: string, body: UpdateUserInput) =>
    client.patch<UserResponse>(`/users/${id}`, body).then((r) => r.data),
  remove: (id: string) => client.delete(`/users/${id}`).then((r) => r.data),
};

export const orgApi = {
  get: () => client.get<Organization>("/organization").then((r) => r.data),
  update: (name: string) =>
    client.patch<Organization>("/organization", { name }).then((r) => r.data),
};
