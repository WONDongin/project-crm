// 상담사 고객 목록 페이지
"use client";

import { useEffect, useState } from "react";

import useCodeStore from "@/stores/codeStore";

import { getCodeName } from "@/utils/codeUtil";

import { ConsCustomer } from "@/types/conscustomer";

import { getConscustomers } from "@/lib/api/consultant.api";

import styles from "../ConsultantLayout.module.css";

export default function CustomersPage() {
  const [customers, setCustomers] = useState<ConsCustomer[]>([]);

  // 고객별 변경 상태 저장
  const [changedStatuses, setChangedStatuses] = useState<
    Record<number, string>
  >({});

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
  const handleStatusChange = (customerId: number, newStatus: string) => {
    setChangedStatuses((prev) => ({
      ...prev,
      [customerId]: newStatus,
    }));
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
            <th>상세보기</th>
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

                {/* 상태 변경 select */}
                <td>
                  <select
                    className={styles.statusSelect}
                    value={
                      changedStatuses[customer.customerId] ?? customer.status
                    }
                    onChange={(e) =>
                      handleStatusChange(customer.customerId, e.target.value)
                    }
                  >
                    <option value="NEW">신규</option>

                    <option value="IN_PROGRESS">수강</option>

                    <option value="DORMANT">미활동</option>

                    <option value="DROP">탈퇴</option>
                  </select>
                </td>

                <td>
                  {getCodeName(codes, "CUSTOMER_TAG", customer.customerTag)}
                </td>

                <td>
                  {customer.lastActivityAt
                    ? customer.lastActivityAt.slice(0, 10)
                    : "-"}
                </td>

                <td>
                  {getCodeName(codes, "LEAD_SOURCE", customer.leadSource)}
                </td>

                {/* 상세보기 */}
                <td>
                  <button className={styles.detailButton}>상세보기</button>
                </td>

                {/* 저장 버튼 */}
                <td>
                  <button
                    className={styles.saveButton}
                    disabled={!changedStatuses[customer.customerId]}
                  >
                    저장
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
