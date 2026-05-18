// 상담사 전용 API 호출모음 (getCustomerList(), getCustomerDetail(), createActivityLog())
import api from "./axios";

// 상담사 대시보드_고객 목록 조회
export const getConscustomers = async () => {
  try {
    const response = await api.get("/consultant/customers");
    return response.data;
  } catch (error) {
    console.error("유저 목록 조회 에러:", error);
    throw error;
  }
};
