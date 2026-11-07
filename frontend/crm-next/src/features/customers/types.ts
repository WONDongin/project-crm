export type CustomerEntry = {
  customerId: number; name: string; phone: string; status: string; interestCourse: string;
  customerTag: string | null; counselorId: number | null; counselorName: string | null;
  lastActivityAt: string | null; leadSource: string; createdAt: string | null;
};
export type CustomerPage = { content: CustomerEntry[]; number: number; size: number; totalElements: number; totalPages: number };
export type Query = { keyword: string; status: string; interestCourse: string; counselor: string; page: number };
