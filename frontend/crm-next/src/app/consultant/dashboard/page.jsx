// 상담사 대시보드 페이지

import styles from "../ConsultantLayout.module.css";

export default function ConsultantDashboardPage() {
  return (
    <div>
      {/* 페이지 제목 */}
      <div className={styles.dashboardHeader}>
        <div>
          <h1 className={styles.dashboardTitle}>상담사 대시보드</h1>

          <p className={styles.dashboardDesc}>
            담당 고객 현황과 최근 상담 내역을 확인하세요.
          </p>
        </div>

        <button className={styles.moveButton}>고객 목록 이동</button>
      </div>

      {/* 통계 카드 */}
      <div className={styles.statsGrid}>
        {/* 전체 담당 고객 */}
        <div className={`${styles.statsCard} ${styles.activeCard}`}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>전체 담당 고객</span>

            <span className={styles.statsIcon}>👥</span>
          </div>

          <h2 className={styles.statsValue}>24</h2>

          <p className={styles.statsChange}>+2</p>
        </div>

        {/* 신규 상담 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>신규 상담</span>

            <span className={styles.statsIcon}>✨</span>
          </div>

          <h2 className={styles.statsValue}>8</h2>

          <p className={styles.statsChange}>+3</p>
        </div>

        {/* 상담 진행 중 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>상담 진행 중</span>

            <span className={styles.statsIcon}>📌</span>
          </div>

          <h2 className={styles.statsValue}>12</h2>

          <p className={styles.statsChange}>-1</p>
        </div>

        {/* 장기 미응답 */}
        <div className={styles.statsCard}>
          <div className={styles.statsTop}>
            <span className={styles.statsLabel}>장기 미응답</span>

            <span className={styles.statsIcon}>⚠️</span>
          </div>

          <h2 className={styles.statsValue}>4</h2>

          <p className={styles.statsChange}>+1</p>
        </div>
      </div>

      {/* 최근 상담 고객 */}
      <div className={styles.dashboardTableCard}>
        <div className={styles.tableHeader}>
          <h2 className={styles.tableTitle}>최근 상담 고객</h2>
        </div>

        <table className={styles.table}>
          <thead>
            <tr>
              <th>고객명</th>
              <th>연락처</th>
              <th>관심 과정</th>
              <th>고객 상태</th>
              <th>최근 상담일</th>
              <th>상세보기</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td>김민수</td>
              <td>010-1111-2222</td>
              <td>JAVA</td>
              <td>
                <span className={styles.badge}>신규</span>
              </td>
              <td>2026-05-18</td>
              <td>
                <button className={styles.detailButton}>상세보기</button>
              </td>
            </tr>

            <tr>
              <td>이지은</td>
              <td>010-3333-4444</td>
              <td>REACT</td>
              <td>
                <span className={styles.badge}>진행중</span>
              </td>
              <td>2026-05-17</td>
              <td>
                <button className={styles.detailButton}>상세보기</button>
              </td>
            </tr>

            <tr>
              <td>박현우</td>
              <td>010-5555-6666</td>
              <td>SPRING</td>
              <td>
                <span className={styles.badge}>미응답</span>
              </td>
              <td>2026-05-15</td>
              <td>
                <button className={styles.detailButton}>상세보기</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
