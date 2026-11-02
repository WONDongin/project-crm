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

// 상담사 담당 고객 상태 변경
export const updateConsCustomerStatus = async (customerId, status) => {
  try {
    const response = await api.patch(
      `/consultant/customers/${customerId}/status`,
      { status },
    );
    return response.data;
  } catch (error) {
    console.error("고객 상태 변경 에러:", error);
    throw error;
  }
};

// 상담사 담당 고객 태그 변경
export const updateConsCustomerTag = async (customerId, customerTag) => {
  try {
    const response = await api.patch(
      `/consultant/customers/${customerId}/tag`,
      { customerTag },
    );
    return response.data;
  } catch (error) {
    console.error("고객 태그 변경 에러:", error);
    throw error;
  }
};

// 상담사 담당 고객 상태 및 태그 변경
export const updateConsCustomer = async (customerId, updates) => {
  try {
    const response = await api.patch(
      `/consultant/customers/${customerId}`,
      updates,
    );
    return response.data;
  } catch (error) {
    console.error("고객 정보 변경 에러:", error);
    throw error;
  }
};
