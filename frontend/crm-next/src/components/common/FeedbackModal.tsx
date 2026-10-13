"use client";

import { useEffect, useRef } from "react";

import styles from "./FeedbackModal.module.css";

type FeedbackModalProps = {
  type: "success" | "error";
  title: string;
  message: string;
  confirmLabel?: string;
  onClose: () => void;
};

export default function FeedbackModal({
  type,
  title,
  message,
  confirmLabel = "확인",
  onClose,
}: FeedbackModalProps) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    closeButtonRef.current?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div
      className={styles.backdrop}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        className={styles.dialog}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="feedback-modal-title"
        aria-describedby="feedback-modal-message"
      >
        <button
          ref={closeButtonRef}
          type="button"
          className={styles.closeButton}
          aria-label="팝업 닫기"
          onClick={onClose}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m6 6 12 12M18 6 6 18" />
          </svg>
        </button>

        <div
          className={`${styles.iconWrap} ${
            type === "success" ? styles.successIcon : styles.errorIcon
          }`}
          aria-hidden="true"
        >
          {type === "success" ? (
            <svg viewBox="0 0 48 48">
              <path d="m12 24 8 8 17-18" />
            </svg>
          ) : (
            <svg viewBox="0 0 48 48">
              <path d="M24 13v15M24 35h.02" />
            </svg>
          )}
        </div>

        <h2 id="feedback-modal-title" className={styles.title}>
          {title}
        </h2>
        <p id="feedback-modal-message" className={styles.message}>
          {message}
        </p>

        <button
          type="button"
          className={styles.confirmButton}
          onClick={onClose}
        >
          {confirmLabel}
        </button>
      </section>
    </div>
  );
}
