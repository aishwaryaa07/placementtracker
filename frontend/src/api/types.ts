export interface AuthResponse {
  token: string;
  email: string;
  name: string;
}

export interface StudentProfile {
  id: number;
  name: string;
  email: string;
  branch: string | null;
  graduationYear: number | null;
  cgpa: number | null;
  phone: string | null;
  resumeUrl: string | null;
}

export interface Company {
  id: number;
  name: string;
  website: string | null;
  description: string | null;
  contactEmail: string | null;
}

export interface Round {
  id: number;
  sequence: number;
  name: string;
  roundDate: string | null;
}

export type DriveStatus = "UPCOMING" | "ONGOING" | "CLOSED";

export interface Drive {
  id: number;
  company: Company;
  role: string;
  description: string | null;
  ctc: number | null;
  minCgpa: number | null;
  eligibleBranches: string[];
  applicationDeadline: string | null;
  driveDate: string | null;
  status: DriveStatus;
  rounds: Round[];
}

export interface StudentDriveView {
  drive: Drive;
  eligible: boolean;
}

export type RoundResultStatus = "PENDING" | "PASSED" | "FAILED";

export interface RoundResult {
  id: number;
  roundSequence: number;
  roundName: string;
  status: RoundResultStatus;
  remarks: string | null;
}

export type OfferStatus = "PENDING" | "ACCEPTED" | "DECLINED";

export interface Offer {
  id: number;
  ctcOffered: number;
  offerDate: string;
  status: OfferStatus;
}

export type ApplicationStatus = "APPLIED" | "IN_PROGRESS" | "REJECTED" | "SELECTED" | "WITHDRAWN";

export interface Application {
  id: number;
  drive: Drive;
  appliedAt: string;
  status: ApplicationStatus;
  roundResults: RoundResult[];
  offer: Offer | null;
}

export interface ApiErrorBody {
  message?: string;
  [field: string]: string | undefined;
}
