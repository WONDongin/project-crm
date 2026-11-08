"use client";

import { useEffect, useState, type FormEvent } from "react";
import { isCancel } from "axios";
import { getUsers } from "@/lib/api/admin.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import UserEditModal from "./UserEditModal";
import type { Account, AccountPage, AccountQuery } from "./types";
import styles from "./Users.module.css";

const initialQuery: AccountQuery = { keyword: "", roles: "", status: "", page: 0 };
const emptyPage: AccountPage = { content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 };

export default function AdminUserList({ currentUserId }: { currentUserId: number }) {
  const [data, setData] = useState<AccountPage>(emptyPage);
  const [query, setQuery] = useState<AccountQuery>(initialQuery);
  const [filters, setFilters] = useState(initialQuery);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [message, setMessage] = useState("");
  const [dialog, setDialog] = useState<{ mode: "create" | "edit" | "password"; user?: Account } | null>(null);
  const { codes, hydrated } = useCodeStore();
  useEffect(() => {
    const controller = new AbortController();
    getUsers({ ...query, size: 10 }, controller.signal).then(response => {
      if (!response.success) throw new Error("조회 실패");
      if (controller.signal.aborted) return;
      const result = response.data as AccountPage;
      const lastPage = Math.max(0, result.totalPages - 1);
      if (query.page > lastPage) {
        setQuery(previous => ({ ...previous, page: lastPage }));
        return;
      }
      setData(result); setError(""); setLoading(false);
    }).catch(cause => {
      if (controller.signal.aborted || isCancel(cause)) return;
      setError("사용자 목록을 불러오지 못했습니다. 다시 조회해 주세요."); setLoading(false);
    });
    return () => controller.abort();
  }, [attempt, query]);
  function refresh() { setLoading(true); setError(""); setAttempt(value => value + 1); }
  function changeQuery(next: AccountQuery) {
    setMessage(""); setError(""); setLoading(true); setQuery(next);
  }
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    changeQuery({ ...filters, keyword: filters.keyword.trim(), page: 0 });
  }
  function reset() { setFilters(initialQuery); changeQuery({ ...initialQuery }); }
  const options = (group: string) => (codes[group] || []) as { code: string; name: string }[];
  const users = data.content;
  const name = (group: string, value: string) => getCodeName(codes, group, value) || value;
  return <section className={styles.page} aria-busy={loading}>
    <header className={styles.header}>
      <div><h1>사용자 관리</h1><p>관리자·상담사 계정과 담당 고객을 관리합니다.</p></div>
      <div className={styles.actions}><button type="button" disabled={loading} onClick={refresh}>목록 새로고침</button><button type="button" className={styles.primary} disabled={!hydrated || loading || !!error} onClick={() => setDialog({ mode: "create" })}>계정 생성</button></div>
    </header>
    <div className={styles.panel}>
    <form className={styles.filters} aria-label="사용자 검색 조건" onSubmit={search}>
      <fieldset disabled={loading}>
        <legend className={styles.srOnly}>이름·이메일, 역할, 상태로 사용자 검색</legend>
        <label className={styles.keyword}>이름·이메일
          <input aria-label="이름·이메일 검색" placeholder="이름 또는 이메일 입력" maxLength={100}
            value={filters.keyword} onChange={event => setFilters({ ...filters, keyword: event.target.value })} />
        </label>
        <label>역할
          <select aria-label="역할 필터" disabled={!hydrated} value={filters.roles} onChange={event => setFilters({ ...filters, roles: event.target.value })}>
            <option value="">전체 역할</option>
            {options("USER_ROLE").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}
          </select>
        </label>
        <label>상태
          <select aria-label="상태 필터" disabled={!hydrated} value={filters.status} onChange={event => setFilters({ ...filters, status: event.target.value })}>
            <option value="">전체 상태</option>
            {options("USER_STATUS").map(code => <option key={code.code} value={code.code}>{code.name}</option>)}
          </select>
        </label>
        <div className={styles.actions}><button type="submit" className={styles.primary}>검색</button><button type="button" onClick={reset}>초기화</button></div>
      </fieldset>
    </form>
    {message && <p role="status" className={styles.notice}>{message}</p>}
    {error ? <div role="alert" className={styles.notice}><p>{error}</p><button type="button" onClick={refresh}>다시 조회</button></div>
      : loading ? <p role="status">사용자 목록을 불러오는 중입니다.</p>
      : <><p className={styles.summary}>검색 결과 {data.totalElements.toLocaleString("ko-KR")}개{data.totalPages > 0 && ` · ${data.number + 1} / ${data.totalPages} 페이지`} · 페이지당 10개</p>
        <div className={styles.scroll} role="region" aria-label="사용자 목록 표" tabIndex={0}><table className={styles.table}>
          <thead><tr><th>사용자</th><th>역할</th><th>전공</th><th>상태</th><th className={styles.number}>담당 고객</th><th>최근 로그인</th><th>관리</th></tr></thead>
          <tbody>{users.length === 0 ? <tr><td colSpan={7} className={styles.empty}>{query.keyword || query.roles || query.status ? "조건에 맞는 계정이 없습니다." : "등록된 계정이 없습니다."}</td></tr> : users.map(user => <tr key={user.userId}>
            <td><strong>{user.name}{user.userId === currentUserId && <span className={styles.self}>본인</span>}</strong><small>{user.email}</small></td>
            <td>{name("USER_ROLE", user.roles)}</td><td>{name("USER_SPECIALTY", user.specialty)}</td>
            <td><span className={user.status === "ACTIVE" ? styles.active : styles.inactive}>{name("USER_STATUS", user.status)}</span></td>
            <td className={styles.number}>{user.assignedCustomerCount ?? 0}명</td>
            <td>{user.lastLoginAt?.slice(0, 16).replace("T", " ") || "-"}</td>
            <td><div className={styles.actions}><button type="button" disabled={!hydrated} aria-label={`${user.name} 계정 수정`} onClick={() => setDialog({ mode: "edit", user })}>수정</button>
              <button type="button" aria-label={`${user.name} 비밀번호 초기화`} onClick={() => setDialog({ mode: "password", user })}>비밀번호 초기화</button></div></td>
          </tr>)}</tbody>
        </table></div>
        {data.totalPages > 0 && <nav className={styles.pagination} aria-label="사용자 목록 페이지">
          <button type="button" disabled={data.number === 0} onClick={() => changeQuery({ ...query, page: data.number - 1 })}>이전</button>
          <span aria-live="polite">{data.number + 1} / {data.totalPages}</span>
          <button type="button" disabled={data.number + 1 >= data.totalPages} onClick={() => changeQuery({ ...query, page: data.number + 1 })}>다음</button>
        </nav>}
      </>}
    </div>
    {dialog && <UserEditModal mode={dialog.mode} user={dialog.user} currentUserId={currentUserId}
      onClose={() => setDialog(null)} onSaved={text => {
        const created = dialog.mode === "create";
        setDialog(null); setMessage(text);
        if (created) setQuery(previous => ({ ...previous, page: 0 }));
        refresh();
      }} />}
  </section>;
}
