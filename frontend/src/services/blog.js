import httpClient from "../api/httpClient";

export const fetchPublicPosts = (params) => httpClient.get("/api/public/blog", { params });

export const fetchPublicPost = (slug) => httpClient.get(`/api/public/blog/${slug}`);

export const fetchMyPosts = () => httpClient.get("/api/blog/mine");

export const fetchBlogDetail = (id) => httpClient.get(`/api/blog/${id}`);

export const createBlogPost = (payload) => httpClient.post("/api/blog", payload);

export const updateBlogPost = (id, payload) => httpClient.put(`/api/blog/${id}`, payload);

export const submitBlogPost = (id) => httpClient.post(`/api/blog/${id}/submit`);

export const fetchPendingBlogPosts = () => httpClient.get("/api/admin/blog/pending");

export const approveBlogPost = (id) => httpClient.post(`/api/admin/blog/${id}/approve`);

export const rejectBlogPost = (id, payload) => httpClient.post(`/api/admin/blog/${id}/reject`, payload);

export const fetchAdminBlogDetail = (id) => httpClient.get(`/api/admin/blog/${id}`);

export const deleteBlogPost = (id) => httpClient.delete(`/api/admin/blog/${id}`);
