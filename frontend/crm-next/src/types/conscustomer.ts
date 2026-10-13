export interface ConsCustomer {
  customerId: number;
  name: string;
  phone: string;
  interestCourse: string;
  status: string;
  customerTag: string | null;
  counselorId: number | null;
  counselorName: string | null;
  lastActivityAt: string | null;
  leadSource: string;
}
