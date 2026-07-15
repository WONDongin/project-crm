import Link from "next/link";
import styles from "../ConsultPage.module.css";

export default function ConsultCompletePage() {
  return (
    <main className={styles.container}>
      <section className={styles.completeCard}>
        <div className={styles.completeIcon}>✓</div>

        <h1 className={styles.completeTitle}>상담 신청이 완료되었습니다</h1>

        <p className={styles.completeDescription}>
          입력해 주신 정보를 확인한 후
          <br />
          담당 상담사가 순차적으로 연락드리겠습니다.
        </p>

        <Link href="/consult/apply" className={styles.completeButton}>
          상담 신청 페이지로 돌아가기
        </Link>
      </section>
    </main>
  );
}
