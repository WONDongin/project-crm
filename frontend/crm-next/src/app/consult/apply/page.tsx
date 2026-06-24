"use client";

import { useState } from "react";

import { applyConsult } from "@/lib/api/consult.api";
import styles from "../ConsultPage.module.css";

export default function ConsultApplyPage() {
  const [form, setForm] = useState({
    name: "",
    birthDate: "",
    phone: "",
    email: "",
    interestCourse: "",
    leadSource: "",
    content: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validate = () => {
    if (!form.name.trim()) {
      return "이름을 입력하세요.";
    }

    if (!form.birthDate) {
      return "생년월일을 입력하세요.";
    }

    if (!form.phone.trim()) {
      return "전화번호를 입력하세요.";
    }

    if (!form.interestCourse.trim()) {
      return "희망 과정을 입력하세요.";
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setErrorMessage("");
    setSuccessMessage("");

    const validationError = validate();

    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    try {
      await applyConsult(form);

      setSuccessMessage("상담 신청이 완료되었습니다.");

      setForm({
        name: "",
        birthDate: "",
        phone: "",
        email: "",
        interestCourse: "",
        leadSource: "",
        content: "",
      });
    } catch (error) {
      console.error(error);

      setErrorMessage("상담 신청 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className={styles.applyPage}>
      <div className={styles.applyCard}>
        <div className={styles.logoBox}>
          <div className={styles.logo}>CRM</div>

          <h1 className={styles.title}>상담 신청</h1>

          <p className={styles.desc}>상담 정보를 입력해주세요.</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.inputGroup}>
            <label>이름</label>
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>생년월일</label>
            <input
              type="date"
              name="birthDate"
              value={form.birthDate}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>전화번호</label>
            <input
              name="phone"
              value={form.phone}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>이메일</label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>희망 과정</label>
            <input
              name="interestCourse"
              value={form.interestCourse}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>유입 경로</label>
            <input
              name="leadSource"
              value={form.leadSource}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>상담 내용</label>

            <textarea
              name="content"
              value={form.content}
              onChange={handleChange}
              className={styles.textarea}
              rows={5}
            />
          </div>

          {errorMessage && (
            <div className={styles.errorBox}>{errorMessage}</div>
          )}

          {successMessage && (
            <div className={styles.successBox}>{successMessage}</div>
          )}

          <button type="submit" className={styles.applyButton}>
            상담 신청
          </button>
        </form>
      </div>
    </div>
  );
}
