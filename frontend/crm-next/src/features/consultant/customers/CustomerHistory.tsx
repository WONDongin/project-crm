"use client";

import { useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { getConsCustomerHistory } from "@/lib/api/consultant.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import type { ActivityLogPage } from "@/types/activityLog";
import styles from "./CustomerDetail.module.css";

type HistoryState =
  | { status: "loading" }
  | { status: "ready"; data: ActivityLogPage }
  | { status: "error"; canRetry: boolean };

const dateTime = (value: string | null) =>
  value ? value.slice(0, 16).replace("T", " ") : "-";

export default function CustomerHistory({ customerId, onRestricted }: {
  customerId: string;
  onRestricted: () => void;
}) {
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState<HistoryState>({ status: "loading" });
  const { codes } = useCodeStore();

  useEffect(() => {
    const controller = new AbortController();
    async function loadHistory() {
      try {
        const response = await getConsCustomerHistory(customerId, page, controller.signal);
        if (!response.success || !response.data) throw new Error("상담 기록 조회 실패");
        if (!controller.signal.aborted) setState({ status: "ready", data: response.data });
      } catch (error) {
        if (controller.signal.aborted) return;
        const status = isAxiosError(error) ? error.response?.status : undefined;
        if (status === 403 || status === 404) {
          onRestricted();
          return;
        }
        setState({ status: "error", canRetry: status !== 401 });
      }
    }
    loadHistory();
    return () => controller.abort();
  }, [customerId, page, attempt, onRestricted]);

  const codeName = (group: string, value: string | null) =>
    value ? getCodeName(codes, group, value) : "-";
  const data = state.status === "ready" ? state.data : null;
  const movePage = (next: number) => {
    setState({ status: "loading" });
    setPage(next);
  };

  return (
    <section className={`${styles.section} ${styles.history}`} aria-labelledby="consultation-history"
      aria-busy={state.status === "loading"}>
      <div className={styles.historyHeading}>
        <h2 id="consultation-history">상담 기록</h2>
        <span>{data ? `전체 ${data.totalElements}건 · 최신순` : "최신순 · 5건씩 조회"}</span>
      </div>
      {state.status === "loading" ? (
        <div role="status" className={styles.applicationLoading}>
          <span>상담 기록을 불러오는 중입니다.</span>
          <span className={styles.skeleton} aria-hidden="true" />
          <span className={styles.skeleton} aria-hidden="true" />
        </div>
      ) : state.status === "error" ? (
        <div role="alert" className={styles.message}>
          <p>상담 기록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
          {state.canRetry && <button type="button" className={styles.retryButton} onClick={() => {
            setState({ status: "loading" });
            setAttempt((value) => value + 1);
          }}>다시 시도</button>}
        </div>
      ) : data && (
        <>
          {data.content.length === 0 ? (
            <p className={styles.empty}>{data.totalElements === 0
              ? "등록된 상담 기록이 없습니다." : "이 페이지에 상담 기록이 없습니다."}</p>
          ) : (
            <ol className={styles.historyList}>
              {data.content.map((record) => {
                const receipt = record.createdBy === "SYSTEM" && record.activityResult === "RECEIVED";
                return (
                  <li key={record.activityId} className={styles.historyEntry}>
                    <div className={styles.historyHeading}>
                      <h3>{receipt ? "신청 접수" : "상담 이력"}</h3>
                      <time dateTime={record.createdAt || undefined}>{dateTime(record.createdAt)}</time>
                    </div>
                    <dl className={styles.historyFields}>
                      <div className={styles.field}><dt>작성자</dt><dd>{record.createdBy === "SYSTEM"
                        ? "시스템" : record.counselorName || "-"}</dd></div>
                      <div className={styles.field}><dt>상담 유형</dt><dd>{codeName("CONSULT_TYPE", record.consultType)}</dd></div>
                      <div className={styles.field}><dt>상담 결과</dt><dd>{codeName("ACTIVITY_RESULT", record.activityResult)}</dd></div>
                      {receipt && <div className={styles.field}><dt>희망 상담 시간</dt>
                        <dd>{codeName("CONSULT_TIME", record.preferredTime)}</dd></div>}
                    </dl>
                    <h4 className={styles.contentTitle}>{receipt ? "신청 내용" : "상담 내용"}</h4>
                    <p className={styles.content}>{record.content || "입력된 내용이 없습니다."}</p>
                    {record.nextAction && <><h4 className={styles.contentTitle}>다음 액션</h4>
                      <p className={styles.content}>{record.nextAction}</p></>}
                  </li>
                );
              })}
            </ol>
          )}
          {data.totalElements > 0 && <nav className={styles.historyPagination} aria-label="상담 기록 페이지">
            <button type="button" disabled={page === 0} onClick={() => movePage(page - 1)}>이전</button>
            <span aria-live="polite">{data.number + 1} / {data.totalPages} 페이지</span>
            <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => movePage(page + 1)}>다음</button>
          </nav>}
          {page > 0 && data.content.length === 0 && <button type="button" className={styles.retryButton}
            onClick={() => movePage(0)}>첫 페이지로</button>}
        </>
      )}
    </section>
  );
}
