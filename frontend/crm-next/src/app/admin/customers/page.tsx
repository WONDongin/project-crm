import CustomerList from "@/features/customers/CustomerList";

export default async function AdminCustomersPage({ searchParams }: {
  searchParams: Promise<{ status?: string | string[]; counselor?: string | string[] }>
}) {
  const params = await searchParams;
  const status = typeof params.status === "string" ? params.status : "";
  const counselor = params.counselor === "unassigned" ? "unassigned" : "";
  return <CustomerList key={`${status}:${counselor}`} admin initialStatus={status} initialCounselor={counselor} />;
}
