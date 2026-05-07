// 로그인 페이지
// src/app/login/page.tsx
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { login } from "@/lib/api/auth.api";
import { getCommonCodes } from "@/lib/api/common.api";

import { ROLES } from "@/constants/roles";

import useCodeStore from "@/stores/codeStore";

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
      return "이메일 형식이 올바르지 않습니다. (예: test@gmail.com)";
    }

    if (!password.trim()) {
      return "비밀번호를 입력하세요.";
    }

    if (!PASSWORD_REGEX.test(password)) {
      return "비밀번호 형식이 올바르지 않습니다. (8자 이상, 영문, 숫자, 특수문자 포함)";
    }

    return null;
  };

  // 로그인 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setErrorMessage("");

    // 클라이언트 검증
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
    <div style={{ maxWidth: 360, margin: "100px auto" }}>
      <h2>CRM 로그인</h2>

      <form onSubmit={handleSubmit}>
        <div>
          <input
            type="email"
            placeholder="이메일을 입력하세요"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>

        <div style={{ marginTop: 10 }}>
          <input
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        <div style={{ marginTop: 20 }}>
          <button type="submit">로그인</button>
        </div>
      </form>

      {errorMessage && (
        <p style={{ color: "red", marginTop: 12 }}>{errorMessage}</p>
      )}
    </div>
  );
}
