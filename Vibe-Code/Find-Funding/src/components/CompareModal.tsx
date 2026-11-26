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
// SIDE-BY-SIDE COMPARISON MODAL
// ============================================================================

interface CompareModalProps {
  isOpen: boolean;
  onClose: () => void;
  scholarships: Scholarship[];
  profile: StudentProfile;
  onRemoveCompare: (id: string) => void;
  onOpenSmartApply: (sch: Scholarship) => void;
}

export const CompareModal: React.FC<CompareModalProps> = ({
  isOpen,
  onClose,
  scholarships,
  profile,
  onRemoveCompare,
  onOpenSmartApply,
}) => {
  if (!isOpen || scholarships.length === 0) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-neutral-950/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl w-full max-w-5xl shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-neutral-200">
          <div className="flex items-center gap-2">
            <Scale className="w-4 h-4 text-emerald-700" />
            <h3 className="text-base font-bold text-neutral-950">
              Side-by-Side South African Bursary Comparison
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-neutral-200">
                <th className="py-3 px-3 text-neutral-500 font-mono uppercase w-40">Attribute</th>
                {scholarships.map((s) => (
                  <th key={s.id} className="py-3 px-4 min-w-[220px] align-top">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <span className="text-[11px] font-mono text-emerald-700 font-bold">
                          {s.matchScore}% Match
                        </span>
                        <div className="text-sm font-bold text-neutral-950 mt-0.5">{s.title}</div>
                        <div className="text-xs font-normal text-neutral-500">{s.provider}</div>
                      </div>
                      <button
                        onClick={() => onRemoveCompare(s.id)}
                        className="text-neutral-400 hover:text-red-600"
                        title="Remove from comparison"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">Annual Package Value</td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4 font-mono font-bold text-neutral-950">
                    R {s.amount.toLocaleString('en-ZA')} / yr
                    <div className="text-[11px] font-sans font-normal text-neutral-500 mt-0.5">
                      {s.amountDisplay}
                    </div>
                  </td>
                ))}
              </tr>
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">
                  Min Marks & APS (Yours: {profile.marksPercentage}%)
                </td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4 font-mono">
                    Min {s.minMarks}% · APS {s.minAPS}
                    {s.requiresPureMath && (
                      <div className="text-[11px] text-amber-800 mt-0.5">
                        Requires Pure Maths ({s.minMathMark || 60}%+)
                      </div>
                    )}
                  </td>
                ))}
              </tr>
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">Coverage Breakdown</td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4 space-y-1">
                    <div>Tuition: {s.coverage.tuition ? '✅ Yes' : '❌ No'}</div>
                    <div>Residence: {s.coverage.accommodation ? '✅ Yes' : '❌ No'}</div>
                    <div>Laptop: {s.coverage.laptop ? '✅ Included' : '—'}</div>
                    <div>Cash Stipend: {s.coverage.stipend ? '✅ Included' : '—'}</div>
                  </td>
                ))}
              </tr>
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">
                  Service / Work-Back Obligation
                </td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4 text-neutral-700">
                    {s.serviceObligation}
                  </td>
                ))}
              </tr>
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">Closing Deadline</td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4 font-mono font-semibold text-neutral-900">
                    {s.deadline} ({getDaysRemaining(s.deadline)}d left)
                  </td>
                ))}
              </tr>
              <tr>
                <td className="py-3 px-3 font-semibold text-neutral-700">Action</td>
                {scholarships.map((s) => (
                  <td key={s.id} className="py-3 px-4">
                    <button
                      onClick={() => onOpenSmartApply(s)}
                      className="w-full py-2 px-3 text-xs font-bold text-white bg-neutral-950 hover:bg-emerald-900 rounded-xl transition-colors"
                    >
                      Launch Guided Apply
                    </button>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
