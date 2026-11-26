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
// STUDENT PROFILE MODAL (Synced with Props via useEffect)
// ============================================================================

interface StudentProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  profile: StudentProfile;
  onSaveProfile: (updated: StudentProfile) => void;
}

export const StudentProfileModal: React.FC<StudentProfileModalProps> = ({
  isOpen,
  onClose,
  profile,
  onSaveProfile,
}) => {
  const [formData, setFormData] = useState<StudentProfile>(profile);

  // Fix stale modal state bug from prototype: sync formData whenever profile or isOpen changes
  useEffect(() => {
    if (isOpen) {
      setFormData(profile);
    }
  }, [isOpen, profile]);

  if (!isOpen) return null;

  const saProvinces = [
    'Eastern Cape',
    'Gauteng',
    'Western Cape',
    'KwaZulu-Natal',
    'Free State',
    'Limpopo',
    'Mpumalanga',
    'North West',
    'Northern Cape',
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSaveProfile(formData);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-neutral-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-neutral-100">
          <div className="flex items-center gap-2">
            <User className="w-4 h-4 text-emerald-700" />
            <h3 className="text-base font-bold text-neutral-950">
              Student Academic & Eligibility Profile (South Africa)
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs max-h-[80vh] overflow-y-auto">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Full Name
              </label>
              <input
                type="text"
                required
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                placeholder="e.g. Sipho Dlamini"
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 placeholder:text-neutral-400"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Student Email (For Reminders)
              </label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="e.g. sipho.dlamini@gmail.com"
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 font-mono placeholder:text-neutral-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Study Level
              </label>
              <select
                value={formData.studyLevel}
                onChange={(e) =>
                  setFormData({ ...formData, studyLevel: e.target.value as StudyLevel })
                }
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 bg-white"
              >
                <option value="undergraduate">Undergraduate (Degree / Diploma)</option>
                <option value="tvet">TVET College (NCV / NATED)</option>
                <option value="matric">Matric / Grade 12</option>
                <option value="honours">Honours Degree</option>
                <option value="masters">Master’s Degree</option>
                <option value="phd">Doctoral / PhD</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Course or Field of Study
              </label>
              <input
                type="text"
                required
                value={formData.fieldOfStudy}
                onChange={(e) => setFormData({ ...formData, fieldOfStudy: e.target.value })}
                placeholder="e.g. Engineering, Teaching, Nursing, Accounting"
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 placeholder:text-neutral-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Province
              </label>
              <input
                type="text"
                list="sa-provinces-datalist"
                value={formData.province}
                onChange={(e) => setFormData({ ...formData, province: e.target.value })}
                placeholder="e.g. Eastern Cape, Gauteng, KwaZulu-Natal"
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 placeholder:text-neutral-400"
              />
              <datalist id="sa-provinces-datalist">
                {saProvinces.map((prov) => (
                  <option key={prov} value={prov} />
                ))}
              </datalist>
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
                Yearly Family Income
              </label>
              <select
                value={formData.financialNeedLevel}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    financialNeedLevel: e.target.value as FinancialNeedLevel,
                  })
                }
                className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900 bg-white"
              >
                <option value="nsfas_eligible">Under R350,000 / year (NSFAS Eligible)</option>
                <option value="missing_middle">
                  R350,000 – R600,000 / year (Missing Middle)
                </option>
                <option value="moderate">Moderate Financial Need</option>
                <option value="none">Over R600,000 / year (Merit Bursaries)</option>
              </select>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="font-bold text-neutral-700 uppercase tracking-wider block font-mono">
              Target University / TVET College
            </label>
            <input
              type="text"
              value={formData.targetInstitution}
              onChange={(e) => setFormData({ ...formData, targetInstitution: e.target.value })}
              className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm text-neutral-900"
              placeholder="e.g. Rhodes University, Wits, UCT, TUT"
            />
          </div>

          <div className="p-3.5 bg-neutral-50 border border-neutral-200 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <label className="font-bold text-neutral-800 uppercase tracking-wider font-mono">
                Overall Academic Mark (%)
              </label>
              <span className="font-mono text-base font-bold text-emerald-800">
                {formData.marksPercentage}% (APS {formData.matricAPS})
              </span>
            </div>
            <input
              type="range"
              min="40"
              max="100"
              value={formData.marksPercentage}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  marksPercentage: Number(e.target.value),
                  matricAPS: Math.min(42, Math.round((Number(e.target.value) / 100) * 42) + 4),
                })
              }
              className="w-full h-2 bg-neutral-200 rounded-lg appearance-none cursor-pointer accent-neutral-950"
            />
          </div>

          <div className="pt-3 border-t border-neutral-100 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-neutral-600 hover:text-neutral-900 rounded-lg"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 px-5 py-2 text-xs font-bold text-white bg-neutral-950 hover:bg-neutral-800 rounded-xl transition-colors"
            >
              <Save className="w-3.5 h-3.5" />
              <span>Save Profile</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
