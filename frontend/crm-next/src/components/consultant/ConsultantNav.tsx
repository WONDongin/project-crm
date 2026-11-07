"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import styles from "@/app/consultant/ConsultantLayout.module.css";
export default function ConsultantNav() {
  const pathname = usePathname();
  return <nav className={styles.nav} aria-label="상담사 메뉴">{[
    { href: "/consultant/dashboard", label: "메인" },
    { href: "/consultant/customers", label: "고객 관리" },
  ].map(item => <Link key={item.href} href={item.href} aria-current={pathname.startsWith(item.href) ? "page" : undefined}>{item.label}</Link>)}</nav>;
}
