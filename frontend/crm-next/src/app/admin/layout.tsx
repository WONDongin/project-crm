import { redirect } from "next/navigation";

import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";

import CodeInitializer from "@/components/common/CodeInitializer";

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

      <hr />
      {children}
    </div>
  );
}