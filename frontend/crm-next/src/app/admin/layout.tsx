import { redirect } from "next/navigation";

import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";

import CodeInitializer from "@/components/common/CodeInitializer";
import Link from "next/link";

export default async function layout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getCurrentUser();

  if (!user || user.role !== "ROLE_ADMIN") {
    redirect("/login");
  }

  return (
    <div>
      <CodeInitializer />

      <div>
        <h1>Admin Dashboard</h1>

        <div style={{ display: "flex", gap: "16px" }}>
          <span>{user.name} 님</span>
          <LogoutButton />
        </div>
      </div>

      <div>
        <aside>
          <h3>관리자 메뉴</h3>

          <ul>
            <li>
              <Link href="/admin/dashboard">대시보드</Link>
            </li>

            <li>
              <Link href="/admin/users">사용자 관리</Link>
            </li>

            <li>
              <Link href="/admin/customers">고객 관리</Link>
            </li>
          </ul>
        </aside>
      </div>

      <hr />
      {children}
    </div>
  );
}
