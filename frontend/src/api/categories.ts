import client from "./client";
import { Category } from "@/types/api";

export interface CategoryInput {
  name: string;
  isActive?: boolean;
}

export const categoriesApi = {
  list: () => client.get<Category[]>("/categories").then((r) => r.data),
  create: (body: CategoryInput) => client.post<Category>("/categories", body).then((r) => r.data),
  update: (id: string, body: CategoryInput) =>
    client.patch<Category>(`/categories/${id}`, body).then((r) => r.data),
  deactivate: (id: string) => client.delete(`/categories/${id}`).then((r) => r.data),
};
