"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { isAxiosError, isCancel } from "axios";
import { getCustomers, getAssignableCounselors, assignCustomerCounselor, unassignCustomerCounselor } from "@/lib/api/admin.api";
import { getConscustomers, updateConsCustomer } from "@/lib/api/consultant.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import type { CustomerEntry, CustomerPage, Query } from "./types";
import adminStyles from "./Customers.module.css";
import consultantStyles from "./ConsultantCustomers.module.css";

type Counselor = { userId: number; name: string; email: string };
type Draft = { status?: string; customerTag?: string; counselorId?: string };
const empty: CustomerPage = { content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 };
function errorMessage(cause: unknown) {
  if (isAxiosError<{ message?: string; detail?: string }>(cause)) return cause.response?.data?.message || cause.response?.data?.detail || "요청을 처리하지 못했습니다. 다시 시도해 주세요.";
  return "요청을 처리하지 못했습니다. 다시 시도해 주세요.";
}
export default function CustomerList({ admin, initialStatus = "", initialCounselor = "" }: { admin: boolean; initialStatus?: string; initialCounselor?: string }) {
  const styles = admin ? adminStyles : consultantStyles;
  const initial: Query = { keyword: "", status: initialStatus, interestCourse: "", counselor: initialCounselor, page: 0 };
  const [query, setQuery] = useState(initial);
  const [filters, setFilters] = useState(initial);
  const [data, setData] = useState<CustomerPage>(empty);
  const [counselors, setCounselors] = useState<Counselor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [busy, setBusy] = useState<number | null>(null);
  const running = useRef(false);
  const [releaseId, setReleaseId] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ error: boolean; text: string } | null>(null);
  const { codes, hydrated } = useCodeStore();
  const name = (group: string, value: string | null) => value ? getCodeName(codes, group, value) || value : "-";
  const options = (group: string) => (codes[group] || []) as { code: string; name: string }[];

  useEffect(() => {
    const controller = new AbortController();
    const params = { keyword: query.keyword, status: query.status, interestCourse: query.interestCourse, page: query.page, size: 10,
      ...(admin && query.counselor ? query.counselor === "unassigned" ? { unassigned: true } : { counselorId: Number(query.counselor) } : {}) };
    Promise.all([
      admin ? getCustomers(params, controller.signal) : getConscustomers(params, controller.signal),
      admin ? getAssignableCounselors(controller.signal) : Promise.resolve({ success: true, data: [] }),
    ]).then(([response, staff]) => {
      if (controller.signal.aborted) return;
      if (!response.success || !staff.success) throw new Error("조회 실패");
      const page = response.data as CustomerPage;
      const last = Math.max(0, page.totalPages - 1);
      if (query.page > last) { setQuery(previous => ({ ...previous, page: last })); return; }
      setData(page); setCounselors(staff.data); setError(""); setLoading(false);
    }).catch(cause => {
      if (controller.signal.aborted || isCancel(cause)) return;
      setError("고객 목록을 불러오지 못했습니다. 검색 조건과 연결 상태를 확인한 후 다시 조회해 주세요."); setLoading(false);
    });
    return () => controller.abort();
  }, [admin, query, reload]);

  function changeQuery(next: Query, resetFilters = false) {
    if (running.current) return;
    if (Object.keys(drafts).length && !window.confirm("저장하지 않은 변경사항이 사라집니다. 계속할까요?")) return;
    if (resetFilters) setFilters(next);
    setDrafts({}); setReleaseId(null); setNotice(null); setError(""); setLoading(true); setQuery(next);
    // 같은 조건으로도 다시 검색할 수 있도록 갱신한다.
    setReload(value => value + 1);
  }
  function search(event: FormEvent<HTMLFormElement>) { event.preventDefault(); changeQuery({ ...filters, keyword: filters.keyword.trim(), page: 0 }); }
  function field(row: CustomerEntry, key: keyof Draft, value: string) {
    const original = key === "counselorId" ? String(row.counselorId ?? "") : key === "customerTag" ? row.customerTag || "" : row.status;
    setDrafts(previous => {
      const next = { ...previous }; const draft = { ...next[row.customerId] };
      if (value === original) delete draft[key]; else draft[key] = value;
      if (Object.keys(draft).length) next[row.customerId] = draft; else delete next[row.customerId];
      return next;
    });
  }
  async function save(row: CustomerEntry, release = false) {
    if (running.current) return;
    const draft = drafts[row.customerId];
    if (!release && !draft) return;
    running.current = true; setBusy(row.customerId); setNotice(null);
    try {
      const response = admin ? release ? await unassignCustomerCounselor(row.customerId)
        : await assignCustomerCounselor(row.customerId, Number(draft!.counselorId))
        : await updateConsCustomer(row.customerId, draft!);
      if (!response.success) throw new Error("저장 실패");
      setDrafts(previous => { const next = { ...previous }; delete next[row.customerId]; return next; });
      setReleaseId(null); setNotice({ error: false, text: release ? "담당자 배정을 해제했습니다. 고객 상태와 상담 기록은 유지합니다." : "변경사항이 저장되었습니다." });
      setLoading(true); setReload(value => value + 1);
    } catch (cause) {
      setNotice({ error: true, text: errorMessage(cause) });
      if (isAxiosError(cause) && [403, 404].includes(cause.response?.status || 0)) {
        setDrafts(previous => { const next = { ...previous }; delete next[row.customerId]; return next; });
        setReleaseId(null); setLoading(true); setReload(value => value + 1);
      }
    } finally { running.current = false; setBusy(null); }
  }
  return <section className={styles.page} aria-busy={loading}>
    <header className={styles.header}><div><h1>고객 관리</h1><p>{admin ? "전체 고객을 조회하고 담당 상담사를 배정합니다." : "본인 담당 고객을 검색하고 상태와 상담 기록을 관리합니다."}</p></div>
      {!admin && <Link href="/consultant/dashboard">대시보드로</Link>}</header>
    <div className={styles.panel}><form className={styles.filters} aria-label="고객 검색 조건" onSubmit={search}>
      <fieldset disabled={busy !== null || loading}>
        <label className={styles.keyword}>이름·연락처<input aria-label="이름·연락처 검색" placeholder="고객명 또는 연락처" maxLength={100} value={filters.keyword} onChange={event => setFilters({ ...filters, keyword: event.target.value })} /></label>
        <label>상태<select aria-label="상태 필터" value={filters.status} onChange={event => setFilters({ ...filters, status: event.target.value })}><option value="">전체 상태</option>
          {filters.status && !options("CUSTOMER_STATUS").some(code => code.code === filters.status) && <option value={filters.status}>{filters.status}</option>}
          {options("CUSTOMER_STATUS").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}</select></label>
        <label>관심 과정<select aria-label="관심 과정 필터" value={filters.interestCourse} onChange={event => setFilters({ ...filters, interestCourse: event.target.value })}><option value="">전체 과정</option>{options("INTEREST_COURSE").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}</select></label>
        {admin ? <label>담당자<select aria-label="담당자 필터" value={filters.counselor} onChange={event => setFilters({ ...filters, counselor: event.target.value })}><option value="">전체 담당자</option><option value="unassigned">미배정</option>{counselors.map(staff => <option key={staff.userId} value={staff.userId}>{staff.name} ({staff.email})</option>)}</select></label>
          : null}
        <div className={styles.actions}><button className={styles.primary} type="submit">검색</button><button type="button" onClick={() => changeQuery({ ...initial, status: "", counselor: "" }, true)}>초기화</button></div>
      </fieldset>
    </form>
    {notice && <p className={styles.notice} role={notice.error ? "alert" : "status"}>{notice.text}</p>}
    {Object.keys(drafts).length > 0 && <p className={styles.muted}>저장하지 않은 변경사항이 있습니다.</p>}
    {loading ? <p role="status">고객 목록을 불러오는 중입니다.</p> : error ? <div role="alert" className={styles.notice}><p>{error}</p><button type="button" onClick={() => { setError(""); setLoading(true); setReload(value => value + 1); }}>다시 조회</button></div>
      : <><p className={styles.summary}>전체 {data.totalElements}명{data.totalPages > 0 && ` · ${data.number + 1} / ${data.totalPages} 페이지`} · 페이지당 10명{!admin && <span className={styles.scope}>본인 담당 고객</span>}</p>
        <div className={styles.scroll} role="region" aria-label="고객 목록 표" tabIndex={0}><table className={styles.table}><thead><tr><th>고객명</th><th>연락처</th><th>관심 과정</th><th>상태</th>{!admin && <th>태그</th>}<th>{admin ? "현재 담당자" : "최근 상담일"}</th>{admin && <th>담당자 선택</th>}<th>관리</th></tr></thead>
          <tbody>{data.content.length === 0 ? <tr><td colSpan={7} className={styles.empty}>조건에 맞는 고객이 없습니다.</td></tr> : data.content.map(row => <tr key={row.customerId}>
            <td><strong>{row.name}</strong></td><td>{row.phone}</td><td>{name("INTEREST_COURSE", row.interestCourse)}</td>
            <td>{admin ? name("CUSTOMER_STATUS", row.status) : <select aria-label={`${row.name} 상태`} disabled={!hydrated || busy !== null} value={drafts[row.customerId]?.status ?? row.status} onChange={event => field(row,"status",event.target.value)}>
              {!options("CUSTOMER_STATUS").some(code => code.code === row.status) && <option disabled value={row.status}>{row.status}</option>}{options("CUSTOMER_STATUS").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}</select>}</td>
            {!admin && <td><select aria-label={`${row.name} 태그`} disabled={!hydrated || busy !== null} value={drafts[row.customerId]?.customerTag ?? row.customerTag ?? ""} onChange={event => field(row,"customerTag",event.target.value)}><option value="">없음</option>
              {row.customerTag && !options("CUSTOMER_TAG").some(code => code.code === row.customerTag) && <option disabled value={row.customerTag}>{row.customerTag}</option>}{options("CUSTOMER_TAG").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}</select></td>}
            <td>{admin ? row.counselorName || "미지정" : row.lastActivityAt?.slice(0,16).replace("T"," ") || "-"}</td>
            {admin && <td><select aria-label={`${row.name} 담당 상담사`} disabled={busy !== null || !counselors.length} value={drafts[row.customerId]?.counselorId ?? String(row.counselorId ?? "")} onChange={event => field(row,"counselorId",event.target.value)}><option value="" disabled>상담사 선택</option>
              {row.counselorId && !counselors.some(staff => staff.userId === row.counselorId) && <option disabled value={row.counselorId}>{row.counselorName} (현재 배정 불가)</option>}{counselors.map(staff => <option key={staff.userId} value={staff.userId}>{staff.name} ({staff.email})</option>)}</select></td>}
            <td><div className={styles.actions}><button type="button" disabled={!drafts[row.customerId] || busy !== null} aria-label={`${row.name} 저장`} onClick={() => void save(row)}>저장</button>
              {!admin && <Link href={`/consultant/customers/${row.customerId}`}>상담 기록</Link>}
              {admin && row.counselorId !== null && (releaseId === row.customerId ? <div role="group" aria-label={`${row.name} 배정 해제 확인`}><p>고객 상태와 기록은 유지하고 배정을 해제할까요?</p><button type="button" disabled={busy !== null} onClick={() => void save(row,true)}>해제 확인</button><button type="button" disabled={busy !== null} onClick={() => setReleaseId(null)}>취소</button></div>
                : <button type="button" disabled={busy !== null} aria-label={`${row.name} 배정 해제`} onClick={() => setReleaseId(row.customerId)}>배정 해제</button>)}</div></td>
          </tr>)}</tbody></table></div>
        {data.totalPages > 0 && <nav className={styles.pagination} aria-label="고객 목록 페이지"><button type="button" disabled={data.number === 0 || busy !== null} onClick={() => changeQuery({ ...query, page: query.page - 1 })}>이전</button><span aria-live="polite">{data.number + 1} / {data.totalPages}</span><button type="button" disabled={data.number + 1 >= data.totalPages || busy !== null} onClick={() => changeQuery({ ...query, page: query.page + 1 })}>다음</button></nav>}
      </>}
  </div></section>;
}
