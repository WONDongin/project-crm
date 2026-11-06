// 상담사 고객 상세 정보 화면
"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { isAxiosError } from "axios";
import { getConsCustomerDetail } from "@/lib/api/consultant.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import type { CustomerDetail as CustomerDetailData } from "@/types/customerDetail";
import CustomerHistory from "./CustomerHistory";
import styles from "./CustomerDetail.module.css";

type DetailState =
  | { status: "loading" }
  | { status: "ready"; customer: CustomerDetailData }
  | { status: "error"; message: string; canRetry: boolean };

const dateTime = (value: string | null) =>
  value ? value.slice(0, 16).replace("T", " ") : "-";

export default function CustomerDetail({ customerId }: { customerId: string }) {
  const [state, setState] = useState<DetailState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  const { codes } = useCodeStore();
  const validId = /^\d+$/.test(customerId) &&
    Number.isSafeInteger(Number(customerId)) && Number(customerId) > 0;

  useEffect(() => {
    if (!validId) return;
    const controller = new AbortController();

    async function loadCustomer() {
      try {
        const response = await getConsCustomerDetail(customerId, controller.signal);
        if (!response.success || !response.data) {
          throw new Error("고객 정보를 불러오지 못했습니다.");
        }
        if (!controller.signal.aborted) {
          setState({ status: "ready", customer: response.data });
        }
      } catch (error) {
        if (controller.signal.aborted) return;
        const status = isAxiosError(error) ? error.response?.status : undefined;
        const restricted = status === 403 || status === 404;
        setState({
          status: "error",
          message: restricted
            ? "조회할 수 없는 고객입니다. 담당 고객 목록을 확인해 주세요."
            : "고객 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
          canRetry: !restricted && status !== 401,
        });
      }
    }

    loadCustomer();
    return () => controller.abort();
  }, [customerId, validId, attempt]);

  const handleHistoryRestricted = useCallback(() => {
    setState({ status: "error", canRetry: false,
      message: "조회할 수 없는 고객입니다. 담당 고객 목록을 확인해 주세요." });
  }, []);

  const codeName = (group: string, value: string | null) =>
    value ? getCodeName(codes, group, value) : "-";

  const customer = state.status === "ready" ? state.customer : null;
  const application = customer?.application;

  return (
    <div className={styles.detail}>
      <Link href="/consultant/customers" className={styles.backLink}>
        고객 목록으로
      </Link>
      <header className={styles.header}>
        <div>
          <h1>{customer ? `${customer.name} 고객` : "고객 상세"}</h1>
          <p>고객 기본 정보와 상담 신청·이력</p>
        </div>
        {customer && (
          <span className={styles.status}>
            {codeName("CUSTOMER_STATUS", customer.status)}
          </span>
        )}
      </header>

      {!validId ? (
        <p role="alert" className={styles.message}>올바른 고객 번호가 아닙니다.</p>
      ) : state.status === "error" ? (
        <div role="alert" className={styles.message}>
          <p>{state.message}</p>
          {state.canRetry && (
            <button type="button" onClick={() => {
              setState({ status: "loading" });
              setAttempt((value) => value + 1);
            }} className={styles.retryButton}>
              다시 시도
            </button>
          )}
        </div>
      ) : (
        <div className={styles.sections} aria-busy={state.status === "loading"}>
          {state.status === "loading" && (
            <p role="status" className={styles.loading}>고객 정보를 불러오는 중입니다.</p>
          )}
          <section className={styles.section} aria-labelledby="customer-information">
            <h2 id="customer-information">기본 정보</h2>
            <dl className={styles.fields}>
              {[
                ["연락처", customer?.phone],
                ["생년월일", customer?.birthDate],
                ["이메일", customer?.email],
                ["관심 과정", customer && codeName("INTEREST_COURSE", customer.interestCourse)],
                ["유입 경로", customer && codeName("LEAD_SOURCE", customer.leadSource)],
                ["고객 태그", customer && codeName("CUSTOMER_TAG", customer.customerTag)],
                ["담당 상담사", customer?.counselorName],
                ["최근 상담일", customer && dateTime(customer.lastActivityAt)],
                ["고객 등록일", customer && dateTime(customer.createdAt)],
              ].map(([label, value]) => (
                <div key={label} className={styles.field}>
                  <dt>{label}</dt>
                  <dd>{state.status === "loading" ? (
                    <span className={styles.skeleton} aria-hidden="true" />
                  ) : value || "-"}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className={styles.section} aria-labelledby="application-information">
            <h2 id="application-information">최근 상담 신청</h2>
            {state.status === "loading" ? (
              <div className={styles.applicationLoading} aria-hidden="true">
                <span className={styles.skeleton} />
                <span className={styles.skeleton} />
              </div>
            ) : application ? (
              <>
                <dl className={styles.applicationFields}>
                  <div className={styles.field}>
                    <dt>신청 접수일</dt>
                    <dd>{dateTime(application.receivedAt)}</dd>
                  </div>
                  <div className={styles.field}>
                    <dt>희망 상담 시간</dt>
                    <dd>{codeName("CONSULT_TIME", application.preferredTime)}</dd>
                  </div>
                </dl>
                <h3 className={styles.contentTitle}>신청 내용</h3>
                <p className={styles.content}>{application.content || "입력된 신청 내용이 없습니다."}</p>
              </>
            ) : (
              <p className={styles.empty}>등록된 상담 신청 정보가 없습니다.</p>
            )}
          </section>
          {state.status === "ready" && <CustomerHistory key={customerId} customerId={customerId}
            onRestricted={handleHistoryRestricted} onCreated={() => setAttempt((value) => value + 1)} />}
        </div>
      )}
    </div>
  );
}

