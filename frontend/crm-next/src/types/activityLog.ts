export interface ActivityLogEntry {
  activityId: number;
  createdBy: string;
  counselorName: string | null;
  consultType: string | null;
  activityResult: string;
  preferredTime: string | null;
  content: string | null;
  nextAction: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  editable: boolean;
}

export interface ActivityLogPage {
  content: ActivityLogEntry[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
