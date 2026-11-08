// 관리자 전용 API 호출모음 (getAdminDashboardSummary(), updateUser(), resetPassword())
import api from "./axios";

// 관리자 대시보드_사용자 관리
export const getUsers = async (params = {}, signal) => {
  const response = await api.get("/admin/users", { params, signal });
  return response.data;
};

// 관리자 대시보드_고객 관리
export const getCustomers = async (params = {}, signal) => {
  const response = await api.get("/admin/customers", { params, signal });
  return response.data;
};

// 배정 가능한 활성 상담사 목록 조회
export const getAssignableCounselors = async (signal) => {
  const response = await api.get("/admin/customers/counselors", { signal });

  return response.data;
};

// 고객 담당 상담사 배정 및 변경
export const assignCustomerCounselor = async (customerId, counselorId) => {
  const response = await api.patch(
    `/admin/customers/${customerId}/counselor`,
    { counselorId },
  );

  return response.data;
};
export const createAdminUser = async (draft) => (await api.post('/admin/users', draft)).data;
export const updateAdminUser = async (id, draft) => (await api.patch(`/admin/users/${id}`, draft)).data;
export const resetAdminUserPassword = async (id, password) => (await api.post(`/admin/users/${id}/password`, { password })).data;
export const unassignCustomerCounselor = async (id) => (await api.delete(`/admin/customers/${id}/counselor`)).data;

export const getAdminDashboard = async (signal) => (await api.get("/admin/dashboard", { signal })).data;
