"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { isAxiosError } from "axios";
import api from "@/lib/api/axios";
import { createAdminUser, updateAdminUser, resetAdminUserPassword } from "@/lib/api/admin.api";
import useCodeStore from "@/stores/codeStore";
import type { Account, AccountDraft, Change } from "./types";
import styles from "./Users.module.css";

export default function UserEditModal({ mode, user, currentUserId, onClose, onSaved }: {
  mode: "create" | "edit" | "password"; user?: Account; currentUserId: number;
  onClose: () => void; onSaved: (message: string) => void;
}) {
  const element = useRef<HTMLDialogElement>(null);
  const busy = useRef(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [acknowledged, setAcknowledged] = useState(false);
  const [draft, setDraft] = useState<AccountDraft>({ name: user?.name || "", email: user?.email || "", roles: user?.roles || "ROLE_CONSULTANT", specialty: user?.specialty || "", status: user?.status || "ACTIVE", memo: user?.memo || "" });
  const { codes } = useCodeStore();
  const self = user?.userId === currentUserId;
  const releases = mode === "edit" && (draft.roles !== "ROLE_CONSULTANT" || draft.status !== "ACTIVE") && (user?.assignedCustomerCount ?? 0) > 0;
  // 카운트가 오래됐더라도 상담사의 자격 해제는 담당 고객 전체에 적용됨을 확인한다.
  const changesEligibility = mode === "edit" && user?.roles === "ROLE_CONSULTANT" && user.status === "ACTIVE" && (draft.roles !== "ROLE_CONSULTANT" || draft.status !== "ACTIVE");
  const title = mode === "create" ? "계정 생성" : mode === "password" ? "비밀번호 초기화" : "계정 수정";
  useEffect(() => {
    const dialog = element.current;
    dialog?.showModal();
    dialog?.querySelector<HTMLInputElement>("input:not(:disabled)")?.focus();
  }, []);
  function field(key: keyof AccountDraft, value: string) { setDraft(previous => ({ ...previous, [key]: value })); setAcknowledged(false); }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy.current) return;
    if (mode !== "edit" && (password.length < 8 || password.length > 20 || password !== confirm)) { setError("비밀번호는 8~20자로 입력하고 확인 값도 같게 입력해 주세요."); return; }
    if ((releases || changesEligibility) && !acknowledged) { setError("담당 고객의 배정 해제 안내를 확인해 주세요."); return; }
    busy.current = true; setSaving(true); setError("");
    try {
      const response = mode === "create" ? await createAdminUser({ ...draft, password })
        : mode === "password" ? await resetAdminUserPassword(user!.userId, password) : await updateAdminUser(user!.userId, draft);
      if (!response.success) throw new Error("저장 실패");
      const result = response.data as Change;
      if (mode !== "create" && result.reauthenticationRequired) {
        try { await api.post("/auth/logout"); } finally { window.location.assign("/login"); }
        return;
      }
      const released = mode === "edit" ? result.releasedCustomerCount : 0;
      onSaved(mode === "create" ? "계정이 생성되었습니다." : mode === "password" ? "비밀번호가 초기화되었습니다. 해당 사용자는 다시 로그인해야 합니다."
        : `계정 정보가 수정되었습니다.${released ? ` 담당 고객 ${released}명을 미배정으로 전환했습니다.` : ""}`);
    } catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined;
      const data = isAxiosError<{ message?: string }>(cause) ? cause.response?.data : undefined;
      setError(data?.message || (status === 403 ? "현재 계정에 관리자 변경 권한이 없습니다." : status === 404 ? "사용자를 찾을 수 없습니다. 목록을 다시 확인해 주세요." : "처리 결과를 확인할 수 없습니다. 목록을 확인한 뒤 다시 시도해 주세요."));
    } finally { busy.current = false; setSaving(false); }
  }
  const options = (group: string) => (codes[group] || []) as { code: string; name: string }[];
  return <dialog ref={element} className={styles.dialog} aria-labelledby="account-dialog-title" onCancel={event => { if (busy.current) event.preventDefault(); else onClose(); }}>
    <form onSubmit={submit}>
      <header className={styles.dialogHeader}><div><h2 id="account-dialog-title">{title}</h2>{user && <p>{user.name} · {user.email}</p>}</div><button type="button" disabled={saving} onClick={onClose} aria-label="닫기">닫기</button></header>
      <fieldset className={styles.fields} disabled={saving}>
        {mode !== "password" && <>
          <label>이름<input autoFocus required maxLength={50} value={draft.name} onChange={event => field("name", event.target.value)} /></label>
          <label>이메일<input required type="email" maxLength={100} autoComplete="off" value={draft.email} onChange={event => field("email", event.target.value)} /></label>
          {([ ["roles", "역할", "USER_ROLE"], ["specialty", "전공", "USER_SPECIALTY"], ["status", "상태", "USER_STATUS"] ] as const).map(([key,label,group]) => <label key={key}>{label}<select aria-label={label} required disabled={self && key !== "specialty"} value={draft[key]} onChange={event => field(key,event.target.value)}>
            <option value="">선택해 주세요</option>
            {draft[key] && !options(group).some(code => code.code === draft[key]) && <option disabled value={draft[key]}>기존 값 (사용 불가)</option>}
            {options(group).filter(code => key !== "roles" || ["ROLE_ADMIN","ROLE_CONSULTANT"].includes(code.code)).map(code => <option key={code.code} value={code.code}>{code.name}</option>)}
          </select></label>)}
          <label className={styles.full}>메모<textarea rows={3} maxLength={10000} value={draft.memo} onChange={event => field("memo",event.target.value)} /></label>
          {self && <p className={styles.full}>본인의 역할·활성 상태는 해제할 수 없습니다. 이메일 변경 후에는 다시 로그인합니다.</p>}
        </>}
        {mode !== "edit" && <>
          {mode === "password" && <p className={styles.full}>초기화하면 이 계정의 기존 로그인은 종료됩니다.{self ? " 저장 후 로그인 화면으로 이동합니다." : ""}</p>}
          <label>새 비밀번호<input autoFocus={mode === "password"} type="password" required minLength={8} maxLength={20} autoComplete="new-password" aria-describedby="account-password-rule" value={password} onChange={event => setPassword(event.target.value)} /></label>
          <label>비밀번호 확인<input type="password" required minLength={8} maxLength={20} autoComplete="new-password" value={confirm} onChange={event => setConfirm(event.target.value)} /></label>
          <p id="account-password-rule" className={styles.full}>비밀번호는 8~20자로 입력해 주세요.</p>
        </>}
        {(releases || changesEligibility) && <div className={`${styles.warning} ${styles.full}`}><p>담당 고객은 모두 미배정으로 전환합니다. 현재 목록 기준 {user?.assignedCustomerCount ?? 0}명이며, 저장 시점의 배정 고객 전체에 적용합니다. 고객 상태와 상담 기록은 유지합니다.</p>
          <label className={styles.check}><input type="checkbox" checked={acknowledged} onChange={event => setAcknowledged(event.target.checked)} />배정 해제 내용을 확인했습니다.</label></div>}
        {error && <p className={styles.full} role="alert">{error}</p>}
        <div className={`${styles.actions} ${styles.full}`}><button className={styles.primary} type="submit">저장</button><button type="button" onClick={onClose}>취소</button>{saving && <span role="status">저장 중입니다.</span>}</div>
      </fieldset>
    </form>
  </dialog>;
}
