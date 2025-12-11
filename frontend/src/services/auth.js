import httpClient from "../api/httpClient";

export const login = (payload) => httpClient.post("/api/auth/login", payload);

export const loginWithGoogle = (payload) => httpClient.post("/api/auth/login/google", payload);

export const loginWithFacebook = (payload) => httpClient.post("/api/auth/login/facebook", payload);

export const fetchMe = () => httpClient.get("/api/auth/me");

export const startForgotPassword = (payload) =>
  httpClient.post("/api/auth/otp/password/forgot/start", payload);

export const completeForgotPassword = (payload) =>
  httpClient.post("/api/auth/otp/password/forgot/complete", payload);
