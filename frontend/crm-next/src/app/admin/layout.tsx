// 관리자 화면 공통 레이아웃
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div>
      <h1>Admin Dashboard</h1>
      {children}
    </div>
  );
}
