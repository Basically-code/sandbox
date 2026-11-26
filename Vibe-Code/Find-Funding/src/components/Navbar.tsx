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

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  profile: StudentProfile;
  onOpenProfile: () => void;
  trackedCount: number;
  readinessScore: number;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  profile,
  onOpenProfile,
  trackedCount,
  readinessScore,
}) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { id: 'home', label: 'Home' },
    { id: 'bursaries', label: 'Bursaries' },
    { id: 'marks', label: 'Check Marks' },
    { id: 'recommendations', label: 'My Matches' },
    {
      id: 'tracker',
      label: `My Applications ${trackedCount > 0 ? `(${trackedCount})` : ''}`,
    },
    { id: 'reminders', label: 'Reminders' },
  ];

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-neutral-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between gap-4 h-16">
          <button
            onClick={() => setActiveTab('home')}
            className="flex items-center gap-2.5 text-left group shrink-0"
          >
            <img
              src="/assets/aistudio/Screenshot_2026-10-23_230324_1ed49d35.png"
              alt="Find Funding Logo"
              className="w-8 h-8 object-contain rounded-md"
            />
            <div className="flex flex-col">
              <span className="font-extrabold tracking-tight text-lg sm:text-xl text-neutral-950 group-hover:text-neutral-700 transition-colors leading-none">
                Find Funding
              </span>
            </div>
          </button>

          <nav className="hidden md:flex items-center gap-4 lg:gap-5 text-sm font-medium text-neutral-600">
            {navItems.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`group relative py-1.5 text-xs xl:text-sm font-medium whitespace-nowrap transition-colors duration-200 ${
                    isActive
                      ? 'text-neutral-950 font-bold'
                      : 'text-neutral-600 hover:text-neutral-950'
                  }`}
                >
                  <span>{item.label}</span>
                  <span
                    className={`pointer-events-none absolute bottom-0 left-0 h-0.5 rounded-full bg-emerald-700 ${
                      isActive
                        ? 'w-full'
                        : 'w-0 group-hover:w-full transition-all duration-300 ease-out'
                    }`}
                  />
                </button>
              );
            })}
          </nav>

          <div className="hidden md:flex items-center gap-2.5 shrink-0">
            <button
              onClick={onOpenProfile}
              className="flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors whitespace-nowrap"
            >
              <User className="w-3.5 h-3.5 text-neutral-600" />
              <span className="max-w-[130px] truncate">{profile.fullName || 'My Profile'}</span>
            </button>
          </div>

          <div className="flex md:hidden items-center gap-2">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-neutral-700 hover:text-neutral-950"
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {mobileMenuOpen && (
        <div className="lg:hidden border-b border-neutral-200 bg-white px-4 pt-2 pb-4 space-y-1">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => {
                setActiveTab(item.id);
                setMobileMenuOpen(false);
              }}
              className={`block w-full text-left px-3 py-2 rounded-lg text-sm font-medium ${
                activeTab === item.id
                  ? 'bg-neutral-100 text-neutral-950 font-bold'
                  : 'text-neutral-600 hover:bg-neutral-50'
              }`}
            >
              {item.label}
            </button>
          ))}
          <div className="pt-2 border-t border-neutral-100">
            <button
              onClick={() => {
                onOpenProfile();
                setMobileMenuOpen(false);
              }}
              className="w-full text-left px-3 py-2 text-xs font-semibold text-neutral-800 bg-neutral-50 rounded-lg flex items-center justify-between"
            >
              <span>
                My Profile {profile.fullName ? `(${profile.fullName})` : ''}
              </span>
              <span className="font-mono text-neutral-500">{readinessScore}% Ready</span>
            </button>
          </div>
        </div>
      )}
    </header>
  );
};
