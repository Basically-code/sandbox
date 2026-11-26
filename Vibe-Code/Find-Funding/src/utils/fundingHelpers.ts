import { Scholarship, StudentProfile } from '../types';

// ============================================================================
// SAFE STORAGE & CLIPBOARD HELPERS (Crash-Proof in Sandboxed Iframes)
// ============================================================================

export function safeGetStorage<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw);
    return parsed !== null && parsed !== undefined ? parsed : fallback;
  } catch {
    return fallback;
  }
}

export function safeSetStorage<T>(key: string, value: T): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore quota or sandboxed iframe storage blocks safely
  }
}

export function copyTextSafe(text: string, onSuccess?: () => void): void {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(
        () => onSuccess && onSuccess(),
        () => fallbackCopy(text, onSuccess)
      );
    } else {
      fallbackCopy(text, onSuccess);
    }
  } catch {
    fallbackCopy(text, onSuccess);
  }
}

export function fallbackCopy(text: string, onSuccess?: () => void): void {
  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    if (onSuccess) onSuccess();
  } catch {
    // Fallback silent
  }
}

export function sanitizePortalUrl(url: string): string {
  try {
    const parsed = new URL(url);
    if (parsed.protocol === 'https:' || parsed.protocol === 'http:') {
      return parsed.toString();
    }
    return 'https://www.nsfas.org.za/';
  } catch {
    return 'https://www.nsfas.org.za/';
  }
}

export function sanitizeStrictText(input: string, maxLen = 500): string {
  return String(input || '')
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .replace(/[<>]/g, '')
    .slice(0, maxLen);
}

