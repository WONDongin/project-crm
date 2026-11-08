"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { isCancel } from "axios";
import { getAdminDashboard } from "@/lib/api/admin.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import styles from "./Dashboard.module.css";

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [updatedAt, setUpdatedAt] = useState("");
  const { codes } = useCodeStore();
  useEffect(() => {
    const controller = new AbortController();
    getAdminDashboard(controller.signal).then(response => {
      if (!response.success) throw new Error("조회 실패");
      if (!controller.signal.aborted) {
        setData(response.data); setError("");
        setUpdatedAt(new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }));
      }
    }).catch(cause => {
      if (!controller.signal.aborted && !isCancel(cause)) setError(cause.response?.status === 403 ? "관리자 접근 권한을 확인해 주세요." : "운영 현황을 불러오지 못했습니다.");
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [attempt]);
  const refresh = () => { setLoading(true); setError(""); setAttempt(value => value + 1); };
  const name = (group, value) => getCodeName(codes, group, value) || value;
  const metrics = [
    ["전체 고객", data?.totalCustomers, "명", "등록된 전체 고객", "/admin/customers"],
    ["미배정 고객", data?.unassignedCustomers, "명", "담당 상담사 배정 필요", "/admin/customers?counselor=unassigned"],
    ["활성 상담사", data?.activeCounselors, "명", "활성 상태의 상담사 계정", "/admin/users"],
    ["오늘 상담", data?.todayConsultations, "건", "신청 접수 제외 · 등록일 기준", null],
  ];
  return <div className={styles.dashboard} aria-busy={loading}>
    <header className={styles.header}><div><h1>관리자 대시보드</h1><p>고객 현황을 확인하고 담당 상담사를 배정하세요.</p></div><div className={styles.actions}><span aria-live="polite">{loading ? "조회 중" : updatedAt && `${updatedAt} 업데이트`}</span><button type="button" onClick={refresh} disabled={loading}>새로고침</button><Link className={styles.primary} href="/admin/customers?counselor=unassigned">담당자 배정하기</Link></div></header>
    {error && <div className={styles.notice} role="alert"><p>{error}{data && " 이전 조회 결과를 표시하고 있습니다."}</p><button type="button" onClick={refresh} disabled={loading}>다시 조회</button></div>}
    <section className={styles.metrics} aria-label="운영 요약">{metrics.map(([label, value, unit, description, href]) => <div className={styles.metric} key={label}><h2>{label}</h2><div className={styles.value}>{value == null ? loading ? <span className={styles.skeleton} aria-label="불러오는 중" /> : <span className={styles.missing} aria-label="조회 불가">—</span> : <>{value.toLocaleString("ko-KR")}<small>{unit}</small></>}</div><p>{description}</p>{href && <Link href={href}>목록 보기 →</Link>}</div>)}</section>
    <div className={styles.columns}>
      <section className={styles.panel} aria-labelledby="status-title"><div className={styles.sectionHeader}><h2 id="status-title">고객 상태별 현황</h2><span>전체 고객 기준</span></div>
        {!data ? <p className={styles.empty}>{loading ? "고객 현황을 불러오는 중입니다." : "고객 현황을 조회할 수 없습니다."}</p> : data.statusCounts.length === 0 ? <p className={styles.empty}>등록된 고객 상태가 없습니다.</p> : <ul className={styles.statuses}>{data.statusCounts.map(status => <li key={status.code}><Link href={`/admin/customers?status=${encodeURIComponent(status.code)}`}><span>{status.name}</span><strong>{status.count.toLocaleString("ko-KR")}<small>명</small></strong><span className={styles.track} aria-hidden="true"><span style={{ width: `${data.totalCustomers ? status.count / data.totalCustomers * 100 : 0}%` }} /></span></Link></li>)}</ul>}
      </section>
      <section className={styles.panel} aria-labelledby="unassigned-title"><div className={styles.sectionHeader}><div><h2 id="unassigned-title">최근 미배정 고객</h2><p>등록일 기준 최신 5명 · 고객 관리에서 담당자를 배정하세요.</p></div><Link href="/admin/customers?counselor=unassigned">전체 보기 →</Link></div>
        {!data ? <p className={styles.empty}>{loading ? "미배정 고객을 불러오는 중입니다." : "미배정 고객을 조회할 수 없습니다."}</p> : data.recentUnassignedCustomers.length === 0 ? <p className={styles.empty}>담당자 배정이 필요한 고객이 없습니다.</p> : <div className={styles.tableScroll} role="region" aria-label="최근 미배정 고객 목록" tabIndex={0}><table><thead><tr><th>고객명 / 연락처</th><th>관심 과정 / 상태</th><th>등록일</th></tr></thead><tbody>{data.recentUnassignedCustomers.map(customer => <tr key={customer.customerId}><td><strong>{customer.name}</strong><span>{customer.phone}</span></td><td>{name("INTEREST_COURSE", customer.interestCourse)}<span>{name("CUSTOMER_STATUS", customer.status)}</span></td><td>{customer.createdAt?.slice(0,16).replace("T"," ") || "-"}</td></tr>)}</tbody></table></div>}
      </section>
    </div>
    {data && <p className={styles.footnote}>오늘 상담: {data.consultationDate} 00:00~24:00 (한국 시간)에 등록된 상담 기록입니다.</p>}
  </div>;
}
