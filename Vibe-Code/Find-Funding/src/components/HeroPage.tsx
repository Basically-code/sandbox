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
// VIEW 1: HERO PAGE WITH QUICK ELIGIBILITY & SMART APPLY LAUNCHPAD
// ============================================================================

interface HeroPageProps {
  scholarships: Scholarship[];
  profile: StudentProfile;
  cmsConfig?: WebsiteCMSConfig;
  readinessScore: number;
  onNavigateToBursaries: (categoryFilter?: string) => void;
  onNavigateToMarks: () => void;
  onNavigateToRecommendations: () => void;
  onNavigateToTracker: () => void;
  onOpenSmartApply: (sch: Scholarship) => void;
  onOpenProfile: () => void;
}

export const HeroPage: React.FC<HeroPageProps> = ({
  scholarships,
  profile,
  cmsConfig,
  readinessScore,
  onNavigateToBursaries,
  onNavigateToMarks,
  onNavigateToRecommendations,
  onNavigateToTracker,
  onOpenSmartApply,
  onOpenProfile,
}) => {
  const qualifiedCount = scholarships.filter(
    (s) =>
      profile.marksPercentage >= s.minMarks &&
      (!s.requiresPureMath || profile.mathStream === 'pure_math')
  ).length;

  const topThreeMatches = [...scholarships]
    .sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
    .slice(0, 3);

  const saCategories = [
    {
      id: 'engineering',
      name: 'Engineering, Energy & Mining',
      count: 'Sasol, Eskom, Anglo American, SANRAL',
      description: 'Chemical, Mechanical, Civil, Electrical, Mining & Metallurgy degrees',
      tag: 'Pure Maths 65–70%+',
      accent: 'border-amber-200 hover:border-amber-400 bg-amber-50/40',
    },
    {
      id: 'commerce',
      name: 'Commerce, CA(SA) & Banking',
      count: 'Standard Bank, Allan Gray, Absa, Investec',
      description: 'Chartered Accounting, Actuarial Science, Quantitative Finance & Economics',
      tag: 'From 65% aggregate',
      accent: 'border-emerald-200 hover:border-emerald-400 bg-emerald-50/40',
    },
    {
      id: 'tech',
      name: 'Information Tech, AI & Data',
      count: 'Vodacom Foundation, Standard Bank, Absa',
      description: 'Computer Science, Software Engineering, Cyber Security & Data Science',
      tag: 'Includes Laptop + Stipend',
      accent: 'border-blue-200 hover:border-blue-400 bg-blue-50/40',
    },
    {
      id: 'education',
      name: 'Teaching & Education (DBE)',
      count: 'Funza Lushaka National Scheme',
      description: 'B.Ed & PGCE in STEM, Accounting & Foundation Phase priority subjects',
      tag: 'Guaranteed School Placement',
      accent: 'border-purple-200 hover:border-purple-400 bg-purple-50/40',
    },
    {
      id: 'health',
      name: 'Health Sciences & Missing Middle',
      count: 'ISFAP, Motsepe Foundation',
      description: 'Medicine (MBChB), Pharmacy, Allied Health & Scarce-Skills Professions',
      tag: 'Household R0 – R600k',
      accent: 'border-rose-200 hover:border-rose-400 bg-rose-50/40',
    },
    {
      id: 'tvet',
      name: 'TVET College & Artisan Trades',
      count: 'NSFAS TVET & National Skills Fund (NSF)',
      description: 'Boilermaking, Electrical, Millwright, Plumbing & Mechatronics (NCV/NATED)',
      tag: 'Includes Trade Toolkits',
      accent: 'border-orange-200 hover:border-orange-400 bg-orange-50/40',
    },
  ];

  return (
    <div className="space-y-14 pb-16">
      {/* Hero Section */}
      <section className="pt-4 sm:pt-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          <div className="lg:col-span-7 space-y-6">
            <div className="inline-flex items-center gap-2.5 px-3 py-1.5 rounded-full bg-emerald-50 border border-emerald-200">
              <img
                src="/assets/aistudio/Screenshot_2026-10-23_230324_1ed49d35.png"
                alt="Find Funding Emblem"
                className="w-4 h-4 object-contain rounded-sm"
              />
              <span className="text-xs font-bold tracking-wider uppercase text-emerald-900">
                {cmsConfig?.heroBadgeText || 'Find Funding South Africa'}
              </span>
              <span className="text-emerald-300">|</span>
              <span className="text-xs text-emerald-800 font-mono">
                {cmsConfig?.heroCycleText || '2026 / 2027 Intake Ready'}
              </span>
            </div>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-neutral-950 leading-[1.08]">
              {cmsConfig?.heroHeadlinePrefix || 'A smarter way to match, prepare &'}{' '}
              <span className="italic font-serif text-emerald-800 underline decoration-emerald-300 decoration-wavy decoration-2">
                {cmsConfig?.heroHeadlineHighlight || 'win'}
              </span>{' '}
              {cmsConfig?.heroHeadlineSuffix || 'South African bursaries.'}
            </h1>

            <p className="text-base sm:text-lg text-neutral-600 leading-relaxed max-w-2xl">
              {cmsConfig?.heroSubheadline ||
                'Don’t just click blind external links. Calculate your official NSC APS & subject eligibility, verify your certified SAPS document vault, generate a tailored motivation letter for each sponsor, and track every deadline to graduation.'}
            </p>

            {/* Simple Marks & Eligibility Bar */}
            <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="text-xs font-mono uppercase tracking-wider text-neutral-500 font-semibold">
                  Your Current Marks {profile.fullName ? `(${profile.fullName})` : ''}
                </div>
                <div className="flex flex-wrap items-center gap-2 text-xs sm:text-sm font-semibold text-neutral-900">
                  <span className="px-2.5 py-0.5 rounded-md bg-emerald-100 text-emerald-900 font-mono">
                    {profile.marksPercentage}% Average
                  </span>
                  <span className="text-neutral-600">
                    You qualify for <strong className="text-emerald-800">{qualifiedCount}</strong> of{' '}
                    {scholarships.length} bursaries
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={onNavigateToMarks}
                  className="px-3.5 py-2 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors whitespace-nowrap"
                >
                  Change Marks
                </button>
                <button
                  onClick={onOpenProfile}
                  className="px-3.5 py-2 text-xs font-semibold text-neutral-800 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-xl transition-colors whitespace-nowrap"
                >
                  My Profile
                </button>
              </div>
            </div>

            {/* Primary CTAs */}
            <div className="pt-1 flex flex-wrap items-center gap-3">
              <button
                onClick={() => onNavigateToBursaries()}
                className="inline-flex items-center gap-2.5 px-6 py-3.5 text-sm font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl transition-all shadow-md"
              >
                <span>Explore {scholarships.length} Bursaries</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <button
                onClick={onNavigateToRecommendations}
                className="inline-flex items-center gap-2 px-5 py-3.5 text-sm font-semibold text-emerald-950 bg-emerald-100 hover:bg-emerald-200 border border-emerald-300 rounded-xl transition-colors"
              >
                <Sparkles className="w-4 h-4 text-emerald-700" />
                <span>View My Top Matches</span>
              </button>

              <button
                onClick={onNavigateToTracker}
                className="inline-flex items-center gap-2 px-5 py-3.5 text-sm font-medium text-neutral-800 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-xl transition-colors"
              >
                <FileCheck className="w-4 h-4 text-neutral-600" />
                <span>My Applications</span>
              </button>
            </div>
          </div>

          {/* Right Column: Visual Photo Collage */}
          <div className="lg:col-span-5 relative">
            <div className="grid grid-cols-2 gap-3.5 relative">
              <div className="relative group col-span-2 sm:col-span-1 rounded-2xl overflow-hidden shadow-lg border-2 border-white aspect-[4/5] bg-neutral-100">
                <img
                  src="/assets/aistudio/oluwaseyi-akinlolu-dPQBwZ6d-NU-unsplash_ff588ebd.jpg"
                  alt="South African university student"
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-neutral-950/85 via-transparent to-transparent flex flex-col justify-end p-4 text-white">
                  <span className="text-[10px] uppercase font-mono tracking-wider text-emerald-300">
                    Undergraduate & TVET
                  </span>
                  <p className="text-xs font-semibold leading-snug">
                    Full tuition, residence, laptop & monthly living allowances
                  </p>
                </div>
              </div>

              <div className="col-span-2 sm:col-span-1 flex flex-col gap-3.5">
                <div className="relative group rounded-2xl overflow-hidden shadow-md border-2 border-white aspect-[4/3] bg-neutral-100">
                  <img
                    src="/assets/aistudio/rut-miit-oTglG1D4hRA-unsplash_f39afdd5.jpg"
                    alt="University graduation mortarboard"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-neutral-950/75 via-transparent to-transparent flex items-end p-3 text-white">
                    <span className="text-xs font-medium">From Grade 12 to PhD</span>
                  </div>
                </div>

                <div className="relative group rounded-2xl overflow-hidden shadow-md border-2 border-white aspect-[4/3] bg-neutral-100">
                  <img
                    src="/assets/aistudio/pang-yuhao-_kd5cxwZOK4-unsplash_878d3efe.jpg"
                    alt="Graduates celebrating success at sunset"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-neutral-950/75 via-transparent to-transparent flex items-end p-3 text-white">
                    <span className="text-xs font-medium">Step-by-step application help</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-3 sm:mt-0 sm:absolute sm:-bottom-4 sm:left-4 bg-white/95 backdrop-blur-md border border-neutral-200 rounded-xl p-3 shadow-xl max-w-[240px]">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                <span className="text-xs font-bold text-neutral-900">
                  Application Helper Ready
                </span>
              </div>
              <p className="text-[11px] text-neutral-500 mt-0.5 font-mono">
                Documents list & motivation letter included
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Top 3 Instant Matches Preview Strip */}
      <section className="bg-white border border-neutral-200 rounded-2xl p-6 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <span className="text-xs font-mono uppercase tracking-wider text-emerald-800 font-bold">
              Fast-Track Application Launchpad
            </span>
            <h2 className="text-xl font-bold text-neutral-950 mt-0.5">
              Your Top 3 Bursary Matches Right Now
            </h2>
          </div>
          <button
            onClick={onNavigateToRecommendations}
            className="text-xs font-semibold text-emerald-800 hover:text-emerald-950 inline-flex items-center gap-1 self-start sm:self-auto"
          >
            <span>See all {scholarships.length} compatibility scores</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {topThreeMatches.map((sch) => (
            <div
              key={sch.id}
              className="border border-neutral-200 rounded-xl p-4 bg-neutral-50/60 hover:bg-white hover:border-neutral-300 transition-all flex flex-col justify-between gap-3"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-1.5">
                  <span className="text-[11px] font-mono font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-900">
                    {sch.matchScore}% Match
                  </span>
                  <span className="text-[11px] font-mono text-neutral-500">
                    Closes {sch.deadline}
                  </span>
                </div>
                <h3 className="text-sm font-bold text-neutral-950 line-clamp-1">{sch.title}</h3>
                <p className="text-xs text-neutral-500 mt-0.5">{sch.provider}</p>
                <p className="text-xs font-semibold text-neutral-900 mt-2 font-mono">
                  {sch.amountDisplay}
                </p>
              </div>
              <div className="pt-2 border-t border-neutral-200/70 flex items-center gap-2">
                <button
                  onClick={() => onOpenSmartApply(sch)}
                  className="flex-1 py-2 px-3 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-lg transition-colors inline-flex items-center justify-center gap-1.5"
                >
                  <span>Guided Apply</span>
                </button>
                <a
                  href={sanitizePortalUrl(sch.portalUrl)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2 text-neutral-600 hover:text-neutral-950 bg-white border border-neutral-200 rounded-lg"
                  title="Open External Portal Directly"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Browse by Key South African Disciplines */}
      <section className="space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <span className="text-xs font-semibold text-emerald-700 uppercase tracking-wider font-mono">
              Explore by Discipline
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-neutral-950 mt-1">
              South African Funding Streams
            </h2>
            <p className="text-sm text-neutral-600 mt-1">
              Select a stream to filter bursaries by academic subject rules, coverage, and closing dates.
            </p>
          </div>
          <button
            onClick={() => onNavigateToBursaries()}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-900 hover:text-neutral-700"
          >
            <span>Open Full Directory</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {saCategories.map((cat) => (
            <div
              key={cat.id}
              onClick={() => onNavigateToBursaries(cat.id)}
              className={`border rounded-2xl p-5 cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5 group flex flex-col justify-between ${cat.accent}`}
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <h3 className="font-bold text-neutral-900 text-base">{cat.name}</h3>
                  <span className="text-[11px] font-mono px-2 py-0.5 rounded-md bg-white border border-neutral-200 text-neutral-700 shrink-0">
                    {cat.tag}
                  </span>
                </div>
                <p className="text-xs font-semibold text-neutral-800 mb-1">
                  Sponsors: {cat.count}
                </p>
                <p className="text-xs text-neutral-600 leading-relaxed">{cat.description}</p>
              </div>
              <div className="pt-4 mt-4 border-t border-neutral-200/60 flex items-center justify-between text-xs font-semibold text-neutral-900">
                <span>Filter {cat.name}</span>
                <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Why Our Guided Apply Workflow Works Better */}
      <section className="bg-neutral-900 text-white rounded-2xl p-6 sm:p-10 space-y-8">
        <div className="max-w-2xl space-y-2">
          <span className="text-xs font-mono uppercase tracking-widest text-emerald-400">
            Built for Higher Acceptance Rates
          </span>
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
            Why 68% of Bursary Applications Get Rejected — And How We Fix It
          </h2>
          <p className="text-xs sm:text-sm text-neutral-300 leading-relaxed">
            Most South African students lose out on funding not because of low intelligence, but due
            to missing subject prerequisites (like applying for STEM with Maths Literacy), expired
            certified documents, or generic motivation letters.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
          <div className="bg-neutral-800/70 border border-neutral-700 rounded-xl p-4 space-y-2">
            <div className="text-xs font-mono text-emerald-400 font-bold">STEP 01</div>
            <h3 className="text-sm font-bold">Check Your Marks</h3>
            <p className="text-xs text-neutral-300 leading-relaxed">
              See right away if your school or university marks and subjects (like Pure Maths) meet
              the bursary requirements.
            </p>
          </div>
          <div className="bg-neutral-800/70 border border-neutral-700 rounded-xl p-4 space-y-2">
            <div className="text-xs font-mono text-emerald-400 font-bold">STEP 02</div>
            <h3 className="text-sm font-bold">Certified Documents List</h3>
            <p className="text-xs text-neutral-300 leading-relaxed">
              Check off the copies you need stamped at the police station or post office (ID,
              results, and family income proof).
            </p>
          </div>
          <div className="bg-neutral-800/70 border border-neutral-700 rounded-xl p-4 space-y-2">
            <div className="text-xs font-mono text-emerald-400 font-bold">STEP 03</div>
            <h3 className="text-sm font-bold">Motivation Letter Helper</h3>
            <p className="text-xs text-neutral-300 leading-relaxed">
              Writes a clear motivation letter for you using your marks and course so you don’t have
              to start from a blank page.
            </p>
          </div>
          <div className="bg-neutral-800/70 border border-neutral-700 rounded-xl p-4 space-y-2">
            <div className="text-xs font-mono text-emerald-400 font-bold">STEP 04</div>
            <h3 className="text-sm font-bold">Apply & Get Reminders</h3>
            <p className="text-xs text-neutral-300 leading-relaxed">
              Opens the official bursary website and sends you email alerts 7 days, 3 days, and 1
              day before applications close.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};
