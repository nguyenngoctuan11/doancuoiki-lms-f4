import httpClient from "../api/httpClient";

export const uploadImage = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return httpClient.post("/api/upload/image", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};
