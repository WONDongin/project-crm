import CodeInitializer from "@/components/common/CodeInitializer";

export default function ConsultLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <CodeInitializer />
      {children}
    </>
  );
}
