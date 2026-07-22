import client from "./client";
import { AuthResponse, MessageResponse, SignupRequest, UserResponse } from "@/types/api";

export const authApi = {
  signup: (body: SignupRequest) =>
    client.post<MessageResponse>("/auth/signup", body).then((r) => r.data),

  verifyOtp: (identifier: string, code: string) =>
    client.post<AuthResponse>("/auth/verify-otp", { identifier, code }).then((r) => r.data),

  resendOtp: (identifier: string) =>
    client.post<MessageResponse>("/auth/resend-otp", { identifier }).then((r) => r.data),

  login: (identifier: string, password: string) =>
    client.post<AuthResponse>("/auth/login", { identifier, password }).then((r) => r.data),

  forgotPassword: (identifier: string) =>
    client.post<MessageResponse>("/auth/forgot-password", { identifier }).then((r) => r.data),

  resetPassword: (identifier: string, code: string, newPassword: string) =>
    client
      .post<MessageResponse>("/auth/reset-password", { identifier, code, newPassword })
      .then((r) => r.data),

  me: () => client.get<UserResponse>("/auth/me").then((r) => r.data),
};
