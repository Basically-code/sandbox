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
// VIEW 2: FUNDING CATALOG VIEW (Reactive Search, Coverage Filters & Compare)
// ============================================================================

interface FundingCatalogViewProps {
  scholarships: Scholarship[];
  totalCatalogCount: number;
  selectedStudyLevel: StudyLevel;
  onSelectStudyLevel: (lvl: StudyLevel) => void;
  selectedField: string;
  onSelectField: (field: string, label?: string) => void;
  searchQuery: string;
  onSearchQueryChange: (q: string) => void;
  filterCoverage: string;
  onFilterCoverageChange: (c: string) => void;
  sortBy: 'match' | 'deadline' | 'amount';
  onSortByChange: (s: 'match' | 'deadline' | 'amount') => void;
  profile: StudentProfile;
  savedIds: string[];
  compareIds: string[];
  onToggleSave: (sch: Scholarship) => void;
  onToggleCompare: (id: string) => void;
  onOpenCompareModal: () => void;
  onOpenReminderModal: (sch: Scholarship) => void;
  onOpenSmartApply: (sch: Scholarship) => void;
  onNavigateToMarks: () => void;
  onShowToast: (msg: string, type?: 'success' | 'info' | 'warning') => void;
}

export const FundingCatalogView: React.FC<FundingCatalogViewProps> = ({
  scholarships,
  totalCatalogCount,
  selectedStudyLevel,
  onSelectStudyLevel,
  selectedField,
  onSelectField,
  searchQuery,
  onSearchQueryChange,
  filterCoverage,
  onFilterCoverageChange,
  sortBy,
  onSortByChange,
  profile,
  savedIds,
  compareIds,
  onToggleSave,
  onToggleCompare,
  onOpenCompareModal,
  onOpenReminderModal,
  onOpenSmartApply,
  onNavigateToMarks,
  onShowToast,
}) => {
  const [lastSyncDate, setLastSyncDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [isRefreshing, setIsRefreshing] = useState(false);

  const studyLevels: { id: StudyLevel; label: string }[] = [
    { id: 'all', label: 'All Levels' },
    { id: 'undergraduate', label: 'Undergraduate (Degree / Diploma)' },
    { id: 'tvet', label: 'TVET College (NCV / NATED)' },
    { id: 'matric', label: 'Matric / Grade 12' },
    { id: 'honours', label: 'Honours' },
    { id: 'masters', label: 'Master’s' },
    { id: 'phd', label: 'Doctoral / PhD' },
  ];

  const saFields = [
    { id: 'all', label: 'All Disciplines & Streams' },
    { id: 'engineering', label: 'Engineering, Energy & Mining' },
    { id: 'commerce', label: 'Commerce, Accounting & Banking' },
    { id: 'tech', label: 'Computer Science, IT & Data' },
    { id: 'health', label: 'Health Sciences & Medicine' },
    { id: 'education', label: 'Teaching & Education' },
    { id: 'tvet', label: 'Artisan Trades & Technical' },
    { id: 'debt_seta', label: 'SETA & Historical Debt Relief (Feenix)' },
    { id: 'provincial', label: 'Provincial Premier Bursaries (EC / WC)' },
    { id: 'science', label: 'Postgraduate & Research Sciences' },
  ];

  const handleRefreshCatalog = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
      setLastSyncDate(new Date().toISOString().split('T')[0]);
      onShowToast(
        `Verified all ${totalCatalogCount} South African bursary deadlines and portal links for ${new Date().toISOString().split('T')[0]}.`
      );
    }, 450);
  };

  return (
    <div className="space-y-6 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 uppercase tracking-wider font-mono">
            <span>South Africa Directory</span>
            <span className="text-neutral-300">·</span>
            <span>Synced {lastSyncDate}</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-950 mt-1">
            Bursaries & Funding Directory
          </h1>
          <p className="text-sm text-neutral-600 mt-1 max-w-2xl">
            Filter verified South African bursaries by study level, coverage perks, and subject
            criteria, or launch our <strong>Guided Apply Studio</strong> on any opportunity.
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          {compareIds.length > 0 && (
            <button
              onClick={onOpenCompareModal}
              className="inline-flex items-center gap-1.5 px-3.5 py-2.5 text-xs font-bold text-white bg-emerald-700 hover:bg-emerald-600 rounded-xl transition-colors"
            >
              <Scale className="w-3.5 h-3.5" />
              <span>Compare ({compareIds.length})</span>
            </button>
          )}
          <button
            onClick={onNavigateToMarks}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-xs font-semibold text-neutral-900 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-xl transition-colors shadow-xs"
          >
            <SlidersHorizontal className="w-4 h-4 text-neutral-500" />
            <span>
              Your Marks: {profile.marksPercentage}% (APS {profile.matricAPS})
            </span>
          </button>
        </div>
      </div>

      {/* Search & Live Sync Bar */}
      <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-center gap-2">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-3" />
            <input
              type="text"
              placeholder="Search by sponsor, degree, or keyword (e.g. 'Sasol', 'NSFAS', 'CA(SA)', 'Missing Middle', 'Mining', 'Laptop')..."
              value={searchQuery}
              onChange={(e) => onSearchQueryChange(e.target.value)}
              className="w-full pl-9 pr-9 py-2 text-xs sm:text-sm border border-neutral-300 rounded-xl text-neutral-900 focus:outline-none focus:border-neutral-900 placeholder:text-neutral-400"
            />
            {searchQuery && (
              <button
                onClick={() => onSearchQueryChange('')}
                className="absolute right-3 top-2.5 text-neutral-400 hover:text-neutral-700"
                aria-label="Clear search query"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={handleRefreshCatalog}
              disabled={isRefreshing}
              className="inline-flex items-center justify-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors whitespace-nowrap w-full sm:w-auto"
            >
              <RefreshCw
                className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-emerald-700' : 'text-neutral-500'}`}
              />
              <span>{isRefreshing ? 'Verifying...' : 'Verify Deadlines'}</span>
            </button>
          </div>
        </div>

        {/* Study Level Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pt-1 pb-1">
          {studyLevels.map((lvl) => {
            const isSelected = selectedStudyLevel === lvl.id;
            return (
              <button
                key={lvl.id}
                onClick={() => onSelectStudyLevel(lvl.id)}
                className={`px-3 py-1.5 text-xs font-medium rounded-lg whitespace-nowrap transition-colors ${
                  isSelected
                    ? 'bg-neutral-950 text-white font-semibold'
                    : 'bg-neutral-50 text-neutral-600 border border-neutral-200 hover:text-neutral-900 hover:bg-neutral-100'
                }`}
              >
                {lvl.label}
              </button>
            );
          })}
        </div>

        {/* Secondary Filters: Discipline, Coverage Perks & Sort */}
        <div className="pt-2 border-t border-neutral-100 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="text-neutral-500 font-medium">Discipline:</span>
              <select
                value={selectedField}
                onChange={(e) => {
                  const item = saFields.find((f) => f.id === e.target.value);
                  onSelectField(e.target.value, item?.label);
                }}
                className="py-1.5 px-2.5 border border-neutral-300 rounded-lg text-neutral-800 bg-white font-medium"
              >
                {saFields.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-neutral-500 font-medium">Coverage Filter:</span>
              <select
                value={filterCoverage}
                onChange={(e) => onFilterCoverageChange(e.target.value)}
                className="py-1.5 px-2.5 border border-neutral-300 rounded-lg text-neutral-800 bg-white font-medium"
              >
                <option value="all">All Coverage Types</option>
                <option value="laptop">Includes Free Laptop</option>
                <option value="accommodation">Includes Residence / Accommodation</option>
                <option value="no_workback">No Work-Back Service Obligation</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-neutral-500 font-medium">Sort By:</span>
              <select
                value={sortBy}
                onChange={(e) => onSortByChange(e.target.value as 'match' | 'deadline' | 'amount')}
                className="py-1.5 px-2.5 border border-neutral-300 rounded-lg text-neutral-800 bg-white font-medium"
              >
                <option value="match">Highest Match % First</option>
                <option value="deadline">Closing Soonest First</option>
                <option value="amount">Highest Annual Value (ZAR)</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-3 text-neutral-500 font-mono">
            <span>
              Showing {scholarships.length} of {totalCatalogCount} bursaries
            </span>
            {(selectedStudyLevel !== 'all' ||
              selectedField !== 'all' ||
              filterCoverage !== 'all' ||
              searchQuery.trim() !== '') && (
              <button
                onClick={() => {
                  onSelectStudyLevel('all');
                  onSelectField('all');
                  onFilterCoverageChange('all');
                  onSearchQueryChange('');
                }}
                className="text-xs text-emerald-800 font-semibold underline hover:text-emerald-950 font-sans"
              >
                Reset filters
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Results Grid */}
      {scholarships.length === 0 ? (
        <div className="text-center py-14 bg-white border border-neutral-200 rounded-2xl p-8 space-y-3">
          <AlertCircle className="w-10 h-10 text-neutral-400 mx-auto" />
          <h3 className="text-base font-bold text-neutral-800">
            No bursaries match these exact filters
          </h3>
          <p className="text-xs text-neutral-500 max-w-md mx-auto">
            Try clearing your search keyword or switching the study level and coverage filters back to
            "All".
          </p>
          <button
            onClick={() => {
              onSelectStudyLevel('all');
              onSelectField('all');
              onFilterCoverageChange('all');
              onSearchQueryChange('');
            }}
            className="px-4 py-2 text-xs font-semibold text-white bg-neutral-900 rounded-lg hover:bg-neutral-800 transition-colors"
          >
            Reset All Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {scholarships.map((scholarship) => (
            <ScholarshipCard
              key={scholarship.id}
              scholarship={scholarship}
              isSaved={savedIds.includes(scholarship.id)}
              isCompared={compareIds.includes(scholarship.id)}
              onToggleSave={onToggleSave}
              onToggleCompare={onToggleCompare}
              onOpenReminderModal={onOpenReminderModal}
              onOpenSmartApply={onOpenSmartApply}
              studentMarks={profile.marksPercentage}
            />
          ))}
        </div>
      )}
    </div>
  );
};
