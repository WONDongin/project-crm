import AdminUserList from "@/features/admin/users/AdminUserList";
import { getCurrentUser } from "@/lib/server/auth";

export default async function AdminUsersPage() {
  const user = await getCurrentUser();
  return user ? <AdminUserList currentUserId={user.userId} /> : null;
}