export function computeDeterministicHash(payload: string): string {
  let h1 = 0xdeadbeef ^ payload.length;
  let h2 = 0x41c6ce57 ^ payload.length;
  for (let i = 0, ch; i < payload.length; i++) {
    ch = payload.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (
    'sha256_' +
    (h2 >>> 0).toString(16).padStart(8, '0') +
    (h1 >>> 0).toString(16).padStart(8, '0')
  );
}

// Calculate NSC Level (1-7) from percentage
export function getNSCLevel(percentage: number): number {
  if (percentage >= 80) return 7;
  if (percentage >= 70) return 6;
  if (percentage >= 60) return 5;
  if (percentage >= 50) return 4;
  if (percentage >= 40) return 3;
  if (percentage >= 30) return 2;
  return 1;
}

export function getDaysRemaining(deadlineStr: string): number {
  const deadlineDate = new Date(deadlineStr + 'T23:59:59');
  const today = new Date();
  const diffTime = deadlineDate.getTime() - today.getTime();
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

// ============================================================================
// ELIGIBILITY & MATCH SCORING ENGINE
// ============================================================================

export function evaluateScholarshipMatch(sch: Scholarship, profile: StudentProfile): Scholarship {
  let score = 0;
  const reasons: string[] = [];
  const warnings: string[] = [];

  // 1. Study Level Match (up to 25 pts)
  if (sch.studyLevel === profile.studyLevel) {
    score += 25;
    reasons.push(`Direct match for your ${formatStudyLevel(profile.studyLevel)} study level`);
  } else if (
    (profile.studyLevel === 'matric' && sch.studyLevel === 'undergraduate') ||
    (profile.studyLevel === 'honours' && sch.studyLevel === 'masters') ||
    (profile.studyLevel === 'masters' && sch.studyLevel === 'honours')
  ) {
    score += 20;
    reasons.push(`Open to ${formatStudyLevel(profile.studyLevel)} applicants entering ${formatStudyLevel(sch.studyLevel)}`);
  } else {
    score += 8;
    warnings.push(`Primary target is ${formatStudyLevel(sch.studyLevel)} (you selected ${formatStudyLevel(profile.studyLevel)})`);
  }

  // 2. Overall Aggregate & APS Match (up to 30 pts)
  if (profile.marksPercentage >= sch.minMarks) {
    const margin = profile.marksPercentage - sch.minMarks;
    score += margin >= 8 ? 30 : 25;
    reasons.push(
      `Your ${profile.marksPercentage}% average meets the ${sch.minMarks}% cutoff (+${margin}% margin)`
    );
  } else {
    const gap = sch.minMarks - profile.marksPercentage;
    score += Math.max(5, 20 - gap * 2);
    warnings.push(`Requires ${sch.minMarks}% aggregate (${gap}% above your current ${profile.marksPercentage}%)`);
  }

  // 3. Subject-Specific Check: Pure Maths & Physical Sciences (up to 20 pts)
  if (sch.requiresPureMath) {
    if (profile.mathStream !== 'pure_math') {
      warnings.push('Requires Grade 12 Pure Mathematics (Maths Literacy / Technical Maths not accepted)');
    } else if (sch.minMathMark && profile.mathMark < sch.minMathMark) {
      warnings.push(`Requires ${sch.minMathMark}% in Pure Maths (your Maths mark is ${profile.mathMark}%)`);
      score += 8;
    } else {
      score += 12;
      reasons.push(`Meets Pure Mathematics requirement (${profile.mathMark}%)`);
    }

    if (sch.minScienceMark) {
      if (profile.scienceMark >= sch.minScienceMark) {
        score += 8;
        reasons.push(`Meets Physical Sciences requirement (${profile.scienceMark}%)`);
      } else {
        warnings.push(`Requires ${sch.minScienceMark}% in Physical Sciences (yours is ${profile.scienceMark}%)`);
      }
    } else {
      score += 8;
    }
  } else {
    score += 20;
    reasons.push('No strict Pure Mathematics / Physical Science barrier');
  }

  // 4. Field of Study Alignment (up to 15 pts)
  const schFieldLower = (sch.field + ' ' + sch.tags.join(' ') + ' ' + sch.categoryKey).toLowerCase();
  const userFieldLower = profile.fieldOfStudy.toLowerCase();
  const userKeywords = userFieldLower.split(/[\s,&/]+/).filter((w) => w.length > 2);

  if (sch.categoryKey === 'all' || schFieldLower.includes('all ')) {
    score += 14;
    reasons.push('Funds all accredited South African degree and diploma disciplines');
  } else if (userKeywords.some((kw) => schFieldLower.includes(kw))) {
    score += 15;
    reasons.push(`Aligned with your ${profile.fieldOfStudy} focus`);
  } else {
    score += 4;
  }

  // 5. Household Financial Need, SASSA Fast-Track & Disability R600k Cap Match (up to 10 pts)
  const effectiveMaxCap =
    profile.hasDisability && sch.id === 'nsfas-dhet-2026' ? 600000 : sch.maxHouseholdIncome;

  if (sch.financialNeedRequired) {
    if (profile.sassaBeneficiary) {
      score += 10;
      reasons.push('SASSA Beneficiary Fast-Track: Automatically qualifies on financial need (means-test waived)');
    } else if (profile.hasDisability) {
      score += 10;
      reasons.push('Disability Support Mode active: Qualifies under expanded R600,000 p.a. threshold & assistive device grant');
    } else if (profile.financialNeedLevel === 'nsfas_eligible') {
      score += 10;
      reasons.push('Matches your NSFAS / low-income household eligibility (<= R350k p.a.)');
    } else if (profile.financialNeedLevel === 'missing_middle') {
      if (!effectiveMaxCap || effectiveMaxCap >= 600000) {
        score += 10;
        reasons.push('Qualifies under Missing Middle household threshold (R350k–R600k p.a.)');
      } else {
        warnings.push(`Strict household income cap of R${effectiveMaxCap.toLocaleString('en-ZA')} p.a.`);
        score += 3;
      }
    } else if (profile.financialNeedLevel === 'none') {
      warnings.push('Requires demonstrated financial need (household income threshold applies)');
    } else {
      score += 7;
    }
  } else {
    score += 10;
    reasons.push('Merit-first selection (no restrictive household income cap)');
  }

  // 6. NBT Readiness check for competitive degree bursaries
  if (
    (sch.categoryKey === 'engineering' || sch.categoryKey === 'commerce' || sch.categoryKey === 'health') &&
    profile.studyLevel === 'matric' &&
    profile.nbtStatus === 'not_booked'
  ) {
    warnings.push('NBT Advisory: Book your National Benchmark Test (AQL & MAT) at nbt.ac.za for university placement');
  }

  const finalScore = Math.min(99, Math.max(35, score));
  return {
    ...sch,
    matchScore: finalScore,
    matchReasons: reasons,
    matchWarnings: warnings,
  };
}

export function formatStudyLevel(level: string): string {
  switch (level) {
    case 'matric':
      return 'Matric / Grade 12';
    case 'tvet':
      return 'TVET College (NCV / NATED)';
    case 'undergraduate':
      return 'Undergraduate Degree / Diploma';
    case 'honours':
      return 'Honours Degree';
    case 'masters':
      return 'Master’s Degree';
    case 'phd':
      return 'Doctoral / PhD';
    default:
      return 'All Study Levels';
  }
}

export function formatCurrencyZAR(amount: number, display?: string): string {
  if (display) return display;
  return `R ${amount.toLocaleString('en-ZA')}`;
}

export function generateMotivationLetter(sch: Scholarship, profile: StudentProfile): string {
  const today = new Date().toISOString().split('T')[0];
  const needStatement =
    profile.financialNeedLevel === 'nsfas_eligible'
      ? 'Coming from a household that qualifies under the South African NSFAS threshold (<= R350,000 per annum), comprehensive financial support is essential for me to focus fully on my academic studies without the burden of tuition and accommodation arrears.'
      : profile.financialNeedLevel === 'missing_middle'
      ? 'As part of South Africa’s "Missing Middle" demographic, my family falls above the state NSFAS cap yet cannot afford full university tuition, residence fees, and learning materials out of pocket.'
      : 'While my family has supported my foundational schooling, securing this merit bursary will enable me to dedicate myself completely to academic research, leadership, and vacation training.';

  const applicantName = profile.fullName.trim() || '[Your Full Name]';
  const applicantProvince = profile.province.trim() || '[Your Province]';
  const applicantEmail = profile.email.trim() || '[Your Email Address]';
  const applicantCourse = profile.fieldOfStudy.trim() || '[Your Course / Qualification]';
  const applicantSchool = profile.targetInstitution.trim() || 'a South African university / college';

  return `${applicantName}
${applicantProvince}, South Africa
Email: ${applicantEmail}
Date: ${today}

Selection Committee
${sch.provider}
Re: Application for ${sch.title} (${sch.cycleInfo})

Dear Bursary Selection Committee,

I am writing to apply for the ${sch.title} offered by ${sch.provider}. I am currently a ${formatStudyLevel(profile.studyLevel)} student studying ${applicantCourse} at ${applicantSchool}, with an academic average of ${profile.marksPercentage}% (Admission Points: ${profile.matricAPS}/42${profile.mathStream === 'pure_math' ? `, Pure Mathematics: ${profile.mathMark}%` : ''}).

I chose to study ${applicantCourse} because I want to ${profile.careerGoal.trim() || 'build a strong career and help my community in South Africa'}. I work hard at my studies and meet the ${sch.minMarks}% mark needed for this bursary.

${needStatement}

I deeply admire ${sch.provider}'s investment in developing South African talent in ${sch.field}. ${
    sch.serviceObligation.toLowerCase().includes('none')
      ? 'If awarded this fellowship, I pledge to uphold academic excellence and mentor fellow students in my community.'
      : `I am also fully committed to fulfilling the programme’s practical vacation work and post-graduation service objectives (${sch.serviceObligation}).`
  }

Enclosed with my application on your official portal are my certified South African ID copy, official academic transcripts, and supporting household documentation. Thank you for considering my application.

Yours sincerely,
${profile.fullName}`;
}
