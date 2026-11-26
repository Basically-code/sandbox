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
// SCHOLARSHIP CARD COMPONENT (With Coverage Badges, Smart Apply & Compare)
// ============================================================================

interface ScholarshipCardProps {
  scholarship: Scholarship;
  isSaved: boolean;
  isCompared: boolean;
  onToggleSave: (sch: Scholarship) => void;
  onToggleCompare: (id: string) => void;
  onOpenReminderModal: (sch: Scholarship) => void;
  onOpenSmartApply: (sch: Scholarship) => void;
  studentMarks?: number;
}

export const ScholarshipCard: React.FC<ScholarshipCardProps> = ({
  scholarship,
  isSaved,
  isCompared,
  onToggleSave,
  onToggleCompare,
  onOpenReminderModal,
  onOpenSmartApply,
  studentMarks,
}) => {
  const daysLeft = getDaysRemaining(scholarship.deadline);
  const isExpiringSoon = daysLeft > 0 && daysLeft <= 21;
  const isExpired = daysLeft < 0;

  const meetsMarks =
    studentMarks !== undefined ? studentMarks >= scholarship.minMarks : true;

  return (
    <article className="bg-white border border-neutral-200 rounded-2xl p-5 hover:border-neutral-400 transition-all shadow-xs flex flex-col justify-between group">
      <div>
        {/* Top Metadata & Match Score Badge */}
        <div className="flex items-center justify-between gap-2 mb-2.5">
          <div className="flex flex-wrap items-center gap-1.5 text-[11px] text-neutral-500 font-mono">
            <span className="font-semibold text-neutral-800">
              {formatStudyLevel(scholarship.studyLevel)}
            </span>
            <span>·</span>
            <span>Min {scholarship.minMarks}% (APS {scholarship.minAPS})</span>
          </div>

          {scholarship.matchScore !== undefined && (
            <span
              className={`text-[11px] font-mono font-bold px-2 py-0.5 rounded-md shrink-0 ${
                scholarship.matchScore >= 80
                  ? 'bg-emerald-100 text-emerald-900'
                  : scholarship.matchScore >= 65
                  ? 'bg-amber-100 text-amber-900'
                  : 'bg-neutral-100 text-neutral-700'
              }`}
            >
              {scholarship.matchScore}% Match
            </span>
          )}
        </div>

        {/* Title & Provider */}
        <div className="mb-2.5">
          <h3 className="text-base font-bold text-neutral-950 tracking-tight leading-snug group-hover:text-emerald-900 transition-colors">
            {scholarship.title}
          </h3>
          <p className="text-xs font-medium text-neutral-500 mt-0.5">{scholarship.provider}</p>
        </div>

        {/* Description */}
        <p className="text-xs text-neutral-600 leading-relaxed line-clamp-2 mb-3">
          {scholarship.description}
        </p>

        {/* Coverage Perks Strip */}
        <div className="flex flex-wrap items-center gap-1.5 mb-3">
          {scholarship.coverage.tuition && (
            <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-neutral-100 text-neutral-700">
              100% Tuition
            </span>
          )}
          {scholarship.coverage.accommodation && (
            <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-neutral-100 text-neutral-700">
              Residence
            </span>
          )}
          {scholarship.coverage.laptop && (
            <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-800 border border-blue-200">
              Laptop Included
            </span>
          )}
          {scholarship.coverage.stipend && (
            <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-emerald-50 text-emerald-800">
              Living Stipend
            </span>
          )}
        </div>

        {/* Academic & Service Obligation Summary */}
        <div className="text-[11px] text-neutral-600 bg-neutral-50 border border-neutral-100 rounded-xl p-2.5 mb-3 space-y-1">
          <div className="line-clamp-2">
            <strong className="text-neutral-800">Criteria:</strong>{' '}
            {scholarship.academicRequirements}
          </div>
          <div className="text-neutral-500 flex items-center justify-between pt-1 border-t border-neutral-200/60">
            <span>
              <strong className="text-neutral-700">Obligation:</strong>{' '}
              {scholarship.serviceObligation}
            </span>
          </div>
        </div>

        {/* Eligibility Warning or Match Banner */}
        {scholarship.matchWarnings && scholarship.matchWarnings.length > 0 ? (
          <div className="text-[11px] py-1.5 px-2.5 bg-amber-50 border border-amber-200/80 text-amber-900 rounded-lg mb-3 flex items-start gap-1.5">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-700 shrink-0 mt-0.5" />
            <span className="line-clamp-1">{scholarship.matchWarnings[0]}</span>
          </div>
        ) : (
          meetsMarks && (
            <div className="text-[11px] py-1.5 px-2.5 bg-emerald-50/70 border border-emerald-200/70 text-emerald-900 rounded-lg mb-3 flex items-center justify-between">
              <span className="font-medium">Academic Eligibility:</span>
              <span className="font-bold">You Qualify ({studentMarks}% Avg)</span>
            </div>
          )
        )}
      </div>

      <div>
        {/* Value & Deadline Strip */}
        <div className="pt-3 border-t border-neutral-100 flex items-center justify-between gap-2 mb-3.5">
          <div>
            <div className="text-[10px] text-neutral-400 uppercase font-mono">Annual Value</div>
            <div className="text-sm font-bold text-neutral-950 font-mono">
              R {scholarship.amount.toLocaleString('en-ZA')}{' '}
              <span className="text-[11px] font-sans font-normal text-neutral-500">/ yr</span>
            </div>
          </div>

          <div className="text-right">
            <div className="text-[10px] text-neutral-400 uppercase font-mono">Closing Date</div>
            <div
              className={`text-xs font-semibold font-mono ${
                isExpired
                  ? 'text-neutral-400'
                  : isExpiringSoon
                  ? 'text-amber-700'
                  : 'text-neutral-800'
              }`}
            >
              {scholarship.deadline}{' '}
              {daysLeft > 0 && (
                <span className="text-[11px] font-normal text-neutral-500">({daysLeft}d)</span>
              )}
            </div>
          </div>
        </div>

        {/* Action Buttons: Guided Smart Apply + Direct Portal + Save + Compare + Bell */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => onOpenSmartApply(scholarship)}
            className="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-emerald-900 rounded-xl transition-colors whitespace-nowrap"
            title="Launch 4-step Guided Application & Motivation Letter Studio"
          >
            <span>Guided Apply</span>
          </button>

          <a
            href={sanitizePortalUrl(scholarship.portalUrl)}
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 rounded-xl border border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-100 transition-colors"
            title="Open official external provider portal directly"
            aria-label="Open external provider portal"
          >
            <ExternalLink className="w-4 h-4" />
          </a>

          <button
            onClick={() => onToggleSave(scholarship)}
            className={`p-2 rounded-xl border transition-colors ${
              isSaved
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                : 'bg-white text-neutral-700 border-neutral-200 hover:bg-neutral-50'
            }`}
            title={isSaved ? 'Saved in Tracker' : 'Save to Application Tracker'}
            aria-label="Save to Application Tracker"
          >
            {isSaved ? <BookmarkCheck className="w-4 h-4" /> : <Bookmark className="w-4 h-4" />}
          </button>

          <button
            onClick={() => onToggleCompare(scholarship.id)}
            className={`p-2 rounded-xl border transition-colors ${
              isCompared
                ? 'bg-neutral-900 text-white border-neutral-900'
                : 'bg-white text-neutral-700 border-neutral-200 hover:bg-neutral-50'
            }`}
            title="Compare side-by-side"
            aria-label="Compare bursary"
          >
            <Scale className="w-4 h-4" />
          </button>

          <button
            onClick={() => onOpenReminderModal(scholarship)}
            className="p-2 rounded-xl border border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-50 transition-colors"
            title="Schedule Automated Deadline Alerts"
            aria-label="Schedule Deadline Reminder"
          >
            <Bell className="w-4 h-4" />
          </button>
        </div>
      </div>
    </article>
  );
};
