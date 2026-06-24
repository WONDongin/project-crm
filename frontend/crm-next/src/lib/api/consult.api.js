// 상담 신청 API 호출
import api from "./axios";

// 상담 신청
export const applyConsult = async (data) => {
  try {
    const response = await api.post("/consult/apply", data);
    return response.data;
  } catch (error) {
    console.error("상담 신청 중 오류 발생:", error);
    throw error;
  }
};
