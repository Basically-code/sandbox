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

interface CategoryAlertPromptProps {
  category: string;
  userEmail: string;
  onClose: () => void;
  onSubscribed: (category: string, email: string) => void;
}

export const CategoryAlertPrompt: React.FC<CategoryAlertPromptProps> = ({
  category,
  userEmail,
  onClose,
  onSubscribed,
}) => {
  const [email, setEmail] = useState(userEmail || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !email.includes('@')) return;
    onSubscribed(category, email.trim());
    onClose();
  };

  return (
    <aside
      aria-label="Category bursary notifications"
      className="fixed bottom-5 right-5 z-50 max-w-sm w-full bg-white border border-neutral-300 rounded-2xl p-5 shadow-2xl"
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
            <Bell className="w-4 h-4" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-mono tracking-wider font-semibold text-emerald-800">
              South Africa Bursary Alert
            </span>
            <h4 className="text-sm font-bold text-neutral-900 leading-tight">
              Tracking {category}?
            </h4>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-neutral-400 hover:text-neutral-700 p-1 rounded-md"
          aria-label="Dismiss alert"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3 mt-1">
        <p className="text-xs text-neutral-600 leading-relaxed">
          Get an instant email alert whenever new <strong>{category}</strong> bursaries open for the
          2026/2027 South African intake.
        </p>
        <div className="flex items-center gap-2">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
            className="flex-1 px-3 py-2 text-xs border border-neutral-300 rounded-lg text-neutral-900 bg-neutral-50"
            required
          />
          <button
            type="submit"
            className="px-3.5 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-lg whitespace-nowrap"
          >
            Notify Me
          </button>
        </div>
      </form>
    </aside>
  );
};
