import httpClient from "../api/httpClient";

export const getProfile = () => httpClient.get("/api/profile");

export const updateProfile = (payload) => httpClient.put("/api/profile", payload);

export const changePassword = (payload) => httpClient.put("/api/profile/password", payload);

export const startPasswordOtp = (payload) => httpClient.post("/api/profile/password/otp/start", payload);

export const completePasswordOtp = (payload) => httpClient.post("/api/profile/password/otp/complete", payload);
