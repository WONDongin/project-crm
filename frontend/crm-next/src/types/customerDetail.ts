import type { ConsCustomer } from "./conscustomer";

export interface CustomerDetail extends Omit<ConsCustomer, "counselorId"> {
  birthDate: string | null;
  email: string | null;
  createdAt: string | null;
  application: {
    activityId: number;
    preferredTime: string | null;
    content: string | null;
    receivedAt: string | null;
  } | null;
}
