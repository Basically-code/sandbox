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
// REMINDER MODAL & CATEGORY PROMPT
// ============================================================================

interface ReminderModalProps {
  isOpen: boolean;
  onClose: () => void;
  scholarship: Scholarship | null;
  userEmail: string;
  onConfirmReminder: (scholarshipId: string, email: string) => void;
}

export const ReminderModal: React.FC<ReminderModalProps> = ({
  isOpen,
  onClose,
  scholarship,
  userEmail,
  onConfirmReminder,
}) => {
  const [email, setEmail] = useState(userEmail || '');

  useEffect(() => {
    if (isOpen && userEmail) {
      setEmail(userEmail);
    }
  }, [isOpen, userEmail]);

  if (!isOpen || !scholarship) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    onConfirmReminder(scholarship.id, email);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-neutral-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl w-full max-w-md shadow-xl overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-neutral-100">
          <div className="flex items-center gap-2">
            <Bell className="w-4 h-4 text-emerald-700" />
            <h3 className="text-base font-bold text-neutral-950">Schedule Deadline Reminders</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
          <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl space-y-1">
            <div className="text-[11px] text-neutral-500 font-medium">Selected Bursary</div>
            <div className="font-bold text-neutral-900 text-sm">{scholarship.title}</div>
            <div className="text-neutral-600 flex items-center gap-2 pt-1 font-mono">
              <Calendar className="w-3.5 h-3.5 text-neutral-500" />
              <span>Closing Date: {scholarship.deadline}</span>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
              Send Countdown Alerts To
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. yourname@gmail.com"
              className="w-full px-3 py-2 border border-neutral-300 rounded-lg text-sm text-neutral-900 font-mono placeholder:text-neutral-400"
            />
            <div className="text-[11px] text-neutral-500">
              Dispatches at <strong>7 days</strong>, <strong>3 days</strong>, and{' '}
              <strong>24 hours</strong> before closing.
            </div>
          </div>

          <div className="pt-2 flex items-center justify-end gap-2 border-t border-neutral-100">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-2 text-xs font-medium text-neutral-600 hover:text-neutral-900"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl flex items-center gap-1.5"
            >
              <Bell className="w-3.5 h-3.5" />
              <span>Confirm Alerts</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
