export interface Customer {
  customerId: number;
  name: string;
  phone: string;
  interestCourse: string;
  status: string;
  counselorId: number | null;
  counselorName: string | null;
}
