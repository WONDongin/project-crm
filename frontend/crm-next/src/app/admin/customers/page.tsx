// 고객 관리 페이지
"use client";

import { useEffect, useRef, useState } from "react";
import axios from "axios";

import type { Customer } from "@/types/customer";
import {
  assignCustomerCounselor,
  getAssignableCounselors,
  getCustomers,
} from "@/lib/api/admin.api";
import FeedbackModal from "@/components/common/FeedbackModal";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";

import styles from "../AdminLayout.module.css";

type Counselor = {
  userId: number;
  name: string;
  email: string;
};

type Feedback = {
  type: "success" | "error";
  title: string;
  message: string;
};

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string; detail?: string }>(error)) {
    return (
      error.response?.data?.message ||
      error.response?.data?.detail ||
      "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
    );
  }

  return error instanceof Error
    ? error.message
    : "알 수 없는 오류가 발생했습니다.";
}

export default function AdminCustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [counselors, setCounselors] = useState<Counselor[]>([]);

  const [changedCounselors, setChangedCounselors] = useState<
    Record<number, string>
  >({});

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [savingCustomerId, setSavingCustomerId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<Feedback | null>(null);

  // 빠르게 연속 클릭해도 요청이 중복되지 않도록 처리
  const savingRef = useRef(false);

  const { codes, hydrated } = useCodeStore();

  // 고객과 배정 가능한 상담사 목록 조회
  useEffect(() => {
    let cancelled = false;

    const loadData = async () => {
      try {
        const [customerResponse, counselorResponse] = await Promise.all([
          getCustomers(),
          getAssignableCounselors(),
        ]);

        if (!customerResponse.success || !counselorResponse.success) {
          throw new Error("고객 또는 상담사 목록을 불러오지 못했습니다.");
        }

        if (!cancelled) {
          setCustomers(customerResponse.data);
          setCounselors(counselorResponse.data);
        }
      } catch (error) {
        if (!cancelled) {
          setLoadError(getErrorMessage(error));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void loadData();

    return () => {
      cancelled = true;
    };
  }, []);

  // 기존 담당자와 다른 상담사를 선택한 경우에만 변경사항 보관
  const handleCounselorChange = (customer: Customer, value: string) => {
    const originalValue = String(customer.counselorId ?? "");

    setChangedCounselors((previous) => {
      const next = { ...previous };

      if (value === originalValue) {
        delete next[customer.customerId];
      } else {
        next[customer.customerId] = value;
      }

      return next;
    });
  };

  // 담당 상담사 저장
  const handleSaveCounselor = async (customer: Customer) => {
    if (savingRef.current) return;

    const selectedValue = changedCounselors[customer.customerId];
    if (!selectedValue) return;

    const selectedCounselor = counselors.find(
      (counselor) => counselor.userId === Number(selectedValue),
    );

    if (!selectedCounselor) {
      setFeedback({
        type: "error",
        title: "선택 확인",
        message: "배정 가능한 상담사를 선택해 주세요.",
      });
      return;
    }

    savingRef.current = true;
    setSavingCustomerId(customer.customerId);

    try {
      const response = await assignCustomerCounselor(
        customer.customerId,
        selectedCounselor.userId,
      );

      if (!response.success) {
        throw new Error(response.message || "담당 상담사 저장에 실패했습니다.");
      }

      // 백엔드 배정 규칙에 맞춰 저장된 행의 화면 갱신
      setCustomers((previous) =>
        previous.map((item) =>
          item.customerId === customer.customerId
            ? {
                ...item,
                counselorId: selectedCounselor.userId,
                counselorName: selectedCounselor.name,
                status: item.status === "NEW" ? "IN_PROGRESS" : item.status,
              }
            : item,
        ),
      );

      setChangedCounselors((previous) => {
        const next = { ...previous };
        delete next[customer.customerId];
        return next;
      });

      setFeedback({
        type: "success",
        title: "저장 완료",
        message: response.message || "담당 상담사가 저장되었습니다.",
      });
    } catch (error) {
      setFeedback({
        type: "error",
        title: "저장 실패",
        message: getErrorMessage(error),
      });
    } finally {
      savingRef.current = false;
      setSavingCustomerId(null);
    }
  };

  if (loading || !hydrated) {
    return <div>고객 관리 정보를 불러오는 중...</div>;
  }

  if (loadError) {
    return (
      <div className={styles.pageCard}>
        <h2 className={styles.pageTitle}>고객 관리</h2>
        <p role="alert">{loadError}</p>
        <button
          type="button"
          className={styles.assignmentSaveButton}
          onClick={() => window.location.reload()}
        >
          다시 불러오기
        </button>
      </div>
    );
  }

  return (
    <div className={styles.pageCard}>
      <h2 className={styles.pageTitle}>고객 관리</h2>

      {counselors.length === 0 && (
        <p role="status">
          배정 가능한 활성 상담사가 없습니다. 상담사 계정을 확인해 주세요.
        </p>
      )}

      <div className={styles.customerTableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>고객명</th>
              <th>연락처</th>
              <th>관심 과정</th>
              <th>고객 상태</th>
              <th>현재 담당자</th>
              <th>담당자 변경</th>
              <th>저장</th>
            </tr>
          </thead>

          <tbody>
            {customers.length === 0 ? (
              <tr>
                <td colSpan={7} className={styles.customerEmpty}>
                  조회된 고객이 없습니다.
                </td>
              </tr>
            ) : (
              customers.map((customer) => {
                const selectedValue =
                  changedCounselors[customer.customerId] ??
                  String(customer.counselorId ?? "");

                const hasChanges =
                  changedCounselors[customer.customerId] !== undefined;

                const isSaving =
                  savingCustomerId === customer.customerId;

                const currentCounselorIsUnavailable =
                  customer.counselorId !== null &&
                  !counselors.some(
                    (counselor) =>
                      counselor.userId === customer.counselorId,
                  );

                return (
                  <tr key={customer.customerId}>
                    <td>{customer.name}</td>
                    <td>{customer.phone}</td>

                    <td>
                      {getCodeName(
                        codes,
                        "INTEREST_COURSE",
                        customer.interestCourse,
                      )}
                    </td>

                    <td>
                      {getCodeName(
                        codes,
                        "CUSTOMER_STATUS",
                        customer.status,
                      )}
                    </td>

                    <td>{customer.counselorName || "미지정"}</td>

                    <td>
                      <select
                        className={styles.statusSelect}
                        aria-label={`${customer.name} 고객 담당 상담사`}
                        value={selectedValue}
                        disabled={
                          savingCustomerId !== null ||
                          counselors.length === 0
                        }
                        onChange={(event) =>
                          handleCounselorChange(
                            customer,
                            event.target.value,
                          )
                        }
                      >
                        <option value="" disabled>
                          상담사 선택
                        </option>

                        {currentCounselorIsUnavailable && (
                          <option
                            value={String(customer.counselorId)}
                            disabled
                          >
                            {customer.counselorName || "기존 담당자"}
                            {" (현재 배정 불가)"}
                          </option>
                        )}

                        {counselors.map((counselor) => (
                          <option
                            key={counselor.userId}
                            value={String(counselor.userId)}
                          >
                            {counselor.name} ({counselor.email})
                          </option>
                        ))}
                      </select>
                    </td>

                    <td>
                      <button
                        type="button"
                        className={styles.assignmentSaveButton}
                        disabled={
                          !hasChanges ||
                          !selectedValue ||
                          savingCustomerId !== null
                        }
                        onClick={() => void handleSaveCounselor(customer)}
                      >
                        {isSaving ? "저장 중..." : "저장"}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {feedback && (
        <FeedbackModal
          type={feedback.type}
          title={feedback.title}
          message={feedback.message}
          onClose={() => setFeedback(null)}
        />
      )}
    </div>
  );
}