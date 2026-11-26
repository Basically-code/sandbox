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
import { ScholarshipCard } from './ScholarshipCard';

// ============================================================================
// VIEW 3: NSC APS & ACADEMIC MARKS ELIGIBILITY CALCULATOR
// ============================================================================

interface MarksAndAPSCalculatorViewProps {
  scholarships: Scholarship[];
  profile: StudentProfile;
  onUpdateProfile: (p: StudentProfile) => void;
  savedIds: string[];
  compareIds: string[];
  onToggleSave: (sch: Scholarship) => void;
  onToggleCompare: (id: string) => void;
  onOpenReminderModal: (sch: Scholarship) => void;
  onOpenSmartApply: (sch: Scholarship) => void;
  selectedStudyLevel: StudyLevel;
  onSelectStudyLevel: (lvl: StudyLevel) => void;
  onShowToast: (msg: string, type?: 'success' | 'info' | 'warning') => void;
}

export const MarksAndAPSCalculatorView: React.FC<MarksAndAPSCalculatorViewProps> = ({
  scholarships,
  profile,
  onUpdateProfile,
  savedIds,
  compareIds,
  onToggleSave,
  onToggleCompare,
  onOpenReminderModal,
  onOpenSmartApply,
  selectedStudyLevel,
  onSelectStudyLevel,
  onShowToast,
}) => {
  const [showOnlyEligible, setShowOnlyEligible] = useState<boolean>(true);
  const [showSubjectBreakdown, setShowSubjectBreakdown] = useState<boolean>(false);
  const [calculatorSubTab, setCalculatorSubTab] = useState<'aps' | 'nsfas_appeal'>('aps');
  const [appealReason, setAppealReason] = useState<string>('income_change');

  const handleSubjectMarkChange = (id: string, pct: number) => {
    const clamped = Math.min(100, Math.max(0, pct));
    const updatedSubjects = profile.subjects.map((sub) =>
      sub.id === id ? { ...sub, percentage: clamped } : sub
    );

    // Calculate APS from the 6 non-LO subjects
    const apsSubjects = updatedSubjects.filter((s) => s.countsForAPS);
    const totalAPS = apsSubjects.reduce((sum, s) => sum + getNSCLevel(s.percentage), 0);
    const avgPct = Math.round(
      apsSubjects.reduce((sum, s) => sum + s.percentage, 0) / (apsSubjects.length || 1)
    );

    const mathSub = updatedSubjects.find((s) => s.id === 'sub3');
    const sciSub = updatedSubjects.find((s) => s.id === 'sub4');
    const engSub = updatedSubjects.find((s) => s.id === 'sub1');

    onUpdateProfile({
      ...profile,
      subjects: updatedSubjects,
      matricAPS: totalAPS,
      marksPercentage: avgPct,
      mathMark: mathSub ? mathSub.percentage : profile.mathMark,
      scienceMark: sciSub ? sciSub.percentage : profile.scienceMark,
      englishMark: engSub ? engSub.percentage : profile.englishMark,
    });
  };

  const filteredScholarships = scholarships.filter((s) => {
    if (selectedStudyLevel !== 'all' && s.studyLevel !== selectedStudyLevel) return false;
    if (showOnlyEligible) {
      const marksOk = profile.marksPercentage >= s.minMarks;
      const mathOk = !s.requiresPureMath || profile.mathStream === 'pure_math';
      return marksOk && mathOk;
    }
    return true;
  });

  const eligibleCount = scholarships.filter(
    (s) =>
      (selectedStudyLevel === 'all' || s.studyLevel === selectedStudyLevel) &&
      profile.marksPercentage >= s.minMarks &&
      (!s.requiresPureMath || profile.mathStream === 'pure_math')
  ).length;

  return (
    <div className="space-y-8 pb-16">
      <div>
        <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 uppercase tracking-wider font-mono">
          <span>South Africa</span>
          <span className="text-neutral-300">·</span>
          <span>School & University Marks</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-950 mt-1">
          Check Your Marks
        </h1>
        <p className="text-sm text-neutral-600 mt-1 max-w-3xl leading-relaxed">
          Move the slider to your school or university average to see which bursaries you qualify
          for, or get help if NSFAS rejected your application.
        </p>

        {/* Clean 2-Mode Switcher */}
        <div className="flex flex-wrap items-center gap-2 pt-4">
          <button
            type="button"
            onClick={() => setCalculatorSubTab('aps')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-colors ${
              calculatorSubTab === 'aps'
                ? 'bg-neutral-950 text-white'
                : 'bg-white border border-neutral-200 text-neutral-600 hover:text-neutral-950'
            }`}
          >
            1. Filter by My Marks
          </button>
          <button
            type="button"
            onClick={() => setCalculatorSubTab('nsfas_appeal')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-colors ${
              calculatorSubTab === 'nsfas_appeal'
                ? 'bg-neutral-950 text-white'
                : 'bg-white border border-neutral-200 text-neutral-600 hover:text-neutral-950'
            }`}
          >
            2. Help with NSFAS Appeal
          </button>
        </div>
      </div>

      {/* Main Calculator Control Card */}
      <div className="bg-white border border-neutral-200 rounded-2xl p-6 shadow-xs space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
          {/* Aggregate Slider */}
          <div className="md:col-span-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-neutral-800 uppercase tracking-wider font-mono">
                Overall Academic Average
              </label>
              <span className="text-xl font-bold font-mono text-emerald-800">
                {profile.marksPercentage}%
              </span>
            </div>
            <input
              type="range"
              min="40"
              max="100"
              value={profile.marksPercentage}
              onChange={(e) =>
                onUpdateProfile({
                  ...profile,
                  marksPercentage: Number(e.target.value),
                  matricAPS: Math.min(42, Math.round((Number(e.target.value) / 100) * 42) + 4),
                })
              }
              className="w-full h-2.5 bg-neutral-200 rounded-lg appearance-none cursor-pointer accent-neutral-950"
            />
            <div className="flex justify-between text-[11px] text-neutral-400 font-mono">
              <span>50% NSFAS</span>
              <span>65% Corporate</span>
              <span>70% STEM</span>
              <span>80%+ Level 7</span>
            </div>
          </div>

          {/* Mathematics Stream Selector */}
          <div className="md:col-span-3 space-y-2">
            <label className="text-xs font-bold text-neutral-800 uppercase tracking-wider font-mono block">
              Mathematics Stream
            </label>
            <select
              value={profile.mathStream}
              onChange={(e) =>
                onUpdateProfile({ ...profile, mathStream: e.target.value as MathStream })
              }
              className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-xs font-semibold text-neutral-900 bg-white"
            >
              <option value="pure_math">Pure Mathematics (NSC)</option>
              <option value="technical_math">Technical Mathematics</option>
              <option value="math_lit">Mathematical Literacy</option>
            </select>
            <div className="text-[11px] text-neutral-500">
              STEM & CA(SA) bursaries require Pure Maths.
            </div>
          </div>

          {/* Study Level Selector */}
          <div className="md:col-span-3 space-y-2">
            <label className="text-xs font-bold text-neutral-800 uppercase tracking-wider font-mono block">
              Target Study Level
            </label>
            <select
              value={selectedStudyLevel}
              onChange={(e) => onSelectStudyLevel(e.target.value as StudyLevel)}
              className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-xs font-semibold text-neutral-900 bg-white"
            >
              <option value="all">All Study Levels</option>
              <option value="undergraduate">Undergraduate Degree/Diploma</option>
              <option value="tvet">TVET College (NCV/NATED)</option>
              <option value="matric">Matric / Grade 12</option>
              <option value="honours">Honours</option>
              <option value="masters">Master’s / PhD</option>
            </select>
            <div className="text-[11px] text-neutral-500">Filters matching qualification level.</div>
          </div>

          {/* Toggle Mode */}
          <div className="md:col-span-2 space-y-2">
            <button
              type="button"
              onClick={() => setShowOnlyEligible(!showOnlyEligible)}
              className={`w-full py-2.5 px-3 text-xs font-bold rounded-xl border transition-colors flex items-center justify-center gap-1.5 ${
                showOnlyEligible
                  ? 'bg-neutral-950 text-white border-neutral-950'
                  : 'bg-white text-neutral-700 border-neutral-300 hover:bg-neutral-50'
              }`}
            >
              <Filter className="w-3.5 h-3.5" />
              <span>{showOnlyEligible ? 'Eligible Only' : 'Showing All'}</span>
            </button>
            <button
              type="button"
              onClick={() => setShowSubjectBreakdown(!showSubjectBreakdown)}
              className="w-full py-1.5 px-2 text-[11px] font-semibold text-emerald-800 hover:underline text-center block"
            >
              {showSubjectBreakdown ? 'Hide Subject Marks' : 'Enter Subject Marks (Optional)'}
            </button>
          </div>
        </div>

        {/* Interactive 7-Subject NSC APS Calculator Grid */}
        {showSubjectBreakdown && (
          <div className="pt-5 border-t border-neutral-100 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-bold text-neutral-950">
                  Your 7 School Subjects (Optional)
                </h3>
                <p className="text-xs text-neutral-500">
                  Move the sliders for each subject if you want us to work out your average and admission points automatically.
                </p>
              </div>
              <span className="text-xs font-mono font-bold px-3 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-900">
                Admission Points: {profile.matricAPS} / 42
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {profile.subjects.map((sub) => {
                const level = getNSCLevel(sub.percentage);
                return (
                  <div
                    key={sub.id}
                    className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl space-y-2"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-semibold text-neutral-800 line-clamp-1">
                        {sub.name}
                      </span>
                      <span className="text-xs font-mono font-bold text-neutral-950 shrink-0">
                        {sub.percentage}% (L{level})
                      </span>
                    </div>
                    <input
                      type="range"
                      min="30"
                      max="100"
                      value={sub.percentage}
                      onChange={(e) => handleSubjectMarkChange(sub.id, Number(e.target.value))}
                      className="w-full h-1.5 bg-neutral-200 rounded-lg appearance-none cursor-pointer accent-emerald-700"
                    />
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Summary Metrics Bar */}
        <div className="pt-4 border-t border-neutral-100 grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div className="p-3.5 bg-emerald-50/70 border border-emerald-200 rounded-xl">
            <div className="text-neutral-600 font-medium">Bursaries You Qualify For</div>
            <div className="text-xl font-bold text-emerald-900 font-mono mt-0.5">
              {eligibleCount} of {scholarships.length} Bursaries
            </div>
            <div className="text-[11px] text-emerald-700 mt-0.5">
              Meets both aggregate ({profile.marksPercentage}%) and Maths stream criteria
            </div>
          </div>

          <div className="p-3.5 bg-neutral-50 border border-neutral-200 rounded-xl">
            <div className="text-neutral-600 font-medium">Your Admission Points (APS)</div>
            <div className="text-xl font-bold text-neutral-950 font-mono mt-0.5">
              {profile.matricAPS} / 42 Points
            </div>
            <div className="text-[11px] text-neutral-500 mt-0.5">
              Level {getNSCLevel(profile.marksPercentage)} Achievement Band
            </div>
          </div>

          <div className="p-3.5 bg-neutral-50 border border-neutral-200 rounded-xl">
            <div className="text-neutral-600 font-medium">Your Maths Choice</div>
            <div className="text-sm font-bold text-neutral-950 mt-1">
              {profile.mathStream === 'pure_math'
                ? `Pure Maths (${profile.mathMark}%) · Science (${profile.scienceMark}%)`
                : 'Maths Literacy / Tech Maths (Non-STEM eligible)'}
            </div>
            <div className="text-[11px] text-neutral-500 mt-0.5">
              {profile.mathStream !== 'pure_math'
                ? 'Switch to Pure Maths above to unlock STEM & Engineering bursaries'
                : 'Qualifies for Engineering, Commerce & Science tracks'}
            </div>
          </div>
        </div>

        {/* Uncluttered Priority Qualifiers Strip: SASSA Fast-Track, Disability R600k Cap & NBT Status */}
        <div className="pt-4 border-t border-neutral-100 grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
          <button
            type="button"
            onClick={() => {
              const nextVal = !profile.sassaBeneficiary;
              onUpdateProfile({ ...profile, sassaBeneficiary: nextVal });
              onShowToast(
                nextVal
                  ? 'SASSA Fast-Track enabled: Financial means-test automatically satisfied!'
                  : 'SASSA Fast-Track disabled.',
                'info'
              );
            }}
            className={`p-3 rounded-xl border text-left flex items-center justify-between transition-colors ${
              profile.sassaBeneficiary
                ? 'bg-emerald-50 border-emerald-300 text-emerald-950 font-semibold'
                : 'bg-white border-neutral-200 text-neutral-700 hover:bg-neutral-50'
            }`}
          >
            <div>
              <div className="font-bold">Receiving a SASSA Grant</div>
              <div className="text-[11px] text-neutral-500">Automatically qualifies for financial need</div>
            </div>
            <span className="font-mono text-[11px] px-2 py-0.5 rounded bg-white border border-neutral-200">
              {profile.sassaBeneficiary ? 'ACTIVE' : 'OFF'}
            </span>
          </button>

          <button
            type="button"
            onClick={() => {
              const nextVal = !profile.hasDisability;
              onUpdateProfile({ ...profile, hasDisability: nextVal });
              onShowToast(
                nextVal
                  ? 'Disability Support Mode active: Income threshold raised to R600,000 p.a. + N+2 rule!'
                  : 'Standard eligibility mode active.',
                'info'
              );
            }}
            className={`p-3 rounded-xl border text-left flex items-center justify-between transition-colors ${
              profile.hasDisability
                ? 'bg-emerald-50 border-emerald-300 text-emerald-950 font-semibold'
                : 'bg-white border-neutral-200 text-neutral-700 hover:bg-neutral-50'
            }`}
          >
            <div>
              <div className="font-bold">Student with a Disability</div>
              <div className="text-[11px] text-neutral-500">Qualifies if family income is under R600,000/yr</div>
            </div>
            <span className="font-mono text-[11px] px-2 py-0.5 rounded bg-white border border-neutral-200">
              {profile.hasDisability ? 'ACTIVE' : 'OFF'}
            </span>
          </button>

          <div className="p-3 rounded-xl border border-neutral-200 bg-white flex items-center justify-between gap-2">
            <div>
              <div className="font-bold text-neutral-900">University Entrance Test (NBT)</div>
              <div className="text-[11px] text-neutral-500">Only if your university asked for the NBT test</div>
            </div>
            <select
              value={profile.nbtStatus || 'booked_aql_mat'}
              onChange={(e) =>
                onUpdateProfile({ ...profile, nbtStatus: e.target.value as NBTStatus })
              }
              className="px-2.5 py-1.5 border border-neutral-300 rounded-lg text-xs font-semibold text-neutral-900 bg-neutral-50"
            >
              <option value="not_booked">Not Booked</option>
              <option value="booked_aql_mat">Booked (AQL/MAT)</option>
              <option value="completed">Completed</option>
            </select>
          </div>
        </div>

        {/* Conditional Sub-View: NSFAS Appeal & N+1 Rule Diagnostic Wizard */}
        {calculatorSubTab === 'nsfas_appeal' && (
          <div className="pt-5 border-t border-neutral-200 space-y-4">
            <div className="bg-neutral-950 text-white rounded-2xl p-5 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <span className="text-[11px] font-mono uppercase text-emerald-400 font-bold">
                    NSFAS Appeal & Study Years Guide
                  </span>
                  <h3 className="text-base font-bold mt-0.5">
                    Was Your NSFAS Application Rejected? Check What You Need to Appeal
                  </h3>
                  <p className="text-xs text-neutral-400 mt-0.5">
                    NSFAS normally funds your course length plus 1 extra year (for example, up to 4
                    years of funding for a 3-year course).
                  </p>
                </div>
                <a
                  href="https://www.nsfas.org.za/content/appeals.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold bg-white text-neutral-950 rounded-xl hover:bg-neutral-100 self-start sm:self-auto shrink-0"
                >
                  <span>Go to NSFAS Appeals Page</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                <div className="p-3.5 bg-neutral-900 border border-neutral-800 rounded-xl space-y-1.5">
                  <label className="text-neutral-400 font-mono block">
                    How Long Is Your Course?
                  </label>
                  <select
                    value={profile.degreeDurationYears || 3}
                    onChange={(e) =>
                      onUpdateProfile({
                        ...profile,
                        degreeDurationYears: Number(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white font-semibold"
                  >
                    <option value={3}>3-Year Course (Degree / Diploma)</option>
                    <option value={4}>4-Year Course (Engineering / Teaching)</option>
                    <option value={5}>5 to 6-Year Course (Medicine)</option>
                  </select>
                </div>

                <div className="p-3.5 bg-neutral-900 border border-neutral-800 rounded-xl space-y-1.5">
                  <label className="text-neutral-400 font-mono block">
                    How Many Years Has NSFAS Funded You?
                  </label>
                  <select
                    value={profile.yearsFundedSoFar ?? 0}
                    onChange={(e) =>
                      onUpdateProfile({
                        ...profile,
                        yearsFundedSoFar: Number(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white font-semibold"
                  >
                    <option value={0}>0 Years (First-Time Student)</option>
                    <option value={1}>1 Year Funded</option>
                    <option value={2}>2 Years Funded</option>
                    <option value={3}>3 Years Funded</option>
                    <option value={4}>4 Years Funded</option>
                    <option value={5}>5+ Years Funded</option>
                  </select>
                </div>

                <div className="p-3.5 bg-neutral-900 border border-neutral-800 rounded-xl flex flex-col justify-center">
                  {(() => {
                    const n = profile.degreeDurationYears || 3;
                    const bonus = profile.hasDisability ? 2 : 1;
                    const maxAllowed = n + bonus;
                    const used = profile.yearsFundedSoFar ?? 0;
                    const remaining = maxAllowed - used;
                    return remaining > 0 ? (
                      <>
                        <div className="text-emerald-400 font-mono font-bold text-sm">
                          ✅ {remaining} Funded Year{remaining > 1 ? 's' : ''} Still Available
                        </div>
                        <div className="text-[11px] text-neutral-400 mt-0.5">
                          NSFAS allows up to {maxAllowed} years in total for your course
                        </div>
                      </>
                    ) : (
                      <>
                        <div className="text-amber-400 font-mono font-bold text-sm">
                          ⚠️ Maximum NSFAS Years Reached ({used}/{maxAllowed} Yrs)
                        </div>
                        <div className="text-[11px] text-neutral-400 mt-0.5">
                          Try applying for the Feenix Debt Bursary instead
                        </div>
                      </>
                    );
                  })()}
                </div>
              </div>

              <div className="p-4 bg-neutral-900 border border-neutral-800 rounded-xl space-y-2 text-xs">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <span className="font-bold text-neutral-200">
                    Why Was Your NSFAS Application Rejected?
                  </span>
                  <select
                    value={appealReason}
                    onChange={(e) => setAppealReason(e.target.value)}
                    className="px-3 py-1.5 bg-neutral-800 border border-neutral-700 rounded-lg text-white font-medium"
                  >
                    <option value="income_change">Family income changed or listed over R350,000</option>
                    <option value="academic_failure">Passed fewer than 50% of modules last year</option>
                    <option value="sassa_Verify">SASSA or parent details did not match</option>
                  </select>
                </div>
                <div className="text-neutral-300 pt-1 leading-relaxed">
                  {appealReason === 'income_change' && (
                    <span>
                      <strong>Documents you need to upload:</strong> Latest payslip, retrenchment
                      letter if a parent lost their job, UIF paper, SARS tax letter, or a certified
                      death certificate if a breadwinner passed away.
                    </span>
                  )}
                  {appealReason === 'academic_failure' && (
                    <span>
                      <strong>Documents you need to upload:</strong> A signed letter from your
                      university faculty confirming you can still finish your course on time, or a
                      doctor’s letter if you were sick or had a family emergency.
                    </span>
                  )}
                  {appealReason === 'sassa_Verify' && (
                    <span>
                      <strong>Documents you need to upload:</strong> An official SASSA confirmation
                      letter (stamped within the last 3 months) or a police station affidavit
                      confirming who takes care of you.
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Matching Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredScholarships.map((sch) => (
          <ScholarshipCard
            key={sch.id}
            scholarship={sch}
            isSaved={savedIds.includes(sch.id)}
            isCompared={compareIds.includes(sch.id)}
            onToggleSave={onToggleSave}
            onToggleCompare={onToggleCompare}
            onOpenReminderModal={onOpenReminderModal}
            onOpenSmartApply={onOpenSmartApply}
            studentMarks={profile.marksPercentage}
          />
        ))}
      </div>
    </div>
  );
};
