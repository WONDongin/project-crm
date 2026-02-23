// 관리자 화면 공통 레이아웃
import { redirect } from "next/navigation";
import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";

export default async function layout({
  children,
}: {
  children: React.ReactNode;
}) {

  const user = await getCurrentUser();

  // 서버에서 인증 검사
    if (!user || user.role !== "ROLE_ADMIN") {
      redirect("/login");
    }

  return (
    <div >
      <div>
        <h1>Admin Dashboard</h1>

        <div style={{ display: "flex", gap: "16px", alignItems: "center" }}>
          <span>{user.name} 님</span>
          <LogoutButton />
        </div>
      </div>

      <hr />
      {children}
    </div>
  );
}