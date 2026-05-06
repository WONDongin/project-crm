// 관리자 전용 API 호출모음 (getAdminDashboardSummary(), updateUser(), resetPassword())
import api from "./axios";

export const getUsers = async () => {
  try {
    const response = await api.get("/admin/users");

    return response.data;
  } catch (error) {
    console.error("유저 목록 조회 에러:", error);
    throw error;
  }
};
