"use client";

import { useEffect, useState } from "react";

import { getUsers } from "@/lib/api/admin.api";

import useCodeStore from "@/stores/codeStore";

import { getCodeName } from "@/utils/codeUtil";

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);

  const { codes, hydrated } = useCodeStore();

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const res = await getUsers();

        console.log("유저 API 응답 =", res);

        if (res.success) {
          setUsers(res.data);
        }
      } catch (error) {
        console.error("사용자 조회 실패:", error);
      }
    };

    fetchUsers();
  }, []);

  useEffect(() => {
    console.log("USER_ROLE =", codes.USER_ROLE);

    if (codes.USER_ROLE) {
      console.log(
        "ROLE_ADMIN 변환 결과 =",
        getCodeName(codes, "USER_ROLE", "ROLE_ADMIN"),
      );
    }
  }, [codes]);

  if (!hydrated) {
    return <div>공통코드 로딩중...</div>;
  }

  return (
    <div>
      <h2>사용자 관리</h2>

      <table border="1">
        <thead>
          <tr>
            <th>이름</th>
            <th>이메일</th>
            <th>역할</th>
            <th>전공</th>
            <th>상태</th>
            <th>마지막 로그인</th>
          </tr>
        </thead>

        <tbody>
          {users.map((user) => (
            <tr key={user.userId}>
              <td>{user.name}</td>

              <td>{user.email}</td>

              <td>{getCodeName(codes, "USER_ROLE", user.roles)}</td>

              <td>{getCodeName(codes, "USER_SPECIALTY", user.specialty)}</td>

              <td>{getCodeName(codes, "USER_STATUS", user.status)}</td>

              <td>{user.lastLoginAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
