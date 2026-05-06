// 로그인/로그아웃 등 인증관련 API 호출 (login(), logout())
// src/lib/api/auth.api.js
import api from "./axios";

// 로그인
export const login = async (data) => {
  try {
    const response = await api.post("/auth/login", data);

    return response.data;
  } catch (error) {
    console.error("로그인 에러:", error);
    throw error;
  }
};

// 로그아웃
export const logout = async () => {
  try {
    await api.post("/auth/logout");
  } finally {
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  }
};
