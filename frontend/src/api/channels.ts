import client from "./client";
import { Channel } from "@/types/api";

export interface ChannelInput {
  name: string;
  code: string;
  type: string;
  isActive?: boolean;
}

export const channelsApi = {
  list: () => client.get<Channel[]>("/channels").then((r) => r.data),
  create: (body: ChannelInput) => client.post<Channel>("/channels", body).then((r) => r.data),
  update: (id: string, body: ChannelInput) =>
    client.patch<Channel>(`/channels/${id}`, body).then((r) => r.data),
  deactivate: (id: string) => client.delete(`/channels/${id}`).then((r) => r.data),
};
