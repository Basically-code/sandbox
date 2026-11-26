import React, { useState, useEffect, useMemo } from 'react';
import {
  User, Menu, X, Bell, Search, Globe, RefreshCw, CheckCircle2,
  SlidersHorizontal, AlertCircle, Info, ExternalLink, Filter,
  ArrowRight, Compass, Sparkles, BookOpen, GraduationCap, Building2,
  Mail, Award, Sliders, Check, Bookmark, BookmarkCheck, Trash2,
  Calendar, FileText, Clock, Plus, Send, Tag, ShieldCheck, Save,
  Copy, FileCheck, Scale, Briefcase, Laptop, Home, Utensils,
  Calculator, ChevronRight, ChevronLeft, Eye, AlertTriangle,
  HelpCircle, Download, Layers
} from 'lucide-react';
import {
  StudyLevel, FinancialNeedLevel, ApplicationStatus, MathStream, NBTStatus,
  SubjectMark, CoverageBreakdown, Scholarship, DocumentVaultState,
  StudentProfile, ApplicationTrackerRecord, DeadlineReminder,
  CategoryAlertSubscription, UnifiedSubscriberUnit, PortalHealthStatus,
  MidnightSyncLog, SecurityAuditEvent, WebsiteCMSConfig
} from '../types';
import {
  safeGetStorage, safeSetStorage, copyTextSafe, sanitizePortalUrl,
  sanitizeStrictText, computeDeterministicHash, getNSCLevel,
  getDaysRemaining, evaluateScholarshipMatch, formatStudyLevel,
  formatCurrencyZAR, generateMotivationLetter
} from '../utils/fundingHelpers';

// ============================================================================
// VIEW 4: RECOMMENDATIONS VIEW
// ============================================================================

interface RecommendationsViewProps {
  profile: StudentProfile;
  scholarships: Scholarship[];
  readinessScore: number;
  onOpenEditProfile: () => void;
  savedIds: string[];
  compareIds: string[];
  onToggleSave: (sch: Scholarship) => void;
  onToggleCompare: (id: string) => void;
  onOpenReminderModal: (sch: Scholarship) => void;
  onOpenSmartApply: (sch: Scholarship) => void;
}

export const RecommendationsView: React.FC<RecommendationsViewProps> = ({
  profile,
  scholarships,
  readinessScore,
  onOpenEditProfile,
  savedIds,
  onToggleSave,
  onOpenReminderModal,
  onOpenSmartApply,
}) => {
  const sorted = [...scholarships].sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));

  return (
    <div className="space-y-8 pb-16">
      <div>
        <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 uppercase tracking-wider font-mono">
          <span>Personalized Match Engine</span>
          <span className="text-neutral-300">·</span>
          <span>Multi-Factor South African Scoring</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-950 mt-1">
          Your Ranked Bursary Matches
        </h1>
        <p className="text-sm text-neutral-600 mt-1 max-w-3xl">
          Ranked using your {profile.marksPercentage}% academic average, APS {profile.matricAPS},{' '}
          {profile.mathStream === 'pure_math' ? 'Pure Mathematics' : 'Mathematics'} stream,{' '}
          {profile.fieldOfStudy} discipline, and household income tier.
        </p>
      </div>

      {/* Student Profile Summary Bar */}
      <div className="bg-white border border-neutral-200 rounded-2xl p-5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-xs text-neutral-500 font-medium">
            <User className="w-3.5 h-3.5 text-emerald-700" />
            <span>
              Active Candidate: <strong className="text-neutral-900">{profile.fullName}</strong> (
              {profile.province})
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs text-neutral-700">
            <span className="font-semibold text-neutral-900">
              {formatStudyLevel(profile.studyLevel)}
            </span>
            <span>·</span>
            <span>
              Average: <strong className="font-mono text-neutral-950">{profile.marksPercentage}%</strong>
            </span>
            <span>·</span>
            <span>
              APS: <strong className="font-mono text-neutral-950">{profile.matricAPS}/42</strong>
            </span>
            <span>·</span>
            <span>
              Field: <strong className="text-neutral-900">{profile.fieldOfStudy}</strong>
            </span>
            <span>·</span>
            <span>
              Documents Ready: <strong className="text-emerald-800 font-mono">{readinessScore}% Ready</strong>
            </span>
          </div>
        </div>

        <button
          onClick={onOpenEditProfile}
          className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors whitespace-nowrap self-start sm:self-auto"
        >
          <Sliders className="w-3.5 h-3.5 text-neutral-600" />
          <span>Edit Student Profile & Docs</span>
        </button>
      </div>

      {/* Ranked Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {sorted.map((sch) => {
          const isSaved = savedIds.includes(sch.id);
          const score = sch.matchScore || 75;

          return (
            <div
              key={sch.id}
              className="bg-white border border-neutral-200 rounded-2xl p-5 hover:border-neutral-400 transition-colors shadow-xs flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-2.5">
                  <span className="text-xs text-neutral-500 font-medium">{sch.provider}</span>
                  <div
                    className={`flex items-center gap-1 text-xs font-mono font-bold px-2.5 py-1 rounded-md ${
                      score >= 80
                        ? 'bg-emerald-100 text-emerald-900'
                        : score >= 65
                        ? 'bg-amber-100 text-amber-900'
                        : 'bg-neutral-100 text-neutral-800'
                    }`}
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>{score}% Match</span>
                  </div>
                </div>

                <h3 className="text-base font-bold text-neutral-950 mb-1">{sch.title}</h3>

                <div className="flex flex-wrap items-center gap-1.5 text-xs text-neutral-500 mb-3 font-mono">
                  <span className="font-semibold text-neutral-900">{sch.amountDisplay}</span>
                  <span>·</span>
                  <span>Min {sch.minMarks}%</span>
                  <span>·</span>
                  <span>Closes {sch.deadline}</span>
                </div>

                {/* Match Reasons & Warnings */}
                <div className="bg-neutral-50 border border-neutral-100 rounded-xl p-3 mb-4 space-y-1.5 text-xs">
                  <div className="text-[11px] font-bold text-neutral-700 uppercase tracking-wider font-mono">
                    Eligibility Diagnosis:
                  </div>
                  {sch.matchReasons?.map((reason, idx) => (
                    <div key={idx} className="flex items-start gap-1.5 text-neutral-700">
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                      <span>{reason}</span>
                    </div>
                  ))}
                  {sch.matchWarnings?.map((warn, idx) => (
                    <div key={idx} className="flex items-start gap-1.5 text-amber-800">
                      <AlertTriangle className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                      <span>{warn}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex items-center gap-2 pt-3 border-t border-neutral-100">
                <button
                  onClick={() => onOpenSmartApply(sch)}
                  className="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-emerald-900 rounded-xl transition-colors"
                >
                  <span>Start Guided Apply</span>
                </button>

                <a
                  href={sanitizePortalUrl(sch.portalUrl)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-2 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors inline-flex items-center gap-1"
                >
                  <span>Portal</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>

                <button
                  onClick={() => onToggleSave(sch)}
                  className={`p-2 rounded-xl border transition-colors ${
                    isSaved
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                      : 'bg-white text-neutral-700 border-neutral-200 hover:bg-neutral-50'
                  }`}
                  title="Track Application"
                >
                  {isSaved ? <BookmarkCheck className="w-4 h-4" /> : <Bookmark className="w-4 h-4" />}
                </button>

                <button
                  onClick={() => onOpenReminderModal(sch)}
                  className="p-2 rounded-xl border border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-50 transition-colors"
                  title="Set Deadline Reminder"
                >
                  <Bell className="w-4 h-4" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
