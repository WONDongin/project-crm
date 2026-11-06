"use client";

import { useRef, useState, type FormEvent } from "react";
import { isAxiosError } from "axios";
import { createConsCustomerActivity } from "@/lib/api/consultant.api";
import useCodeStore from "@/stores/codeStore";
import styles from "./CustomerDetail.module.css";

type CodeOption = { code: string; name: string };

export default function CreateActivityForm({ customerId, onCreated, onRestricted }: {
  customerId: string; onCreated: () => void; onRestricted: () => void;
}) {
  const { codes } = useCodeStore();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const busy = useRef(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [draft, setDraft] = useState({ consultType: "", activityResult: "", content: "", nextAction: "" });
  const types: CodeOption[] = codes.CONSULT_TYPE || [];
  const results: CodeOption[] = (codes.ACTIVITY_RESULT || []).filter((code: CodeOption) => code.code !== "RECEIVED");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy.current) return;
    if (!draft.content.trim()) { setError("상담 내용을 입력해 주세요."); return; }
    busy.current = true; setSaving(true); setError(""); setMessage("");
    try {
      const response = await createConsCustomerActivity(customerId, draft);
      if (!response.success) throw new Error("등록 실패");
      setDraft({ consultType: "", activityResult: "", content: "", nextAction: "" });
      setOpen(false); setMessage("상담 기록이 등록되었습니다.");
      onCreated();
    } catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined;
      if (status === 403 || status === 404) { onRestricted(); return; }
      setError(status === 400 ? "입력값과 사용 가능한 상담 유형·결과를 확인해 주세요."
        : "등록 결과를 확인할 수 없습니다. 상담 기록을 확인한 후 다시 시도해 주세요.");
    } finally { busy.current = false; setSaving(false); }
  }

  return <div className={styles.composer}>
    {message && <p role="status">{message}</p>}
    {!open ? <button className={styles.retryButton} type="button" onClick={() => {
      setOpen(true); setMessage("");
    }}>상담 추가</button> : <form onSubmit={submit} aria-label="상담 기록 등록">
      <h3>상담 기록 등록</h3>
      <fieldset disabled={saving} className={styles.formFields}>
        <label>상담 유형<select aria-label="상담 유형" autoFocus required value={draft.consultType}
          onChange={(e) => setDraft({ ...draft, consultType: e.target.value })}>
          <option value="">선택해 주세요</option>
          {types.map((code) => <option key={code.code} value={code.code}>{code.name}</option>)}
        </select></label>
        <label>상담 결과<select aria-label="상담 결과" required value={draft.activityResult}
          onChange={(e) => setDraft({ ...draft, activityResult: e.target.value })}>
          <option value="">선택해 주세요</option>
          {results.map((code) => <option key={code.code} value={code.code}>{code.name}</option>)}
        </select></label>
        <label className={styles.fullField}>상담 내용<textarea required rows={5} maxLength={10000}
          value={draft.content} onChange={(e) => setDraft({ ...draft, content: e.target.value })} />
          <span>최대 10,000자</span></label>
        <label className={styles.fullField}>다음 액션 (선택)<textarea rows={2} maxLength={255}
          value={draft.nextAction} onChange={(e) => setDraft({ ...draft, nextAction: e.target.value })} />
          <span>최대 255자</span></label>
        {error && <p role="alert" className={styles.fullField}>{error}</p>}
        <div className={styles.formActions}>
          <button type="submit" className={styles.retryButton}
            disabled={!types.length || !results.length}>저장</button>
          <button type="button" onClick={() => { setOpen(false); setError(""); }}>닫기</button>
          {saving && <span role="status">저장 중입니다.</span>}
        </div>
        {(!types.length || !results.length) && <p role="alert" className={styles.fullField}>
          사용 가능한 상담 유형·결과가 없습니다. 공통 코드 상태를 확인해 주세요.</p>}
      </fieldset>
    </form>}
  </div>;
}
