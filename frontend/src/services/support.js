import httpClient from "../api/httpClient";

export const fetchStudentThreads = (params = {}) =>
  httpClient.get("/api/support/threads/my", { params });

export const createSupportThread = (payload) => httpClient.post("/api/support/threads", payload);

export const fetchSupportThread = (threadId) => httpClient.get(`/api/support/threads/${threadId}`);

export const sendSupportMessage = (threadId, payload) =>
  httpClient.post(`/api/support/threads/${threadId}/messages`, payload);

export const submitSupportRating = (threadId, payload) =>
  httpClient.post(`/api/support/threads/${threadId}/rating`, payload);

// Manager endpoints
export const managerListThreads = (params = {}) =>
  httpClient.get("/api/support/manager/threads", { params });

export const managerFetchThread = (threadId) => httpClient.get(`/api/support/threads/${threadId}`);

export const managerAssignThread = (threadId) =>
  httpClient.post(`/api/support/manager/threads/${threadId}/claim`);

export const managerSendMessage = (threadId, payload) =>
  httpClient.post(`/api/support/manager/threads/${threadId}/messages`, payload);

export const managerChangeStatus = (threadId, payload) =>
  httpClient.post(`/api/support/manager/threads/${threadId}/status`, payload);

export const managerTransferThread = (threadId, payload) =>
  httpClient.post(`/api/support/manager/threads/${threadId}/transfer`, payload);
