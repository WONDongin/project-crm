"use client";

import { useEffect, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { getConsAttachments, uploadConsAttachment, downloadConsAttachment, deleteConsAttachment } from "@/lib/api/consultant.api";
import styles from "./CustomerDetail.module.css";

type Attachment = { fileId: number; originalName: string; sizeBytes: number; contentType: string; uploadedAt: string };
const accept = ".png,.jpg,.jpeg,.gif,.webp,.pdf,.txt,.docx,.xlsx,.pptx";
export default function ActivityAttachments({ customerId, activityId, editable, onRestricted }: {
  customerId: string; activityId: number; editable: boolean; onRestricted: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [files, setFiles] = useState<Attachment[] | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [busy, setBusy] = useState(false);
  const running = useRef(false);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [selected, setSelected] = useState<File | null>(null);
  const input = useRef<HTMLInputElement>(null);
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    getConsAttachments(customerId, activityId, controller.signal).then(response => {
      if (!response.success) throw new Error("조회 실패");
      if (!controller.signal.aborted) { setFiles(response.data); setLoadError(""); }
    }).catch(cause => {
      if (controller.signal.aborted) return;
      const status = isAxiosError(cause) ? cause.response?.status : undefined;
      if (status === 403) { onRestricted(); return; }
      setFiles(null);
      setLoadError(status === 404
        ? "첨부파일 목록을 찾을 수 없습니다. 상담 기록을 다시 확인해 주세요."
        : "첨부파일을 불러오지 못했습니다. 다시 조회해 주세요.");
    });
    return () => controller.abort();
  }, [open, attempt, customerId, activityId, onRestricted]);

  async function action(operation: () => Promise<void>) {
    if (running.current) return;
    running.current = true; setBusy(true); setError(""); setMessage("");
    try { await operation(); }
    catch (cause) {
      const status = isAxiosError(cause) ? cause.response?.status : undefined;
      if (status === 403) { onRestricted(); return; }
      if (status === 404) {
        setError("첨부파일 또는 상담 기록을 찾을 수 없습니다. 목록을 다시 확인해 주세요.");
        setDeleting(null);
        setAttempt(value => value + 1);
        return;
      }
      setError(status === 413 ? "파일당 10MB까지 첨부할 수 있습니다."
        : status === 409 ? "기록당 최대 10개까지 첨부할 수 있습니다."
        : status === 400 ? "파일 확장자와 실제 형식, 파일명을 확인해 주세요."
        : "처리 결과를 확인할 수 없습니다. 목록을 다시 조회해 주세요.");
    } finally { running.current = false; setBusy(false); }
  }
  return <div className={styles.attachments}>
    <button type="button" className={styles.attachmentButton} aria-expanded={open}
      onClick={() => setOpen(!open)}>첨부파일 {open ? "접기" : "보기"}</button>
    {open && <div aria-busy={busy}>
      <p className={styles.contentTitle}>PNG·JPG·GIF·WEBP·PDF·TXT·DOCX·XLSX·PPTX · 파일당 10MB · 기록당 최대 10개</p>
      {(error || loadError) && <div role="alert"><p>{error || loadError}</p><button type="button" disabled={busy}
        onClick={() => { setError(""); setLoadError(""); setFiles(null); setAttempt(v => v + 1); }}>다시 조회</button></div>}
      {message && <p role="status">{message}</p>}
      {files === null ? (!(error || loadError) && <p role="status">첨부파일을 불러오는 중입니다.</p>) : files.length === 0 ? <p>첨부파일이 없습니다.</p> :
        <ul className={styles.attachmentList}>{files.map(file => <li key={file.fileId}>
          <span className={styles.fileName}>{file.originalName} <small>({(file.sizeBytes / 1024).toFixed(1)} KB)</small></span>
          <div className={styles.formActions}>
            <button type="button" disabled={busy} aria-label={`${file.originalName} 다운로드`} onClick={() => action(async () => {
              const blob: Blob = await downloadConsAttachment(customerId, activityId, file.fileId);
              const url = URL.createObjectURL(blob); const link = document.createElement("a");
              link.href = url; link.download = file.originalName; document.body.appendChild(link); link.click(); link.remove();
              setTimeout(() => URL.revokeObjectURL(url), 1000);
            })}>다운로드</button>
            {editable && (deleting === file.fileId ? <>
              <span>삭제할까요?</span><button type="button" disabled={busy} onClick={() => action(async () => {
                const response = await deleteConsAttachment(customerId, activityId, file.fileId);
                if (!response.success) throw new Error("삭제 실패");
                setDeleting(null); setMessage("첨부파일이 삭제되었습니다."); setAttempt(v => v + 1);
              })}>삭제 확인</button><button type="button" disabled={busy} onClick={() => setDeleting(null)}>취소</button>
            </> : <button type="button" disabled={busy} aria-label={`${file.originalName} 삭제`} onClick={() => setDeleting(file.fileId)}>삭제</button>)}
          </div>
        </li>)}</ul>}
      {editable && <form aria-label="첨부파일 업로드" onSubmit={event => {
        event.preventDefault(); if (!selected) return;
        if (!selected.size || selected.size > 10 * 1024 * 1024) { setError("빈 파일은 제외하고 파일당 10MB까지 첨부할 수 있습니다."); return; }
        action(async () => {
          const response = await uploadConsAttachment(customerId, activityId, selected);
          if (!response.success) throw new Error("업로드 실패");
          setSelected(null); if (input.current) input.current.value = "";
          setMessage("첨부파일이 등록되었습니다."); setAttempt(v => v + 1);
        });
      }}>
        <label className={styles.uploadLabel}>첨부할 파일<input aria-label="첨부할 파일" ref={input} type="file" accept={accept}
          disabled={busy} onChange={event => setSelected(event.target.files?.[0] || null)} /></label>
        <button type="submit" className={styles.retryButton} disabled={busy || !selected || files === null || files.length >= 10}>업로드</button>
      </form>}
      {busy && <p role="status">처리 중입니다.</p>}
    </div>}
  </div>;
}
