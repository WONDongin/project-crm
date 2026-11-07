import { redirect } from "next/navigation";

import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";

import CodeInitializer from "@/components/common/CodeInitializer";
import styles from "./AdminLayout.module.css";
import Link from "next/link";
import AdminNav from "@/components/admin/AdminNav";
import theme from "@/styles/CrmTheme.module.css";

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
    <div className={`${theme.theme} ${styles.layout}`}>
      <CodeInitializer />

      <a className={styles.skipLink} href="#main-content">본문으로 이동</a>
      <header className={styles.topbar}>
        <div className={styles.topbarInner}>
          <Link href="/admin/dashboard" className={styles.brand} aria-label="DIHS CRM 관리자 메인">DIHS<span>CRM</span></Link>
          <span className={styles.product}>관리자</span>
          <AdminNav />
          <div className={styles.account}><span>{user.name} · 관리자</span><LogoutButton /></div>
        </div>
      </header>

      <main id="main-content" className={styles.main}>{children}</main>
    </div>
  );
}
