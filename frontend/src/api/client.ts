import axios, { AxiosError } from "axios";
import type { ApiErrorBody } from "./types";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorBody>;
    const body = axiosError.response?.data;
    if (body?.message) {
      return body.message;
    }
    if (body && typeof body === "object") {
      const fieldErrors = Object.entries(body)
        .filter(([key]) => key !== "message")
        .map(([field, message]) => `${field}: ${message}`);
      if (fieldErrors.length > 0) {
        return fieldErrors.join(", ");
      }
    }
    if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
      return "You're not authorized to do that.";
    }
  }
  return "Something went wrong. Please try again.";
}
