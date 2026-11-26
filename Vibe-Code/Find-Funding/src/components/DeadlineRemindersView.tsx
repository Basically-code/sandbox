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
// VIEW 6: DEADLINE REMINDERS & CATEGORY ALERTS VIEW
// ============================================================================

interface DeadlineRemindersViewProps {
  reminders: DeadlineReminder[];
  onRemoveReminder: (id: string) => void;
  categorySubscriptions: CategoryAlertSubscription[];
  onAddCategorySubscription: (email: string, category: string) => void;
  onRemoveCategorySubscription: (id: string) => void;
  userEmail: string;
  studentName: string;
  onUpdateEmail: (email: string) => void;
  onRegisterReminder: (scholarshipId: string, customEmail?: string) => void;
  scholarships: Scholarship[];
}

export const DeadlineRemindersView: React.FC<DeadlineRemindersViewProps> = ({
  reminders,
  onRemoveReminder,
  categorySubscriptions,
  onAddCategorySubscription,
  onRemoveCategorySubscription,
  userEmail,
  studentName,
  onUpdateEmail,
  onRegisterReminder,
  scholarships,
}) => {
  const [inputEmail, setInputEmail] = useState(userEmail || '');
  const [selectedScholarshipId, setSelectedScholarshipId] = useState(
    scholarships[0]?.id || 'nsfas-dhet-2026'
  );
  const [selectedCategory, setSelectedCategory] = useState('Engineering, Energy & Mining');
  const [testNotification, setTestNotification] = useState<{
    email: string;
    studentName: string;
    scholarshipTitle: string;
    providerName: string;
    portalUrl: string;
    deadline: string;
    daysRemaining: number;
    subject: string;
    sentAt: string;
  } | null>(null);

  // Sync email and default scholarship ID when props update
  useEffect(() => {
    if (userEmail) setInputEmail(userEmail);
  }, [userEmail]);

  useEffect(() => {
    if (!selectedScholarshipId && scholarships.length > 0) {
      setSelectedScholarshipId(scholarships[0].id);
    }
  }, [scholarships, selectedScholarshipId]);

  const saCategories = [
    'Engineering, Energy & Mining',
    'Commerce, CA(SA) & Banking',
    'Information Tech, AI & Data',
    'Teaching & Education (DBE)',
    'Health Sciences & Missing Middle',
    'TVET College & Artisan Trades',
    'Postgraduate Honours, Master’s & PhD',
  ];

  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputEmail || !selectedScholarshipId) return;
    onUpdateEmail(inputEmail);
    onRegisterReminder(selectedScholarshipId, inputEmail);
  };

  const handleSubscribeCategory = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputEmail || !selectedCategory) return;
    onAddCategorySubscription(inputEmail.trim(), selectedCategory);
  };

  const handleSendTestAlert = () => {
    const selectedSch =
      scholarships.find((s) => s.id === selectedScholarshipId) || scholarships[0];
    if (!selectedSch) return;
    const days = Math.max(1, getDaysRemaining(selectedSch.deadline));
    setTestNotification({
      email: inputEmail || userEmail || 'student@gmail.com',
      studentName: studentName || 'Applicant',
      scholarshipTitle: selectedSch.title,
      providerName: selectedSch.provider,
      portalUrl: sanitizePortalUrl(selectedSch.portalUrl),
      deadline: selectedSch.deadline,
      daysRemaining: days,
      subject: `⚡ Automated Alert: ${selectedSch.title} closes in ${days} days (${selectedSch.deadline})`,
      sentAt: new Date().toISOString().split('T')[0],
    });
  };

  return (
    <div className="space-y-8 pb-16">
      <div>
        <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 uppercase tracking-wider font-mono">
          <span>Automated Notification Center</span>
          <span className="text-neutral-300">·</span>
          <span>South Africa</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-950 mt-1">
          Deadline Alerts & Category Opening Subscriptions
        </h1>
        <p className="text-sm text-neutral-600 mt-1 max-w-3xl">
          Schedule automated countdown alerts 7 days, 3 days, and 24 hours before South African
          bursary portals close, or subscribe to instant discipline opening alerts.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-7 bg-white border border-neutral-200 rounded-2xl p-6 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-neutral-950 flex items-center gap-2">
            <Mail className="w-4 h-4 text-emerald-700" />
            <span>Schedule Specific Bursary Countdown Alert</span>
          </h2>

          <form onSubmit={handleRegister} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Student Email Address
              </label>
              <input
                type="email"
                required
                value={inputEmail}
                onChange={(e) => setInputEmail(e.target.value)}
                placeholder="yourname@gmail.com"
                className="w-full px-3.5 py-2.5 border border-neutral-300 rounded-xl text-sm text-neutral-900"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Target South African Bursary
              </label>
              <select
                value={selectedScholarshipId}
                onChange={(e) => setSelectedScholarshipId(e.target.value)}
                className="w-full px-3.5 py-2.5 border border-neutral-300 rounded-xl text-sm text-neutral-900 bg-white font-medium"
              >
                {scholarships.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.title} (Closes {s.deadline})
                  </option>
                ))}
              </select>
            </div>

            <button
              type="submit"
              className="w-full py-2.5 px-4 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl transition-colors flex items-center justify-center gap-2"
            >
              <Bell className="w-3.5 h-3.5" />
              <span>Activate Countdown Reminders (7d, 3d, 24h)</span>
            </button>
          </form>

          <div className="pt-3 border-t border-neutral-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-neutral-500">
            <span>Preview how your automated reminder email looks:</span>
            <button
              type="button"
              onClick={handleSendTestAlert}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors"
            >
              <Send className="w-3 h-3 text-neutral-600" />
              <span>Preview Sample Alert Dispatch</span>
            </button>
          </div>
        </div>

        {/* Right Column: Category Subscriptions */}
        <div className="lg:col-span-5 bg-white border border-neutral-200 rounded-2xl p-6 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-neutral-950 flex items-center gap-2">
            <Tag className="w-4 h-4 text-emerald-700" />
            <span>Discipline Opening Alerts</span>
          </h2>
          <p className="text-xs text-neutral-600 leading-relaxed">
            Get notified the moment new South African corporate or government bursaries open in your
            study stream.
          </p>

          <form onSubmit={handleSubscribeCategory} className="space-y-3">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Select Study Stream
              </label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-xs font-medium text-neutral-900 bg-white"
              >
                {saCategories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="submit"
              className="w-full py-2 px-3 text-xs font-semibold text-neutral-900 bg-emerald-50 border border-emerald-200 hover:bg-emerald-100 rounded-xl transition-colors"
            >
              Subscribe to {selectedCategory}
            </button>
          </form>

          {categorySubscriptions.length > 0 && (
            <div className="pt-3 border-t border-neutral-100 space-y-2">
              <div className="text-[11px] font-bold text-neutral-500 uppercase tracking-wider font-mono">
                Active Subscriptions ({categorySubscriptions.length}):
              </div>
              <div className="space-y-1.5 max-h-40 overflow-y-auto">
                {categorySubscriptions.map((sub) => (
                  <div
                    key={sub.id}
                    className="p-2 bg-neutral-50 border border-neutral-200 rounded-lg text-xs flex items-center justify-between gap-2"
                  >
                    <div>
                      <div className="font-semibold text-neutral-800">{sub.category}</div>
                      <div className="text-[11px] text-neutral-500 font-mono">{sub.email}</div>
                    </div>
                    <button
                      onClick={() => onRemoveCategorySubscription(sub.id)}
                      className="text-neutral-400 hover:text-red-600 p-1"
                      aria-label="Remove subscription"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Live Preview Dispatched Card */}
      {testNotification && (
        <div className="bg-neutral-950 text-white rounded-2xl p-6 shadow-xl border border-neutral-800 space-y-3">
          <div className="flex items-center justify-between border-b border-neutral-800 pb-3">
            <div className="flex items-center gap-2 text-xs text-emerald-400 font-mono">
              <CheckCircle2 className="w-4 h-4" />
              <span>Simulated Email Dispatch Preview ({testNotification.sentAt})</span>
            </div>
            <button
              onClick={() => setTestNotification(null)}
              className="text-xs text-neutral-400 hover:text-white"
            >
              Close Preview
            </button>
          </div>
          <div className="space-y-1 text-xs">
            <div className="text-neutral-400">
              To: <span className="text-white font-mono">{testNotification.email}</span>
            </div>
            <div className="text-neutral-400">
              Subject: <span className="text-white font-semibold">{testNotification.subject}</span>
            </div>
          </div>
          <div className="bg-neutral-900 rounded-xl p-4 text-xs space-y-2 border border-neutral-800">
            <p className="text-neutral-200 leading-relaxed">
              Hello {testNotification.studentName}, your tracked bursary{' '}
              <strong className="text-white">{testNotification.scholarshipTitle}</strong> (
              {testNotification.providerName}) closes on{' '}
              <span className="font-mono text-amber-400 font-semibold">
                {testNotification.deadline}
              </span>{' '}
              ({testNotification.daysRemaining} days left). Ensure your certified SA ID copy,
              academic transcript, and motivation letter are uploaded before cutoff.
            </p>
            <div className="pt-2">
              <a
                href={testNotification.portalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-neutral-950 bg-white hover:bg-neutral-100 rounded-lg"
              >
                <span>Open Official Provider Portal</span>
                <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>
        </div>
      )}

      {/* Scheduled Reminders Table */}
      <div className="space-y-3">
        <h2 className="text-base font-bold text-neutral-950">
          Scheduled Countdown Reminders ({reminders.length})
        </h2>
        {reminders.length === 0 ? (
          <div className="bg-white border border-neutral-200 rounded-2xl p-8 text-center space-y-2">
            <Clock className="w-7 h-7 text-neutral-400 mx-auto" />
            <h3 className="text-sm font-bold text-neutral-800">No active deadline reminders</h3>
            <p className="text-xs text-neutral-500">
              Select a bursary above or click the bell icon on any card to schedule alerts.
            </p>
          </div>
        ) : (
          <div className="bg-white border border-neutral-200 rounded-2xl overflow-hidden shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-neutral-50 text-neutral-600 border-b border-neutral-200 font-semibold font-mono">
                  <tr>
                    <th className="py-3 px-4">Bursary Scheme</th>
                    <th className="py-3 px-4">Recipient Email</th>
                    <th className="py-3 px-4">Closing Date</th>
                    <th className="py-3 px-4">Countdown</th>
                    <th className="py-3 px-4">Schedule</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {reminders.map((rem) => (
                    <tr key={rem.id} className="hover:bg-neutral-50/50">
                      <td className="py-3 px-4 font-semibold text-neutral-900">
                        {rem.scholarshipTitle}
                      </td>
                      <td className="py-3 px-4 font-mono text-neutral-600">{rem.email}</td>
                      <td className="py-3 px-4 font-mono text-neutral-800">{rem.deadline}</td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-800">
                        {getDaysRemaining(rem.deadline)} days left
                      </td>
                      <td className="py-3 px-4">
                        <span className="text-emerald-700 font-medium inline-flex items-center gap-1">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>7d · 3d · 24h</span>
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => onRemoveReminder(rem.id)}
                          className="text-neutral-400 hover:text-red-600 p-1"
                          aria-label="Delete reminder"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
