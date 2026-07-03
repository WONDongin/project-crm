"use client";

import { useState } from "react";

import { applyConsult } from "@/lib/api/consult.api";
import { CommonCode } from "@/types/commonCode";
import useCodeStore from "@/stores/codeStore";

import styles from "../ConsultPage.module.css";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const PHONE_REGEX = /^01([0|1|6|7|8|9])-?\d{3,4}-?\d{4}$/;

export default function ConsultApplyPage() {
  const { codes, hydrated } = useCodeStore();

  const [form, setForm] = useState({
    name: "",
    birthDate: "",
    phone: "",
    email: "",
    interestCourse: "",
    preferredTime: "",
    content: "",
    leadSource: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleChange = (
    e:
      | React.ChangeEvent<HTMLInputElement>
      | React.ChangeEvent<HTMLSelectElement>
      | React.ChangeEvent<HTMLTextAreaElement>,
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
      return "연락처를 입력하세요.";
    }

    if (!PHONE_REGEX.test(form.phone)) {
      return "연락처 형식이 올바르지 않습니다.";
    }

    if (form.email && !EMAIL_REGEX.test(form.email)) {
      return "이메일 형식이 올바르지 않습니다.";
    }

    if (!form.interestCourse) {
      return "관심 과정을 선택하세요.";
    }

    if (!form.content.trim()) {
      return "상담 희망 내용을 입력하세요.";
    }

    if (!form.leadSource) {
      return "유입 경로를 선택하세요.";
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
        preferredTime: "",
        content: "",
        leadSource: "",
      });
    } catch (error) {
      console.error(error);

      setErrorMessage("상담 신청 중 오류가 발생했습니다.");
    }
  };

  if (!hydrated) {
    return <div>공통코드 로딩중...</div>;
  }

  return (
    <div className={styles.applyPage}>
      <div className={styles.applyCard}>
        <div className={styles.logoBox}>
          <div className={styles.logo}>CRM</div>

          <h1 className={styles.title}>상담 신청</h1>

          <p className={styles.desc}>상담 정보를 입력해주세요.</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {/* 기본 정보 */}
          <h3 className={styles.sectionTitle}>기본 정보</h3>

          <div className={styles.inputGroup}>
            <label>이름 *</label>

            <input
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              className={styles.input}
              placeholder="이름을 입력하세요."
            />
          </div>

          <div className={styles.inputGroup}>
            <label>생년월일 *</label>

            <input
              type="date"
              name="birthDate"
              value={form.birthDate}
              onChange={handleChange}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>연락처 *</label>

            <input
              type="text"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              className={styles.input}
              placeholder="010-0000-0000"
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
              placeholder="example@email.com"
            />
          </div>

          {/* 상담 정보 */}
          <h3 className={styles.sectionTitle}>상담 정보</h3>

          <div className={styles.inputGroup}>
            <label>관심 과정 *</label>

            <select
              name="interestCourse"
              value={form.interestCourse}
              onChange={handleChange}
              className={styles.select}
            >
              <option value="">과정을 선택하세요.</option>

              {codes.INTEREST_COURSE?.map((course: CommonCode) => (
                <option key={course.code} value={course.code}>
                  {course.name}
                </option>
              ))}
            </select>
          </div>

          <div className={styles.inputGroup}>
            <label>상담 희망 내용 *</label>

            <textarea
              name="content"
              value={form.content}
              onChange={handleChange}
              rows={5}
              className={styles.textarea}
              placeholder="상담 내용을 입력하세요."
            />
          </div>

          <div className={styles.inputGroup}>
            <label>상담 희망 시간대</label>

            <select
              name="preferredTime"
              value={form.preferredTime}
              onChange={handleChange}
              className={styles.select}
            >
              <option value="">선택하세요.</option>
              <option value="MORNING">오전 (09:00 ~ 12:00)</option>
              <option value="AFTERNOON">오후 (13:00 ~ 18:00)</option>
              <option value="EVENING">저녁 (18:00 ~ 21:00)</option>
            </select>
          </div>

          {/* 유입 경로 */}
          <h3 className={styles.sectionTitle}>유입 경로</h3>

          <div className={styles.radioGroup}>
            {codes.LEAD_SOURCE?.map((source: CommonCode) => (
              <label key={source.code} className={styles.radioItem}>
                <input
                  type="radio"
                  name="leadSource"
                  value={source.code}
                  checked={form.leadSource === source.code}
                  onChange={handleChange}
                />

                {source.name}
              </label>
            ))}
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
