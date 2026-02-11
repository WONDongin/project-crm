// 로그인/로그아웃 등 인증관련 API 호출 (login(), logout())
// src/lib/api/auth.api.js
import apiClient from "./axios";

// 로그인
export const login = async (data) => {
  try {
    const response = await apiClient.post("/auth/login", data);

    if (response.data?.accessToken) {
      localStorage.setItem("accessToken", response.data.accessToken);
    }

    return response.data;
  } catch (error) {
    console.error("로그인 에러:", error);
    throw error;
  }
};

// 로그아웃
export const logout = async () => {
  try {
    await apiClient.post("/auth/logout");
  } finally {
    localStorage.removeItem("accessToken");
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  }
};
