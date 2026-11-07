"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { isCancel } from "axios";
import { getConsDashboard } from "@/lib/api/consultant.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import type { CustomerEntry } from "@/features/customers/types";
import styles from "./Dashboard.module.css";

type Dashboard = { totalCustomers: number; statusCounts: { code: string; name: string; count: number }[]; recentCustomers: CustomerEntry[] };
export default function ConsultantDashboard() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const { codes } = useCodeStore();
  useEffect(() => {
    const controller = new AbortController();
    getConsDashboard(controller.signal).then(response => {
      if (!response.success) throw new Error("조회 실패");
      if (!controller.signal.aborted) { setData(response.data); setError(false); }
    }).catch(cause => { if (!controller.signal.aborted && !isCancel(cause)) setError(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [attempt]);
  const refresh = () => { setLoading(true); setError(false); setAttempt(value => value + 1); };
  const name = (group: string, value: string) => getCodeName(codes, group, value) || value;
  return <div className={styles.dashboard} aria-busy={loading}>
    <header className={styles.header}><div><h1>상담사 대시보드</h1><p>본인 담당 고객의 상태와 최근 상담을 확인합니다.</p></div><div className={styles.actions}><button type="button" disabled={loading} onClick={refresh}>새로고침</button><Link className={styles.primary} href="/consultant/customers">고객 목록 이동</Link></div></header>
    {loading ? <p role="status">담당 고객 현황을 불러오는 중입니다.</p> : error ? <div role="alert"><p>대시보드를 불러오지 못했습니다. 다시 시도해 주세요.</p><button type="button" onClick={refresh}>다시 조회</button></div> : data && <>
      <section className={styles.overview} aria-labelledby="assigned-summary"><div className={styles.total}><h2 id="assigned-summary">전체 담당 고객</h2><strong>{data.totalCustomers}<small>명</small></strong></div>
        <div className={styles.statuses}>{data.statusCounts.map(status => <Link key={status.code} className={styles.statusCell} href={`/consultant/customers?status=${encodeURIComponent(status.code)}`} aria-label={`${status.name} 고객 ${status.count}명 보기`}><span>{status.name}</span><strong>{status.count}<small>명</small></strong></Link>)}</div>
      </section>
      <section className={styles.recent} aria-labelledby="recent-customers"><div className={styles.recentHeader}><div><h2 id="recent-customers">최근 상담 고객</h2><p>현재 담당 고객 중 최근 상담일 기준 최신 5명입니다.</p></div><Link href="/consultant/customers">전체 고객 보기</Link></div>
        {data.recentCustomers.length === 0 ? <p className={styles.empty}>최근 상담일이 등록된 담당 고객이 없습니다.</p> : <div className={styles.scroll} role="region" aria-label="고객 목록 표" tabIndex={0}><table><thead><tr><th>고객명</th><th>연락처</th><th>관심 과정</th><th>상태</th><th>최근 상담일</th><th>상담 기록</th></tr></thead><tbody>{data.recentCustomers.map(customer => <tr key={customer.customerId}><td><strong>{customer.name}</strong></td><td>{customer.phone}</td><td>{name("INTEREST_COURSE", customer.interestCourse)}</td><td>{name("CUSTOMER_STATUS", customer.status)}</td><td>{customer.lastActivityAt?.slice(0,16).replace("T"," ") || "-"}</td><td><Link href={`/consultant/customers/${customer.customerId}`}>{customer.name} 상담 기록</Link></td></tr>)}</tbody></table></div>}
      </section>
    </>}
  </div>;
}
