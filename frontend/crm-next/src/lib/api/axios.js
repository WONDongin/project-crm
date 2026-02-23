// axios 인스턴스 설정, 요청/응답 인터셉터 (토큰 자동첨부, 401처리, 공통 에러 메세지 변환)
// src/lib/api/axios.js
import axios from "axios";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  timeout: 10000,
  withCredentials: true, // 쿠키 자동 포함
});

// 요청 인터셉터 - 토큰 자동 첨부
apiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => Promise.reject(error),
);

// 응답 인터셉터 - 401 처리 (토큰 만료, 미 로그인 상태)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401 && typeof window !== "undefined") {
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
