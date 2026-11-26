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
// GUIDED SMART APPLY STUDIO MODAL (4-Step Better Way to Apply)
// ============================================================================

interface SmartApplyModalProps {
  scholarship: Scholarship;
  profile: StudentProfile;
  step: number;
  setStep: (s: number) => void;
  draftMotivation: string;
  setDraftMotivation: (m: string) => void;
  checkedDocs: string[];
  setCheckedDocs: (d: string[]) => void;
  onRegenerateMotivation: () => void;
  onCopyMotivation: () => void;
  onClose: () => void;
  onFinalize: (status: ApplicationStatus, openPortal: boolean) => void;
}

export const SmartApplyModal: React.FC<SmartApplyModalProps> = ({
  scholarship,
  profile,
  step,
  setStep,
  draftMotivation,
  setDraftMotivation,
  checkedDocs,
  setCheckedDocs,
  onRegenerateMotivation,
  onCopyMotivation,
  onClose,
  onFinalize,
}) => {
  const toggleDoc = (docName: string) => {
    if (checkedDocs.includes(docName)) {
      setCheckedDocs(checkedDocs.filter((d) => d !== docName));
    } else {
      setCheckedDocs([...checkedDocs, docName]);
    }
  };

  const steps = [
    { num: 1, title: '1. Eligibility Check' },
    { num: 2, title: '2. Certified Docs' },
    { num: 3, title: '3. Motivation Builder' },
    { num: 4, title: '4. Portal Launch' },
  ];

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-neutral-950/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="bg-neutral-950 text-white p-5 flex items-start justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-1.5 text-[11px] font-mono uppercase tracking-wider text-emerald-400">
              <span>Application Helper · {scholarship.matchScore}% Match</span>
            </div>
            <h3 className="text-lg font-bold mt-0.5">{scholarship.title}</h3>
            <p className="text-xs text-neutral-400">
              {scholarship.provider} · Deadline: {scholarship.deadline}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-white rounded-lg hover:bg-neutral-800"
            aria-label="Close Guided Apply Studio"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Step Indicator Bar */}
        <div className="bg-neutral-100 px-5 py-2.5 border-b border-neutral-200 grid grid-cols-4 gap-2">
          {steps.map((s) => (
            <button
              key={s.num}
              type="button"
              onClick={() => setStep(s.num)}
              className={`py-1.5 px-2 rounded-lg text-[11px] font-mono font-bold text-center transition-colors ${
                step === s.num
                  ? 'bg-neutral-950 text-white'
                  : step > s.num
                  ? 'bg-emerald-100 text-emerald-900'
                  : 'text-neutral-500 hover:bg-neutral-200'
              }`}
            >
              {s.title}
            </button>
          ))}
        </div>

        {/* Body Content */}
        <div className="p-6 space-y-5 max-h-[68vh] overflow-y-auto text-xs">
          {step === 1 && (
            <div className="space-y-4">
              <div className="p-4 bg-neutral-50 border border-neutral-200 rounded-xl space-y-2">
                <div className="font-bold text-neutral-900 text-sm">
                  Step 1: Live Academic & Subject Audit
                </div>
                <p className="text-neutral-600 leading-relaxed">
                  Before spending time filling out {scholarship.provider}’s external portal, verify
                  how your profile stacks up against their selection criteria:
                </p>
                <div className="pt-2 space-y-2">
                  {scholarship.matchReasons?.map((r, i) => (
                    <div
                      key={i}
                      className="flex items-start gap-2 p-2 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-950"
                    >
                      <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
                      <span>{r}</span>
                    </div>
                  ))}
                  {scholarship.matchWarnings?.map((w, i) => (
                    <div
                      key={i}
                      className="flex items-start gap-2 p-2 bg-amber-50 border border-amber-200 rounded-lg text-amber-950"
                    >
                      <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                      <span>{w}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="p-3.5 border border-neutral-200 rounded-xl">
                  <div className="text-[11px] font-mono text-neutral-500 uppercase">
                    Package Coverage
                  </div>
                  <div className="font-bold text-neutral-900 text-sm mt-0.5">
                    {scholarship.amountDisplay}
                  </div>
                </div>
                <div className="p-3.5 border border-neutral-200 rounded-xl">
                  <div className="text-[11px] font-mono text-neutral-500 uppercase">
                    Work-Back / Service Obligation
                  </div>
                  <div className="font-bold text-neutral-900 text-sm mt-0.5">
                    {scholarship.serviceObligation}
                  </div>
                </div>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4">
              <div>
                <h4 className="text-sm font-bold text-neutral-950">
                  Step 2: Required Supporting Documents Checklist
                </h4>
                <p className="text-neutral-600 mt-0.5">
                  South African bursary administrators automatically disqualify incomplete uploads.
                  Tick off each document you have certified (less than 3 months old) and scanned:
                </p>
              </div>

              <div className="space-y-2">
                {scholarship.requiredDocuments.map((docName, idx) => {
                  const isChecked = checkedDocs.includes(docName);
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => toggleDoc(docName)}
                      className={`w-full p-3.5 rounded-xl border text-left flex items-center justify-between gap-3 transition-colors ${
                        isChecked
                          ? 'bg-emerald-50 border-emerald-300 text-emerald-950 font-semibold'
                          : 'bg-white border-neutral-200 text-neutral-700 hover:bg-neutral-50'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-4 h-4 rounded flex items-center justify-center ${
                            isChecked
                              ? 'bg-emerald-700 text-white'
                              : 'border border-neutral-300 bg-white'
                          }`}
                        >
                          {isChecked && <Check className="w-3 h-3" />}
                        </div>
                        <span>{docName}</span>
                      </div>
                      <span className="text-[10px] font-mono uppercase px-2 py-0.5 rounded bg-white border border-neutral-200">
                        {isChecked ? 'Ready' : 'Pending'}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-sm font-bold text-neutral-950">
                    Step 3: Tailored Motivation Statement Builder
                  </h4>
                  <p className="text-neutral-600">
                    Pre-filled with your {profile.marksPercentage}% mark, APS {profile.matricAPS},
                    financial background, and {scholarship.provider}’s focus. Edit freely and copy
                    for the portal:
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={onRegenerateMotivation}
                    className="px-2.5 py-1.5 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg"
                  >
                    Reset Template
                  </button>
                  <button
                    type="button"
                    onClick={onCopyMotivation}
                    className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-bold text-white bg-emerald-700 hover:bg-emerald-600 rounded-lg"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy Letter</span>
                  </button>
                </div>
              </div>

              <textarea
                value={draftMotivation}
                onChange={(e) => setDraftMotivation(e.target.value)}
                rows={11}
                className="w-full p-3.5 border border-neutral-300 rounded-xl text-xs font-mono text-neutral-900 leading-relaxed focus:outline-none focus:border-neutral-950"
              />
            </div>
          )}

          {step === 4 && (
            <div className="space-y-4">
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-bold text-emerald-950">
                    Step 4: Pre-Flight Summary & Official Portal Handoff
                  </span>
                  <span className="font-mono text-xs font-bold text-emerald-800">
                    {checkedDocs.length}/{scholarship.requiredDocuments.length} Docs Checked
                  </span>
                </div>
                <p className="text-emerald-900 leading-relaxed">
                  Your tailored motivation letter is saved to your tracker, and automated 7d, 3d,
                  and 24h email alerts will be activated for <strong>{profile.email}</strong>.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => onFinalize('preparing', false)}
                  className="p-4 border border-neutral-300 rounded-xl text-left hover:bg-neutral-50 transition-colors space-y-1"
                >
                  <div className="font-bold text-neutral-950 text-sm flex items-center gap-1.5">
                    <BookmarkCheck className="w-4 h-4 text-emerald-700" />
                    <span>Save Prep Package for Later</span>
                  </div>
                  <p className="text-neutral-500 text-[11px]">
                    Saves your motivation letter and document checklist under "Preparing Documents"
                    in your tracker.
                  </p>
                </button>

                <button
                  type="button"
                  onClick={() => onFinalize('submitted', true)}
                  className="p-4 bg-neutral-950 text-white rounded-xl text-left hover:bg-emerald-950 transition-colors space-y-1"
                >
                  <div className="font-bold text-sm flex items-center justify-between">
                    <span>Launch Official Portal & Track</span>
                    <ExternalLink className="w-4 h-4 text-emerald-400" />
                  </div>
                  <p className="text-neutral-300 text-[11px]">
                    Opens {scholarship.provider}’s application portal in a new tab and logs this
                    application in your pipeline.
                  </p>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer Navigation */}
        <div className="px-6 py-4 bg-neutral-50 border-t border-neutral-200 flex items-center justify-between">
          <button
            type="button"
            disabled={step === 1}
            onClick={() => setStep(Math.max(1, step - 1))}
            className="inline-flex items-center gap-1 px-3.5 py-2 text-xs font-semibold text-neutral-700 disabled:opacity-40 hover:bg-neutral-200 rounded-xl"
          >
            <ChevronLeft className="w-4 h-4" />
            <span>Previous Step</span>
          </button>

          {step < 4 ? (
            <button
              type="button"
              onClick={() => setStep(step + 1)}
              className="inline-flex items-center gap-1 px-5 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl"
            >
              <span>Next Step</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              type="button"
              onClick={() => onFinalize('preparing', true)}
              className="inline-flex items-center gap-1.5 px-5 py-2 text-xs font-bold text-white bg-emerald-700 hover:bg-emerald-600 rounded-xl"
            >
              <span>Save & Open Official Portal</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
