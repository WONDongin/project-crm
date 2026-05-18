import { redirect } from "next/navigation";

import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";

import CodeInitializer from "@/components/common/CodeInitializer";
import styles from "./AdminLayout.module.css";
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
    <div className={styles.layout}>
      <CodeInitializer />

      <aside className={styles.sidebar}>
        <h1 className={styles.logo}>Consultant</h1>

        <div className={styles.userBox}>
          <span className={styles.userName}>{user.name}님</span>

          <LogoutButton />
        </div>

        <h3 className={styles.menuTitle}>관리자 메뉴</h3>

        <ul className={styles.menuList}>
          <li className={styles.menuItem}>
            <Link href="/admin/dashboard" className={styles.menuLink}>
              대시보드
            </Link>
          </li>

          <li className={styles.menuItem}>
            <Link href="/admin/users" className={styles.menuLink}>
              사용자 관리
            </Link>
          </li>

          <li className={styles.menuItem}>
            <Link href="/admin/customers" className={styles.menuLink}>
              고객 관리
            </Link>
          </li>
        </ul>
      </aside>

      <main className={styles.main}>{children}</main>
    </div>
  );
}
