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
// VIEW 5: APPLICATION STUDIO & DOCUMENT VAULT TRACKER
// ============================================================================

interface ApplicationTrackerViewProps {
  applications: ApplicationTrackerRecord[];
  scholarships: Scholarship[];
  profile: StudentProfile;
  onUpdateProfile: (p: StudentProfile) => void;
  onUpdateStatus: (id: string, status: ApplicationStatus) => void;
  onUpdateNotes: (id: string, notes: string) => void;
  onRemove: (id: string) => void;
  onNavigateToExplore: () => void;
  onOpenSmartApply: (sch: Scholarship) => void;
  onOpenReminderModalForTracked: (app: ApplicationTrackerRecord) => void;
  onShowToast: (msg: string, type?: 'success' | 'info' | 'warning') => void;
}

export const ApplicationTrackerView: React.FC<ApplicationTrackerViewProps> = ({
  applications,
  scholarships,
  profile,
  onUpdateProfile,
  onUpdateStatus,
  onUpdateNotes,
  onRemove,
  onNavigateToExplore,
  onOpenSmartApply,
  onOpenReminderModalForTracked,
  onShowToast,
}) => {
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [editingNotesId, setEditingNotesId] = useState<string | null>(null);
  const [tempNotes, setTempNotes] = useState<string>('');
  const [viewingLetterId, setViewingLetterId] = useState<string | null>(null);

  const statusOptions: { value: ApplicationStatus; label: string }[] = [
    { value: 'saved', label: 'Saved / Reviewing' },
    { value: 'preparing', label: 'Preparing Documents' },
    { value: 'submitted', label: 'Submitted to Portal' },
    { value: 'awarded', label: 'Awarded Bursary 🎉' },
    { value: 'not_awarded', label: 'Unsuccessful' },
  ];

  const docItems: { key: keyof DocumentVaultState; label: string; hint: string }[] = [
    {
      key: 'certifiedId',
      label: 'Certified SA ID Copy',
      hint: 'Stamped by SAPS / Commissioner of Oaths (< 3 months old)',
    },
    {
      key: 'academicTranscript',
      label: 'NSC Matric / University Results',
      hint: 'Official school statement or registrar academic record',
    },
    {
      key: 'universityAcceptance',
      label: 'University Provisional Admission',
      hint: 'Official acceptance or application reference number for 2027',
    },
    {
      key: 'proofOfIncome',
      label: 'Parents Proof of Income / Affidavit',
      hint: '3 months payslips, SASSA letter, or sworn unemployment affidavit',
    },
    {
      key: 'proofOfResidence',
      label: 'Proof of Residence',
      hint: 'Municipal utility bill or traditional council letter',
    },
    {
      key: 'cvAndMotivation',
      label: 'Curriculum Vitae & Motivation Letter',
      hint: 'Generated via Find Funding Smart Apply Studio',
    },
  ];

  const toggleGlobalDoc = (key: keyof DocumentVaultState) => {
    onUpdateProfile({
      ...profile,
      documents: {
        ...profile.documents,
        [key]: !profile.documents[key],
      },
    });
  };

  const filteredApps = applications.filter((app) =>
    filterStatus === 'all' ? true : app.status === filterStatus
  );

  const handleExportSummary = () => {
    const lines = [
      `FIND FUNDING SOUTH AFRICA - APPLICATION PIPELINE (${profile.fullName})`,
      `Academic Profile: ${profile.marksPercentage}% Avg | APS ${profile.matricAPS}/42 | ${profile.fieldOfStudy}`,
      `Generated: ${new Date().toISOString().split('T')[0]}`,
      '',
      ...applications.map(
        (a, i) =>
          `${i + 1}. ${a.scholarshipTitle} (${a.providerName}) - Status: ${a.status.toUpperCase()} | Deadline: ${a.deadline} | Portal: ${a.portalUrl}`
      ),
    ];
    copyTextSafe(lines.join('\n'), () =>
      onShowToast('Copied complete application tracker summary to clipboard!')
    );
  };

  return (
    <div className="space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 uppercase tracking-wider font-mono">
            <span>My Applications & Documents</span>
            <span className="text-neutral-300">·</span>
            <span>South Africa</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-950 mt-1">
            My Applications & Required Documents
          </h1>
          <p className="text-sm text-neutral-600 mt-1">
            Tick off the documents you have stamped at the police station, copy your motivation letters, and track your bursary applications.
          </p>
        </div>
        <div className="flex items-center gap-2 self-start sm:self-auto">
          {applications.length > 0 && (
            <button
              onClick={handleExportSummary}
              className="inline-flex items-center gap-1.5 px-3.5 py-2.5 text-xs font-semibold text-neutral-800 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-xl transition-colors"
            >
              <Copy className="w-3.5 h-3.5" />
              <span>Copy Schedule</span>
            </button>
          )}
          <button
            onClick={onNavigateToExplore}
            className="inline-flex items-center gap-1.5 px-4 py-2.5 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Bursaries</span>
          </button>
        </div>
      </div>

      {/* Global Certified Document Readiness Vault */}
      <div className="bg-white border border-neutral-200 rounded-2xl p-6 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-base font-bold text-neutral-950 flex items-center gap-2">
              <FileCheck className="w-4 h-4 text-emerald-700" />
              <span>My Certified Documents Checklist</span>
            </h2>
            <p className="text-xs text-neutral-500 mt-0.5">
              Check off the certified documents you have scanned and ready in PDF format (under 2MB
              per file for most portals).
            </p>
          </div>
          <span className="text-xs font-mono font-bold px-3 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-900 self-start sm:self-auto">
            {Object.values(profile.documents).filter(Boolean).length} / 6 Documents Ready
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {docItems.map((doc) => {
            const checked = profile.documents[doc.key];
            return (
              <button
                key={doc.key}
                type="button"
                onClick={() => toggleGlobalDoc(doc.key)}
                className={`p-3.5 rounded-xl border text-left transition-all flex items-start gap-3 ${
                  checked
                    ? 'bg-emerald-50/60 border-emerald-300 text-neutral-900'
                    : 'bg-neutral-50 border-neutral-200 text-neutral-600 hover:bg-white'
                }`}
              >
                <div
                  className={`w-4 h-4 rounded flex items-center justify-center shrink-0 mt-0.5 ${
                    checked
                      ? 'bg-emerald-700 text-white'
                      : 'bg-white border border-neutral-300'
                  }`}
                >
                  {checked && <Check className="w-3 h-3" />}
                </div>
                <div>
                  <div className="text-xs font-bold text-neutral-900">{doc.label}</div>
                  <div className="text-[11px] text-neutral-500 mt-0.5 leading-snug">{doc.hint}</div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Ready-to-Print SAPS Statutory Affidavit & Consent Templates */}
        <div className="pt-4 border-t border-neutral-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
          <div className="text-neutral-600">
            <strong className="text-neutral-900">Need a Police Station Affidavit?</strong> Copy a
            ready-to-print South African statutory template to sign at SAPS:
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => {
                const tpl = `SWORN AFFIDAVIT: UNEMPLOYED PARENT / LEGAL GUARDIAN (SOUTH AFRICA)

I, the undersigned, ________________________________ [Parent/Guardian Full Name], Identity Number ________________________, residing at ________________________________________________ (${profile.province}), do hereby make oath and state that:
1. I am the biological parent / legal guardian of the applicant, ${profile.fullName}.
2. I am currently unemployed and receive no formal salary or wage.
3. Our household survives on [SASSA Grant / Informal remittances] amounting to approximately R__________ per month.

Signed at ____________________ (SAPS Station) on this ____ day of ____________ 2026.

_______________________
Deponent Signature

_______________________
Commissioner of Oaths / SAPS Official Stamp`;
                copyTextSafe(tpl, () =>
                  onShowToast('Copied SAPS Unemployed Parent Affidavit template to clipboard!')
                );
              }}
              className="px-3 py-1.5 rounded-lg bg-neutral-100 hover:bg-neutral-200 font-semibold text-neutral-800 inline-flex items-center gap-1.5"
            >
              <Copy className="w-3 h-3" />
              <span>Unemployed Parent Affidavit</span>
            </button>
            <button
              type="button"
              onClick={() => {
                const tpl = `SWORN DECLARATION: INFORMAL SECTOR / SELF-EMPLOYED HOUSEHOLD INCOME

I, the undersigned, ________________________________, Identity Number ________________________, residing in ${profile.province}, declare under oath that:
1. I support student applicant ${profile.fullName} (${profile.email}).
2. I earn an informal income without formal payslips, averaging R__________ per month (under R350,000 per annum).

Signed at ____________________ (SAPS / Commissioner of Oaths) on this ____ day of ____________ 2026.

_______________________
Deponent Signature & Official SAPS Stamp`;
                copyTextSafe(tpl, () =>
                  onShowToast('Copied Informal Sector Income Declaration template to clipboard!')
                );
              }}
              className="px-3 py-1.5 rounded-lg bg-neutral-100 hover:bg-neutral-200 font-semibold text-neutral-800 inline-flex items-center gap-1.5"
            >
              <Copy className="w-3 h-3" />
              <span>Informal Income Affidavit</span>
            </button>
            <button
              type="button"
              onClick={() => {
                const tpl = `DISABILITY & ASSISTIVE DEVICE MEDICAL ANNEXURE CHECKLIST (NSFAS / DHET)

Applicant: ${profile.fullName} (${profile.email})
Target Institution: ${profile.targetInstitution}
1. HPCSA Registered Doctor / Specialist Assessment & Stamp
2. Confirmation of permanent physical, sensory, or learning disability
3. Recommended Assistive Device quote (Braille / Laptop / Hearing / Mobility support)
4. Expanded Household Income Threshold: Up to R600,000 p.a. (N+2 Rule applies)`;
                copyTextSafe(tpl, () =>
                  onShowToast('Copied Disability Medical Annexure Checklist to clipboard!')
                );
              }}
              className="px-3 py-1.5 rounded-lg bg-neutral-100 hover:bg-neutral-200 font-semibold text-neutral-800 inline-flex items-center gap-1.5"
            >
              <Copy className="w-3 h-3" />
              <span>Disability Annexure Guide</span>
            </button>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
        {[
          { id: 'all', label: `All Applications (${applications.length})` },
          {
            id: 'saved',
            label: `Saved (${applications.filter((a) => a.status === 'saved').length})`,
          },
          {
            id: 'preparing',
            label: `Preparing (${applications.filter((a) => a.status === 'preparing').length})`,
          },
          {
            id: 'submitted',
            label: `Submitted (${applications.filter((a) => a.status === 'submitted').length})`,
          },
          {
            id: 'awarded',
            label: `Awarded (${applications.filter((a) => a.status === 'awarded').length})`,
          },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setFilterStatus(tab.id)}
            className={`px-3.5 py-1.5 font-medium rounded-lg whitespace-nowrap transition-colors ${
              filterStatus === tab.id
                ? 'bg-neutral-950 text-white font-semibold'
                : 'bg-white border border-neutral-200 text-neutral-600 hover:text-neutral-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tracked Applications List */}
      {filteredApps.length === 0 ? (
        <div className="text-center py-14 bg-white border border-neutral-200 rounded-2xl p-8 space-y-3">
          <Bookmark className="w-8 h-8 text-neutral-400 mx-auto" />
          <h3 className="text-base font-bold text-neutral-800">
            No bursary applications in this stage yet
          </h3>
          <p className="text-xs text-neutral-500 max-w-sm mx-auto">
            Click "Guided Apply" or the bookmark icon on any bursary in the directory to build your
            application package here.
          </p>
          <button
            onClick={onNavigateToExplore}
            className="px-4 py-2 text-xs font-semibold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl transition-colors"
          >
            Browse Bursaries
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredApps.map((app) => {
            const daysLeft = getDaysRemaining(app.deadline);
            const matchedSch = scholarships.find((s) => s.id === app.scholarshipId);

            return (
              <div
                key={app.id}
                className="bg-white border border-neutral-200 rounded-2xl p-5 hover:border-neutral-400 transition-colors shadow-xs space-y-4"
              >
                <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-4">
                  <div className="space-y-1 flex-1">
                    <div className="flex items-center gap-2 text-xs text-neutral-500 font-mono">
                      <span>{app.providerName}</span>
                      <span>·</span>
                      <span className="font-semibold text-neutral-950">
                        {formatCurrencyZAR(app.amount, app.amountDisplay)}
                      </span>
                    </div>
                    <h3 className="text-base font-bold text-neutral-950">
                      {app.scholarshipTitle}
                    </h3>
                    <div className="flex flex-wrap items-center gap-3 text-xs text-neutral-600 pt-1">
                      <span className="flex items-center gap-1 font-mono">
                        <Calendar className="w-3.5 h-3.5 text-neutral-500" />
                        Deadline: {app.deadline}
                      </span>
                      {daysLeft > 0 ? (
                        <span
                          className={`font-semibold ${
                            daysLeft <= 14 ? 'text-amber-700' : 'text-emerald-800'
                          }`}
                        >
                          ({daysLeft} days remaining)
                        </span>
                      ) : (
                        <span className="text-neutral-400">(Closed)</span>
                      )}
                      {app.completedDocs && (
                        <span className="font-mono text-[11px] px-2 py-0.5 rounded bg-neutral-100 text-neutral-700">
                          {app.completedDocs.length} Checklist Docs Ready
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <select
                      value={app.status}
                      onChange={(e) => onUpdateStatus(app.id, e.target.value as ApplicationStatus)}
                      className="text-xs font-semibold py-1.5 px-2.5 bg-neutral-50 border border-neutral-300 rounded-xl text-neutral-900"
                    >
                      {statusOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>

                    {matchedSch && (
                      <button
                        onClick={() => onOpenSmartApply(matchedSch)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-white bg-neutral-950 hover:bg-emerald-900 rounded-xl transition-colors"
                      >
                        <span>Smart Apply Prep</span>
                      </button>
                    )}

                    {app.motivationLetter && (
                      <button
                        onClick={() =>
                          setViewingLetterId(viewingLetterId === app.id ? null : app.id)
                        }
                        className="px-3 py-1.5 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors"
                      >
                        {viewingLetterId === app.id ? 'Hide Letter' : 'Motivation Letter'}
                      </button>
                    )}

                    <a
                      href={sanitizePortalUrl(app.portalUrl)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors"
                    >
                      <span>Portal</span>
                      <ExternalLink className="w-3.5 h-3.5 text-neutral-600" />
                    </a>

                    <button
                      onClick={() => onOpenReminderModalForTracked(app)}
                      className="px-3 py-1.5 text-xs font-medium text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors"
                    >
                      Set Alert
                    </button>

                    <button
                      onClick={() => onRemove(app.id)}
                      className="p-2 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors"
                      aria-label="Remove application"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Collapsible Saved Motivation Letter */}
                {viewingLetterId === app.id && app.motivationLetter && (
                  <div className="p-4 bg-neutral-900 text-neutral-100 rounded-xl space-y-2 text-xs">
                    <div className="flex items-center justify-between border-b border-neutral-800 pb-2">
                      <span className="font-mono font-semibold text-emerald-400">
                        Saved Motivation Letter for {app.scholarshipTitle}
                      </span>
                      <button
                        onClick={() =>
                          copyTextSafe(app.motivationLetter, () =>
                            onShowToast('Motivation letter copied to clipboard!')
                          )
                        }
                        className="inline-flex items-center gap-1 px-2.5 py-1 bg-neutral-800 hover:bg-neutral-700 rounded-md text-white font-semibold"
                      >
                        <Copy className="w-3 h-3" />
                        <span>Copy Text</span>
                      </button>
                    </div>
                    <pre className="whitespace-pre-wrap font-sans text-xs leading-relaxed text-neutral-200">
                      {app.motivationLetter}
                    </pre>
                  </div>
                )}

                {/* Notes Editor */}
                <div className="pt-3 border-t border-neutral-100">
                  {editingNotesId === app.id ? (
                    <div className="space-y-2">
                      <textarea
                        value={tempNotes}
                        onChange={(e) => setTempNotes(e.target.value.slice(0, 1000))}
                        rows={2}
                        className="w-full text-xs p-2.5 border border-neutral-300 rounded-xl text-neutral-900"
                        placeholder="Add portal reference number, referee contact, or outstanding certified documents..."
                      />
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => setEditingNotesId(null)}
                          className="px-2.5 py-1 text-xs text-neutral-600"
                        >
                          Cancel
                        </button>
                        <button
                          onClick={() => {
                            onUpdateNotes(app.id, tempNotes);
                            setEditingNotesId(null);
                          }}
                          className="px-3 py-1 text-xs font-bold text-white bg-neutral-950 rounded-lg"
                        >
                          Save Notes
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div
                      onClick={() => {
                        setEditingNotesId(app.id);
                        setTempNotes(app.notes || '');
                      }}
                      className="text-xs text-neutral-600 cursor-pointer hover:bg-neutral-50 p-2 rounded-xl -mx-2 flex items-start gap-2"
                    >
                      <FileText className="w-3.5 h-3.5 text-neutral-400 shrink-0 mt-0.5" />
                      <span>
                        {app.notes ||
                          'Click to add application reference number or document notes...'}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
