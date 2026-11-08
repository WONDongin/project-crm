export type Account = {
  userId: number; name: string; email: string; roles: string; specialty: string;
  status: string; memo: string | null; assignedCustomerCount: number; lastLoginAt: string | null;
};
export type AccountDraft = {
  name: string; email: string; roles: string; specialty: string; status: string; memo: string; password?: string;
};
export type Change = { user: Account; releasedCustomerCount: number; reauthenticationRequired: boolean };

export type AccountPage = {
  content: Account[]; number: number; size: number; totalElements: number; totalPages: number;
};
export type AccountQuery = { keyword: string; roles: string; status: string; page: number };
