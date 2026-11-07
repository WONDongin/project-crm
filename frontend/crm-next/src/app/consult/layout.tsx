import theme from "@/styles/CrmTheme.module.css";
import styles from "./ConsultPage.module.css";
import CodeInitializer from "@/components/common/CodeInitializer";

export default function ConsultLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className={`${theme.theme} ${styles.publicRoot}`}>
      <CodeInitializer />
      {children}
    </div>
  );
}
