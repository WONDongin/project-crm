// src/lib/api/common.api.js
import api from "./axios";

// 공통 코드 조회
export const getCommonCodes = async () => {
  try {
    const response = await api.get("/common-codes");

    return response.data;
  } catch (error) {
    console.error("공통 코드 조회 에러:", error);
    throw error;
  }
};
