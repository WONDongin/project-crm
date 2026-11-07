// 로그인 페이지
// src/app/login/page.tsx

"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { login } from "@/lib/api/auth.api";

import { ROLES } from "@/constants/roles";

import Link from "next/link";
import theme from "@/styles/CrmTheme.module.css";

import styles from "./LoginPage.module.css";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const PASSWORD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");

  const [password, setPassword] = useState("");

  const [errorMessage, setErrorMessage] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const pending = useRef(false);

  // 유효성 검사
  const validate = () => {
    if (!email.trim()) {
      return "이메일을 입력하세요.";
    }

    if (!EMAIL_REGEX.test(email.trim())) {
      return "이메일 형식이 올바르지 않습니다.";
    }

    if (!password.trim()) {
      return "비밀번호를 입력하세요.";
    }

    if (!PASSWORD_REGEX.test(password)) {
      return "비밀번호 형식이 올바르지 않습니다.";
    }

    return null;
  };

  // 로그인 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (pending.current) return;
    setErrorMessage("");

    const validationError = validate();

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    pending.current = true;
    setSubmitting(true);
    try {
      // 로그인 API 호출
      const user = await login({
        email: email.trim(),
        password,
      });

      // 권한 체크
      if (![ROLES.ADMIN, ROLES.CONSULTANT].includes(user.role)) {
        throw new Error("권한 없음");
      }

      // 역할별 페이지 이동
      if (user.role === ROLES.ADMIN) {
        router.push("/admin/dashboard");
      } else if (user.role === ROLES.CONSULTANT) {
        router.push("/consultant/dashboard");
      }
    } catch (error) {
      console.error(error);

      setErrorMessage("로그인에 실패했습니다. 이메일과 비밀번호를 확인하세요.");
      pending.current = false;
      setSubmitting(false);
    }
  };

  return (
    <main className={`${theme.theme} ${styles.loginPage}`}>
      <section className={styles.intro} aria-label="DIHS CRM 소개">
        <Link href="/" className={styles.brand} aria-label="DIHS CRM 홈">DIHS<span>CRM</span></Link>
        <div className={styles.introContent}>
          <h2>상담의 시작부터,<br />다음 단계까지.</h2>
          <p>고객과의 대화에 집중하세요.<br />상담 관리는 DIHS CRM이 함께합니다.</p>
          <div className={styles.workflow}>
            <div><strong>상담 접수</strong><span>고객의 첫 관심을 한곳에</span></div>
            <div><strong>상담 진행</strong><span>대화와 다음 할 일을 기록</span></div>
            <div><strong>수강 전환</strong><span>고객의 변화를 이어서 관리</span></div>
          </div>
        </div>
        <p className={styles.introFooter}>교육 상담 고객 관리 시스템</p>
      </section>
      <section className={styles.formSide} aria-labelledby="login-title">
        <div className={styles.loginCard}>
          <h1 id="login-title" className={styles.title}>로그인</h1>
          <p className={styles.desc}>관리자·상담사 계정으로 시작하세요.</p>
          <form onSubmit={handleSubmit} className={styles.form} aria-busy={submitting}>
            <div className={styles.inputGroup}>
              <label htmlFor="email">이메일</label>
              <input id="email" name="email" type="email" autoComplete="username" required
                placeholder="name@example.com" value={email}
                onChange={(e) => setEmail(e.target.value)} className={styles.input} />
            </div>
            <div className={styles.inputGroup}>
              <label htmlFor="password">비밀번호</label>
              <div className={styles.passwordField}>
                <input id="password" name="password" type={showPassword ? "text" : "password"}
                  autoComplete="current-password" required placeholder="비밀번호를 입력하세요"
                  value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} />
                <button type="button" className={styles.reveal} aria-controls="password" aria-pressed={showPassword}
                  onClick={() => setShowPassword(value => !value)}>{showPassword ? "숨기기" : "보기"}</button>
              </div>
            </div>
            {errorMessage && <p className={styles.errorBox} role="alert">{errorMessage}</p>}
            <button type="submit" className={styles.loginButton} disabled={submitting}>
              {submitting ? "로그인 중…" : "로그인"}
            </button>
          </form>
          <p className={styles.help}>계정이 필요하거나 로그인이 어려우신가요?<br />시스템 관리자에게 문의해 주세요.</p>
          <Link className={styles.applyLink} href="/consult/apply">상담을 원하시나요? 상담 신청하기 →</Link>
        </div>
      </section>
    </main>
  );
}
