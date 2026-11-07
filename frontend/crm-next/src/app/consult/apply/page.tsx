"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
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
    consultType: "",
    preferredTime: "",
    content: "",
    leadSource: "",
  });

  const router = useRouter();
  const [errorMessage, setErrorMessage] = useState("");

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

    if (!form.consultType) {
      return "상담 유형을 선택하세요.";
    }

    if (!form.preferredTime) {
      return "상담 희망 시간대를 선택하세요.";
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

    const validationError = validate();

    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    try {
      await applyConsult(form);
      // 완료 페이지 이동
      router.push("/consult/complete");
    } catch (error) {
      console.error(error);

      setErrorMessage("상담 신청 중 오류가 발생했습니다.");
    }
  };

  if (!hydrated) {
    return <div className={styles.applyPage} role="status">상담 신청 정보를 불러오는 중입니다.</div>;
  }

  return (
    <main className={styles.applyPage}>
      <div className={styles.applyCard}>
        <div className={styles.logoBox}>
          <Link href="/" className={styles.brand} aria-label="DIHS CRM 홈">DIHS<span>CRM</span></Link>

          <h1 className={styles.title}>상담 신청</h1>

          <p className={styles.desc}>관심 과정과 상담 내용을 남겨주시면 담당 상담사가 연락드립니다.</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {/* 기본 정보 */}
          <h2 className={styles.sectionTitle}>기본 정보</h2>
          <div className={styles.inputGroup}>
            <label htmlFor="name">이름 *</label>

            <input
              type="text"
              id="name"
              name="name"
              value={form.name}
              onChange={handleChange}
              className={styles.input}
              placeholder="이름을 입력하세요."
            />
          </div>
          <div className={styles.inputGroup}>
            <label htmlFor="birthDate">생년월일 *</label>

            <input
              type="date"
              id="birthDate"
              name="birthDate"
              value={form.birthDate}
              onChange={handleChange}
              className={styles.input}
            />
          </div>
          <div className={styles.inputGroup}>
            <label htmlFor="phone">연락처 *</label>

            <input
              type="text"
              id="phone"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              className={styles.input}
              placeholder="010-0000-0000"
            />
          </div>
          <div className={styles.inputGroup}>
            <label htmlFor="email">이메일</label>

            <input
              type="email"
              id="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              className={styles.input}
              placeholder="example@email.com"
            />
          </div>
          {/* 상담 정보 */}
          <h2 className={styles.sectionTitle}>상담 정보</h2>
          <div className={styles.inputGroup}>
            <label htmlFor="interestCourse">관심 과정 *</label>

            <select
              id="interestCourse"
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
            <label htmlFor="consultType">상담 유형 *</label>

            <select
              id="consultType"
              name="consultType"
              value={form.consultType}
              onChange={handleChange}
              className={styles.select}
              aria-required="true"
            >
              <option value="">상담 유형을 선택하세요.</option>

              {codes.CONSULT_TYPE?.map((type: CommonCode) => (
                <option key={type.code} value={type.code}>
                  {type.name}
                </option>
              ))}
            </select>
          </div>
          <div className={`${styles.inputGroup} ${styles.full}`}>
            <label htmlFor="content">상담 희망 내용 *</label>

            <textarea
              id="content"
              name="content"
              value={form.content}
              onChange={handleChange}
              rows={5}
              className={styles.textarea}
              placeholder="상담 내용을 입력하세요."
            />
          </div>
          <div className={`${styles.inputGroup} ${styles.full}`}>
            <label htmlFor="preferredTime">상담 희망 시간대 *</label>

            <select
              id="preferredTime"
              name="preferredTime"
              value={form.preferredTime}
              onChange={handleChange}
              className={styles.select}
            >
              <option value="">선택하세요.</option>

              {codes.CONSULT_TIME?.map((time: CommonCode) => (
                <option key={time.code} value={time.code}>
                  {time.name}
                </option>
              ))}
            </select>
          </div>
          {/* 유입 경로 */}
          <h2 className={styles.sectionTitle}>유입 경로</h2>
          <div className={styles.radioGroup} role="group" aria-label="유입 경로">
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
            <div className={styles.errorBox} role="alert">{errorMessage}</div>
          )}
          <button type="submit" className={styles.applyButton}>
            상담 신청
          </button>
        </form>
      </div>
    </main>
  );
}
