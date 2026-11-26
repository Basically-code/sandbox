# Find Funding South Africa — Production Full-Stack Release

## Overview
Find Funding South Africa is a full-stack React 19 + TypeScript and Express platform for discovering South African bursaries, calculating 7-subject NSC APS & subject-specific eligibility, assembling statutory certified documents, generating tailored motivation letters, and receiving automated 7d/3d/24h closing reminders.

## Key System Components
1. **`src/App.tsx` (React 19 + TypeScript Front-End)**:
   - **Bursaries Directory**: Search, filter by study level, discipline, coverage perks (laptop, residence, no work-back), and compare up to 3 bursaries side-by-side.
   - **7-Subject NSC APS & Marks Calculator**: Calculates the 42-point Admission Point Score (excluding Life Orientation) and checks Pure Mathematics vs Mathematical Literacy prerequisites.
   - **4-Step Guided Smart Apply Studio**: Live eligibility audit, per-bursary certified document checklist, custom Motivation Letter generator, and direct portal launchpad.
   - **System & Security Operations Center**: Live dashboard for the 24-hour Midnight Google Search Grounding Crawler, Portal Liveness Monitor, Unified Subscriber Registry, and Cybersecurity Audit Log.
2. **`server.ts` (Express Backend, Midnight Search Crawler & Security Middleware)**:
   - **24-Hour Midnight Rebuild (`scheduleMidnightSASTRebuild`)**: Automatically executes at `00:00:00 SAST (UTC+2)` every night to check all external bursary URLs (`HEAD` liveness check for 404/takedowns), query Gemini (`gemini-2.5-flash` with `googleSearch` grounding) for updated closing dates, and re-sign the catalog with `HMAC-SHA256`.
   - **Unified Subscriber Database (`data/findfunding_db.json`)**: Deduplicates and merges profile data, tracked bursaries, deadline reminders, and category alerts into a single `UnifiedSubscriberRecord` per student email using atomic write-ahead disk persistence.
   - **Cybersecurity Shield**: Enforces read-only public catalog access, `HMAC-SHA256` integrity signatures, sliding-window IP rate limiting, recursive XSS/NoSQL/prototype-pollution sanitization, `https://` URL validation, and HTTP security headers.
3. **`firestore.rules`**:
   - Default-deny security rules enforcing strict owner isolation (`/users/{userId}/...`), read-only `/scholarships/{scholarshipId}`, and field length caps (`notes <= 1000`, `motivationLetter <= 5000`).

## Quick Start
```bash
npm install
npm run dev
```

## Production Build & Launch
```bash
export GEMINI_API_KEY="your_api_key"
export CATALOG_HMAC_SECRET="your_random_64_char_secret"
export ADMIN_SYNC_TOKEN="your_admin_token"
npm run build
NODE_ENV=production npm start
```
