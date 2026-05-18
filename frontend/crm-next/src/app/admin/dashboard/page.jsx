// 관리자 대시보드 페이지
import styles from "../AdminLayout.module.css";

export default function AdminDashboardPage() {
  return (
    <div>
      {/* 헤더 */}
      <div className={styles.dashboardHeader}>
        <div>
          <h1 className={styles.dashboardTitle}>관리자 대시보드</h1>

          <p className={styles.dashboardDesc}>
            CRM 운영 현황과 주요 관리 기능을 확인하세요.
          </p>
        </div>
      </div>

      {/* 통계 카드 */}
      <div className={styles.statsGrid}>
        {/* 전체 고객 수 */}
        <div className={`${styles.statsCard} ${styles.activeCard}`}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>전체 고객 수</span>

            <span className={styles.statsIcon}>👥</span>
          </div>

          <h2 className={styles.statsValue}>142</h2>

          <p className={styles.statsChange}>+12</p>
        </div>

        {/* 미지정 고객 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>미지정 고객</span>

            <span className={styles.statsIcon}>📌</span>
          </div>

          <h2 className={styles.statsValue}>18</h2>

          <p className={styles.statsChange}>-2</p>
        </div>

        {/* 활성 상담사 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>활성 상담사 수</span>

            <span className={styles.statsIcon}>👨‍💼</span>
          </div>

          <h2 className={styles.statsValue}>12</h2>

          <p className={styles.statsChange}>+1</p>
        </div>

        {/* 오늘 상담 건수 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>오늘 상담 건수</span>

            <span className={styles.statsIcon}>📞</span>
          </div>

          <h2 className={styles.statsValue}>36</h2>

          <p className={styles.statsChange}>+5</p>
        </div>
      </div>
    </div>
  );
}
