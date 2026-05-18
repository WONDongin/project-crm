// 상담사 화면 공통 레이아웃
import { redirect } from "next/navigation";
import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";
import CodeInitializer from "@/components/common/CodeInitializer";
import Link from "next/link";
import styles from "./ConsultantLayout.module.css";
export default async function layout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getCurrentUser();

  // 서버에서 인증 검사
  if (!user) {
    redirect("/login");
  }

  if (user.role !== "ROLE_CONSULTANT") {
    redirect("/forbidden");
  }

  return (
    <div className={styles.layout}>
      <CodeInitializer />

      <aside className={styles.sidebar}>
        <h1 className={styles.logo}>Consultant</h1>

        <div className={styles.userBox}>
          <span className={styles.userName}>{user.name} 상담사 님</span>

          <LogoutButton />
        </div>

        <h3 className={styles.menuTitle}>상담사 메뉴</h3>

        <ul className={styles.menuList}>
          <li className={styles.menuItem}>
            <Link href="/consultant/dashboard" className={styles.menuLink}>
              메인
            </Link>
          </li>

          <li className={styles.menuItem}>
            <Link href="/consultant/customers" className={styles.menuLink}>
              고객 관리
            </Link>
          </li>
        </ul>
      </aside>

      <main className={styles.main}>{children}</main>
    </div>
  );
}
