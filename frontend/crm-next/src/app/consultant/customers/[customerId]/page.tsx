import CustomerDetail from "@/features/consultant/customers/CustomerDetail";

export default async function CustomerDetailPage({
  params,
}: {
  params: Promise<{ customerId: string }>;
}) {
  const { customerId } = await params;
  return <CustomerDetail key={customerId} customerId={customerId} />;
}
