"use client";

import { useRef, useState, type FormEvent } from "react";
import { isAxiosError } from "axios";
import { updateConsCustomerProfile } from "@/lib/api/consultant.api";
import type { CustomerDetail } from "@/types/customerDetail";
import useCodeStore from "@/stores/codeStore";
import styles from "./CustomerDetail.module.css";

export default function CustomerProfileForm({ customer, onSaved, onRestricted }: {
  customer: CustomerDetail; onSaved: () => void; onRestricted: () => void;
}) {
  const { codes } = useCodeStore();
  const options: {code: string; name: string}[] = codes.INTEREST_COURSE || [];
  const initial = { name: customer.name, phone: customer.phone, interestCourse: customer.interestCourse };
  const [draft, setDraft] = useState(initial);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const busy = useRef(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy.current) return;
    if (!draft.name.trim() || !/^[0-9]{9,11}$/.test(draft.phone.replace(/[^0-9]/g, ""))) {
      setError("이름과 연락처(숫자 9~11자리)를 확인해 주세요."); return;
    }
    busy.current = true; setSaving(true); setError("");
    try {
      const response = await updateConsCustomerProfile(customer.customerId, draft);
      if (!response.success) throw new Error("저장 실패");
      setOpen(false); setMessage("고객 정보가 수정되었습니다."); onSaved();
    } catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined;
      if (status === 403 || status === 404) { onRestricted(); return; }
      setError(status === 409 ? "같은 연락처와 생년월일로 등록된 고객이 있습니다. 입력값을 확인해 주세요."
        : status === 400 ? "이름·연락처·사용 가능한 관심 과정을 확인해 주세요."
        : "저장 결과를 확인할 수 없습니다. 고객 정보를 확인한 후 다시 시도해 주세요.");
    } finally { busy.current = false; setSaving(false); }
  }
  return <div className={styles.composer}>
    {message && <p role="status">{message}</p>}
    {!open ? <button type="button" className={styles.retryButton} onClick={() => {
      setDraft(initial); setMessage(""); setError(""); setOpen(true);
    }}>고객 정보 수정</button> : <form aria-label="고객 기본 정보 수정" onSubmit={submit}>
      <h3>고객 기본 정보 수정</h3>
      <fieldset className={styles.formFields} disabled={saving}>
        <label>이름<input aria-label="이름" autoFocus required maxLength={50} value={draft.name}
          onChange={e => setDraft({...draft, name: e.target.value})} /></label>
        <label>연락처<input aria-label="연락처" required type="tel" maxLength={20} value={draft.phone}
          onChange={e => setDraft({...draft, phone: e.target.value})} /><span>숫자 9~11자리, 하이픈·공백 입력 가능</span></label>
        <label className={styles.fullField}>관심 과정<select aria-label="관심 과정" required value={draft.interestCourse}
          onChange={e => setDraft({...draft, interestCourse: e.target.value})}>
          <option value="">선택해 주세요</option>
          {draft.interestCourse && !options.some(c => c.code === draft.interestCourse) &&
            <option disabled value={draft.interestCourse}>기존 값 (사용 불가) · 다시 선택해 주세요</option>}
          {options.map(c => <option key={c.code} value={c.code}>{c.name}</option>)}
        </select></label>
        {error && <p role="alert" className={styles.fullField}>{error}</p>}
        <div className={styles.formActions}>
          <button type="submit" className={styles.retryButton} disabled={!options.length}>저장</button>
          <button type="button" onClick={() => setOpen(false)}>닫기</button>
          {saving && <span role="status">저장 중입니다.</span>}
        </div>
      </fieldset>
    </form>}
  </div>;
}
