// ============================================================================
// TYPES & DOMAIN INTERFACES
// ============================================================================

export type StudyLevel =
  | 'all'
  | 'matric'
  | 'tvet'
  | 'undergraduate'
  | 'honours'
  | 'masters'
  | 'phd';

export type FinancialNeedLevel =
  | 'nsfas_eligible'
  | 'missing_middle'
  | 'moderate'
  | 'none';

export type ApplicationStatus =
  | 'saved'
  | 'preparing'
  | 'submitted'
  | 'awarded'
  | 'not_awarded';

export type MathStream = 'pure_math' | 'technical_math' | 'math_lit';

export interface SubjectMark {
  id: string;
  name: string;
  percentage: number;
  countsForAPS: boolean;
}

export interface CoverageBreakdown {
  tuition: boolean;
  accommodation: boolean;
  meals: boolean;
  books: boolean;
  laptop: boolean;
  stipend: boolean;
  vacationWork: boolean;
}

export interface Scholarship {
  id: string;
  title: string;
  provider: string;
  amount: number; // In ZAR (R)
  currency: string;
  amountDisplay: string;
  studyLevel: StudyLevel;
  minMarks: number; // percentage (0-100)
  minAPS: number; // NSC APS score (20-42)
  requiresPureMath: boolean;
  minMathMark?: number;
  minScienceMark?: number;
  field: string;
  categoryKey: string;
  financialNeedRequired: boolean;
  maxHouseholdIncome?: number; // ZAR p.a.
  serviceObligation: string; // e.g., "None (Grant)" or "1 year work-back per year funded"
  deadline: string; // YYYY-MM-DD
  portalUrl: string;
  description: string;
  academicRequirements: string;
  requiredDocuments: string[];
  coverage: CoverageBreakdown;
  cycleInfo: string;
  tags: string[];
  matchScore?: number;
  matchReasons?: string[];
  matchWarnings?: string[];
}

export interface DocumentVaultState {
  certifiedId: boolean;
  academicTranscript: boolean;
  universityAcceptance: boolean;
  proofOfIncome: boolean;
  proofOfResidence: boolean;
  cvAndMotivation: boolean;
}

export type NBTStatus = 'not_booked' | 'booked_aql_mat' | 'completed';

export interface StudentProfile {
  uid: string;
  email: string;
  fullName: string;
  studyLevel: StudyLevel;
  fieldOfStudy: string;
  marksPercentage: number;
  matricAPS: number;
  mathStream: MathStream;
  mathMark: number;
  scienceMark: number;
  englishMark: number;
  financialNeedLevel: FinancialNeedLevel;
  sassaBeneficiary?: boolean;
  hasDisability?: boolean;
  nbtStatus?: NBTStatus;
  yearsFundedSoFar?: number;
  degreeDurationYears?: number;
  province: string;
  targetInstitution: string;
  careerGoal: string;
  country: string;
  emailRemindersEnabled: boolean;
  documents: DocumentVaultState;
  subjects: SubjectMark[];
}

export interface ApplicationTrackerRecord {
  id: string;
  userId: string;
  scholarshipId: string;
  scholarshipTitle: string;
  providerName: string;
  amount: number;
  currency: string;
  amountDisplay: string;
  status: ApplicationStatus;
  deadline: string;
  notes: string;
  motivationLetter: string;
  completedDocs: string[];
  portalUrl: string;
  reminderScheduled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DeadlineReminder {
  id: string;
  userId: string;
  email: string;
  scholarshipId: string;
  scholarshipTitle: string;
  providerName: string;
  portalUrl: string;
  deadline: string;
  daysRemaining: number;
  scheduledDate: string;
  status: 'scheduled' | 'delivered' | 'dismissed';
  category?: string;
}

export interface CategoryAlertSubscription {
  id: string;
  email: string;
  category: string;
  createdAt: string;
}

export interface UnifiedSubscriberUnit {
  unitId: string;
  normalizedEmail: string;
  fullName: string;
  province: string;
  studyLevel: StudyLevel;
  marksPercentage: number;
  matricAPS: number;
  financialNeedLevel: FinancialNeedLevel;
  trackedBursaryTitles: string[];
  scheduledReminderDeadlines: { title: string; deadline: string; daysRemaining: number }[];
  subscribedCategories: string[];
  popiaConsentVerified: boolean;
  integrityHash: string;
  lastSyncedAt: string;
}

export interface PortalHealthStatus {
  scholarshipId: string;
  title: string;
  provider: string;
  portalUrl: string;
  httpStatus: number;
  livenessState: 'ONLINE_200' | 'REDIRECT_VERIFIED' | 'OFFLINE_ARCHIVED';
  sslValid: boolean;
  deadlineVerified: string;
  searchGroundedNote: string;
  integritySignature: string;
  lastCheckedAt: string;
}

export interface MidnightSyncLog {
  id: string;
  runTimestamp: string;
  triggerType: 'AUTO_MIDNIGHT_CRON' | 'MANUAL_REBUILD' | 'BOOT_VERIFICATION';
  portalsScanned: number;
  portalsOnline: number;
  portalsTakenDown: number;
  updatesApplied: number;
  catalogDigestHash: string;
  summary: string;
}

export interface SecurityAuditEvent {
  id: string;
  timestamp: string;
  severity: 'BLOCKED' | 'VERIFIED' | 'HARDENED';
  vector: string;
  detail: string;
  actionTaken: string;
}

export interface WebsiteCMSConfig {
  heroBadgeText: string;
  heroCycleText: string;
  heroHeadlinePrefix: string;
  heroHeadlineHighlight: string;
  heroHeadlineSuffix: string;
  heroSubheadline: string;
  announcementBannerEnabled: boolean;
  announcementBannerText: string;
  footerTagline: string;
  adminPasscode: string;
}
