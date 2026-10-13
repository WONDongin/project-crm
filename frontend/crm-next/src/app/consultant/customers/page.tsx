// 상담사 고객 목록 페이지
"use client";

import { useEffect, useState } from "react";

import useCodeStore from "@/stores/codeStore";
import FeedbackModal from "@/components/common/FeedbackModal";

import { CommonCode } from "@/types/commonCode";
import { ConsCustomer } from "@/types/conscustomer";

import { getCodeName } from "@/utils/codeUtil";

import {
  getConscustomers,
  updateConsCustomer,
} from "@/lib/api/consultant.api";

import styles from "../ConsultantLayout.module.css";

export default function CustomersPage() {
  const [customers, setCustomers] = useState<ConsCustomer[]>([]);

  // 고객별 변경 상태 저장
  const [changedStatuses, setChangedStatuses] = useState<
    Record<number, string>
  >({});
  const [changedTags, setChangedTags] = useState<Record<number, string>>({});
  const [savingCustomers, setSavingCustomers] = useState<
    Record<number, boolean>
  >({});
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    title: string;
    message: string;
  } | null>(null);

  const { codes, hydrated } = useCodeStore();

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const res = await getConscustomers();

        console.log("상담사 고객 API 응답 =", res);

        if (res.success) {
          setCustomers(res.data);
        }
      } catch (error) {
        console.error("상담사 고객 목록 조회 에러:", error);
      }
    };

    fetchCustomers();
  }, []);

  // 상태 변경 처리
  const handleStatusChange = (
    customerId: number,
    newStatus: string,
    originalStatus: string,
  ) => {
    setChangedStatuses((prev) => {
      const next = { ...prev };

      // 기존 상태와 같으면 변경 목록에서 제거
      if (newStatus === originalStatus) {
        delete next[customerId];
      } else {
        next[customerId] = newStatus;
      }

      return next;
    });
  };

  const handleTagChange = (
    customerId: number,
    newTag: string,
    originalTag: string | null,
  ) => {
    setChangedTags((prev) => {
      const next = { ...prev };

      if (newTag === (originalTag ?? "")) {
        delete next[customerId];
      } else {
        next[customerId] = newTag;
      }

      return next;
    });
  };

  const handleSaveCustomer = async (customer: ConsCustomer) => {
    const { customerId } = customer;
    const status = changedStatuses[customerId];
    const hasTagChange = changedTags[customerId] !== undefined;
    const customerTag = changedTags[customerId];

    if ((!status && !hasTagChange) || savingCustomers[customerId]) {
      return;
    }

    const updates: { status?: string; customerTag?: string } = {};
    if (status) {
      updates.status = status;
    }
    if (hasTagChange) {
      updates.customerTag = customerTag;
    }

    setSavingCustomers((prev) => ({ ...prev, [customerId]: true }));

    try {
      const response = await updateConsCustomer(customerId, updates);

      if (!response.success) {
        throw new Error(response.message || "고객 정보 저장에 실패했습니다.");
      }

      setCustomers((prev) =>
        prev.map((customer) =>
          customer.customerId === customerId
            ? {
                ...customer,
                ...(status ? { status } : {}),
                ...(hasTagChange
                  ? { customerTag: customerTag || null }
                  : {}),
              }
            : customer,
        ),
      );
      setChangedStatuses((prev) => {
        const next = { ...prev };
        delete next[customerId];
        return next;
      });
      setChangedTags((prev) => {
        const next = { ...prev };
        delete next[customerId];
        return next;
      });
      setFeedback({
        type: "success",
        title: "저장 완료",
        message: response.message || "변경사항이 정상적으로 저장되었습니다.",
      });
    } catch (error) {
      console.error("고객 정보 저장 에러:", error);

      const message =
        error instanceof Error
          ? error.message
          : "알 수 없는 오류가 발생했습니다.";
      setFeedback({
        type: "error",
        title: "저장 실패",
        message: `고객 정보 저장에 실패했습니다. ${message}`,
      });
    } finally {
      setSavingCustomers((prev) => {
        const next = { ...prev };
        delete next[customerId];
        return next;
      });
    }
  };

  // 공통코드 로딩 전
  if (!hydrated) {
    return <div>공통코드 로딩중...</div>;
  }

  return (
    <div className={styles.pageCard}>
      <h2 className={styles.pageTitle}>고객 관리</h2>

      <table className={styles.table}>
        <thead>
          <tr>
            <th>고객명</th>
            <th>연락처</th>
            <th>관심 과정</th>
            <th>고객 상태</th>
            <th>고객 태그</th>
            <th>최근 상담일</th>
            <th>유입 경로</th>
            <th>상담 기록</th>
            <th>저장</th>
          </tr>
        </thead>

        <tbody>
          {customers.length === 0 ? (
            <tr>
              <td
                colSpan={9}
                style={{
                  textAlign: "center",
                  padding: "20px",
                }}
              >
                조회된 고객이 없습니다.
              </td>
            </tr>
          ) : (
            customers.map((customer) => (
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

                {/* 고객 상태 변경 */}
                <td>
                  <select
                    className={styles.statusSelect}
                    value={
                      changedStatuses[customer.customerId] ?? customer.status
                    }
                    onChange={(e) =>
                      handleStatusChange(
                        customer.customerId,
                        e.target.value,
                        customer.status,
                      )
                    }
                  >
                    {codes.CUSTOMER_STATUS?.map((status: CommonCode) => (
                      <option key={status.code} value={status.code}>
                        {status.name}
                      </option>
                    ))}
                  </select>
                </td>

                <td>
                  <div className={styles.tagEditor}>
                    <select
                      className={styles.statusSelect}
                      value={
                        changedTags[customer.customerId] ??
                        customer.customerTag ??
                        ""
                      }
                      onChange={(e) =>
                        handleTagChange(
                          customer.customerId,
                          e.target.value,
                          customer.customerTag,
                        )
                      }
                    >
                      <option value="">태그 선택</option>
                      {codes.CUSTOMER_TAG?.map((tag: CommonCode) => (
                        <option key={tag.code} value={tag.code}>
                          {tag.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </td>

                <td>
                  {customer.lastActivityAt
                    ? customer.lastActivityAt.slice(0, 10)
                    : "-"}
                </td>

                <td>
                  {getCodeName(codes, "LEAD_SOURCE", customer.leadSource)}
                </td>

                {/* 상담 기록 */}
                <td>
                  <button className={styles.detailButton}>상담 기록</button>
                </td>

                {/* 저장 버튼 */}
                <td>
                  <button
                    className={styles.saveButton}
                    disabled={
                      (!changedStatuses[customer.customerId] &&
                        changedTags[customer.customerId] === undefined) ||
                      savingCustomers[customer.customerId]
                    }
                    onClick={() => handleSaveCustomer(customer)}
                  >
                    {savingCustomers[customer.customerId] ? "저장 중..." : "저장"}
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
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
