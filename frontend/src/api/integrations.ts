import client from "./client";
import { DriveStatus } from "@/types/api";

/**
 * Google Drive integration endpoints. Connecting is a full-page redirect to Google's
 * consent screen (not an XHR), so `connect()` navigates the browser to the authorize URL
 * the backend returns; Google then redirects back to the backend callback, which sends
 * the browser to /settings?drive=connected.
 */
export const googleDriveApi = {
  status: () =>
    client.get<DriveStatus>("/integrations/google-drive/status").then((r) => r.data),

  authorizeUrl: () =>
    client
      .get<{ url: string }>("/integrations/google-drive/authorize-url")
      .then((r) => r.data.url),

  disconnect: () => client.delete("/integrations/google-drive").then((r) => r.data),
};
