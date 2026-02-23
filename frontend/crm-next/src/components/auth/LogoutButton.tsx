"use client";

import { logout } from "@/lib/api/auth.api";

export default function LogoutButton() {

  const handleLogout = async () => {
    await logout();
  };

  return (
    <button onClick={handleLogout}>
      로그아웃
    </button>
  );
}