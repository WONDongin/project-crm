// 고객 관리 페이지
"use client";

import { Customer } from "@/types/customer";
import { useEffect, useState } from "react";
import { getCustomers } from "@/lib/api/admin.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import styles from "../AdminLayout.module.css";

export default function AdminCustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const { codes, hydrated } = useCodeStore();

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const res = await getCustomers();

        console.log("고객 API 응답 =", res);

        if (res.success) {
          setCustomers(res.data);
        }
      } catch (error) {
        console.error("고객 목록 조회 에러:", error);
      }
    };

    fetchCustomers();
  }, []);

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
            <th>현재 담당자</th>
            <th>담당자 변경</th>
            <th>저장</th>
          </tr>
        </thead>

        <tbody>
          {customers.map((customer) => (
            <tr key={customer.customerId}>
              <td>{customer.name}</td>

              <td>{customer.phone}</td>

              <td>
                {getCodeName(codes, "INTEREST_COURSE", customer.interestCourse)}
              </td>

              <td>{getCodeName(codes, "CUSTOMER_STATUS", customer.status)}</td>

              <td>{customer.counselorName ?? "미지정"}</td>

              <td>
                <select className={styles.statusSelect}>
                  <option value="">선택</option>
                </select>
              </td>

              <td>
                <button>저장</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
