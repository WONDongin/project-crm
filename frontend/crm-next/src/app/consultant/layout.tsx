// 상담사 화면 공통 레이아웃
import { redirect } from "next/navigation";
import LogoutButton from "@/components/auth/LogoutButton";
import { getCurrentUser } from "@/lib/server/auth";
import CodeInitializer from "@/components/common/CodeInitializer";
import Link from "next/link";
import ConsultantNav from "@/components/consultant/ConsultantNav";
import theme from "@/styles/CrmTheme.module.css";
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
    <div className={`${theme.theme} ${styles.layout}`}>
      <CodeInitializer />

      <a className={styles.skipLink} href="#main-content">본문으로 이동</a>
      <header className={styles.topbar}>
        <div className={styles.topbarInner}>
          <Link href="/consultant/dashboard" className={styles.brand} aria-label="DIHS CRM 메인">DIHS<span>CRM</span></Link>
          <span className={styles.product}>교육 상담 관리</span>
          <ConsultantNav />
          <div className={styles.account}><span>{user.name} · 상담사</span><LogoutButton /></div>
        </div>
      </header>

      <main id="main-content" className={styles.main}>{children}</main>
    </div>
  );
}

