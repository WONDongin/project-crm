// 로그인 페이지
// src/app/login/page.tsx

"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { login } from "@/lib/api/auth.api";

import { ROLES } from "@/constants/roles";

import useCodeStore from "@/stores/codeStore";

import styles from "./LoginPage.module.css";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const PASSWORD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");

  const [password, setPassword] = useState("");

  const [errorMessage, setErrorMessage] = useState("");

  // 공통코드 store
  const { setCodes } = useCodeStore();

  // 유효성 검사
  const validate = () => {
    if (!email.trim()) {
      return "이메일을 입력하세요.";
    }

    if (!EMAIL_REGEX.test(email)) {
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

    setErrorMessage("");

    const validationError = validate();

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    try {
      // 로그인 API 호출
      const user = await login({
        email,
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
    }
  };

  return (
    <div className={styles.loginPage}>
      <div className={styles.loginCard}>
        {/* 로고 */}
        <div className={styles.logoBox}>
          <div className={styles.logo}>CRM</div>

          <h1 className={styles.title}>CRM 로그인</h1>

          <p className={styles.desc}>관리자 및 상담사 전용 시스템입니다.</p>
        </div>

        {/* 로그인 폼 */}
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.inputGroup}>
            <label>Email</label>

            <input
              type="email"
              placeholder="이메일을 입력하세요"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={styles.input}
            />
          </div>

          <div className={styles.inputGroup}>
            <label>Password</label>

            <input
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
            />
          </div>

          {errorMessage && (
            <div className={styles.errorBox}>{errorMessage}</div>
          )}

          <button type="submit" className={styles.loginButton}>
            로그인
          </button>
        </form>
      </div>
    </div>
  );
}
