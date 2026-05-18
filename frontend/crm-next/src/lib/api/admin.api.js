// 관리자 전용 API 호출모음 (getAdminDashboardSummary(), updateUser(), resetPassword())
import api from "./axios";

// 관리자 대시보드_사용자 관리
export const getUsers = async () => {
  try {
    const response = await api.get("/admin/users");

    return response.data;
  } catch (error) {
    console.error("유저 목록 조회 에러:", error);
    throw error;
  }
};

// 관리자 대시보드_고객 관리
export const getCustomers = async () => {
  try {
    const response = await api.get("/admin/customers");

    return response.data;
  } catch (error) {
    console.error("고객 목록 조회 에러:", error);
    throw error;
  }
};
