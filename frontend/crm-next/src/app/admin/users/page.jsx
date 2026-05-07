// 사용자 관리 페이지
"use client";

import { useEffect, useState } from "react";
import { getUsers } from "@/lib/api/admin.api";

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);

  useEffect(() => {
    const fetchUsers = async () => {
      const res = await getUsers();

      if (res.success) {
        setUsers(res.data);
      }
    };

    fetchUsers();
  }, []);

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
              <td>{user.roles}</td>
              <td>{user.specialty}</td>
              <td>{user.status}</td>
              <td>{user.lastLoginAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
