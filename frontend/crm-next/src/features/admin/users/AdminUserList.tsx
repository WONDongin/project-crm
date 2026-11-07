"use client";

import { useEffect, useState } from "react";
import { isCancel } from "axios";
import { getUsers } from "@/lib/api/admin.api";
import useCodeStore from "@/stores/codeStore";
import { getCodeName } from "@/utils/codeUtil";
import UserEditModal from "./UserEditModal";
import type { Account } from "./types";
import styles from "./Users.module.css";

export default function AdminUserList({ currentUserId }: { currentUserId: number }) {
  const [users, setUsers] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [message, setMessage] = useState("");
  const [dialog, setDialog] = useState<{ mode: "create" | "edit" | "password"; user?: Account } | null>(null);
  const { codes, hydrated } = useCodeStore();
  useEffect(() => {
    const controller = new AbortController();
    getUsers(controller.signal).then(response => {
      if (!response.success) throw new Error("조회 실패");
      if (!controller.signal.aborted) { setUsers(response.data); setError(""); }
    }).catch(cause => {
      if (controller.signal.aborted || isCancel(cause)) return;
      setError("사용자 목록을 불러오지 못했습니다. 다시 조회해 주세요.");
    })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [attempt]);
  function refresh() { setLoading(true); setError(""); setAttempt(value => value + 1); }
  const name = (group: string, value: string) => getCodeName(codes, group, value) || value;
  return <section className={styles.page} aria-busy={loading}>
    <header className={styles.header}>
      <div><h1>사용자 관리</h1><p>관리자·상담사 계정과 담당 고객을 관리합니다.</p></div>
      <div className={styles.actions}><button type="button" disabled={loading} onClick={refresh}>목록 새로고침</button><button type="button" className={styles.primary} disabled={!hydrated || loading || !!error} onClick={() => setDialog({ mode: "create" })}>계정 생성</button></div>
    </header>
    {message && <p role="status" className={styles.notice}>{message}</p>}
    {error ? <div role="alert" className={styles.notice}><p>{error}</p><button type="button" onClick={refresh}>다시 조회</button></div>
      : loading ? <p role="status">사용자 목록을 불러오는 중입니다.</p>
      : <><p className={styles.summary}>계정 {users.length}개 · 활성 상담사 {users.filter(user => user.roles === "ROLE_CONSULTANT" && user.status === "ACTIVE").length}명</p>
        <div className={styles.scroll}><table className={styles.table}>
          <thead><tr><th>사용자</th><th>역할</th><th>전공</th><th>상태</th><th className={styles.number}>담당 고객</th><th>최근 로그인</th><th>관리</th></tr></thead>
          <tbody>{users.length === 0 ? <tr><td colSpan={7} className={styles.empty}>등록된 계정이 없습니다.</td></tr> : users.map(user => <tr key={user.userId}>
            <td><strong>{user.name}{user.userId === currentUserId && <span className={styles.self}>본인</span>}</strong><small>{user.email}</small></td>
            <td>{name("USER_ROLE", user.roles)}</td><td>{name("USER_SPECIALTY", user.specialty)}</td>
            <td><span className={user.status === "ACTIVE" ? styles.active : styles.inactive}>{name("USER_STATUS", user.status)}</span></td>
            <td className={styles.number}>{user.assignedCustomerCount ?? 0}명</td>
            <td>{user.lastLoginAt?.slice(0, 16).replace("T", " ") || "-"}</td>
            <td><div className={styles.actions}><button type="button" disabled={!hydrated} aria-label={`${user.name} 계정 수정`} onClick={() => setDialog({ mode: "edit", user })}>수정</button>
              <button type="button" aria-label={`${user.name} 비밀번호 초기화`} onClick={() => setDialog({ mode: "password", user })}>비밀번호 초기화</button></div></td>
          </tr>)}</tbody>
        </table></div></>}
    {dialog && <UserEditModal mode={dialog.mode} user={dialog.user} currentUserId={currentUserId}
      onClose={() => setDialog(null)} onSaved={text => { setDialog(null); setMessage(text); refresh(); }} />}
  </section>;
}
