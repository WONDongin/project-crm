"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import styles from "@/app/admin/AdminLayout.module.css";

export default function AdminNav() {
  const pathname = usePathname();
  return <nav className={styles.nav} aria-label="관리자 메뉴">{[
    { href: "/admin/dashboard", label: "대시보드" },
    { href: "/admin/users", label: "사용자 관리" },
    { href: "/admin/customers", label: "고객 관리" },
  ].map(item => <Link key={item.href} href={item.href}
    aria-current={pathname.startsWith(item.href) ? "page" : undefined}>{item.label}</Link>)}</nav>;
}
