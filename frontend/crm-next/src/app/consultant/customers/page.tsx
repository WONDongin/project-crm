import CustomerList from "@/features/customers/CustomerList";
export default async function ConsultantCustomersPage({ searchParams }: { searchParams: Promise<{ status?: string | string[] }> }) {
  const params = await searchParams;
  const status = typeof params.status === "string" ? params.status : "";
  return <CustomerList key={status} admin={false} initialStatus={status} />;
}
