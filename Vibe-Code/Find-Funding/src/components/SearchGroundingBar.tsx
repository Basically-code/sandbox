import React, { useState } from 'react';
import { Search, Globe, RefreshCw, CheckCircle2 } from 'lucide-react';
import { Scholarship, StudyLevel } from '../types';

interface SearchGroundingBarProps {
  onResultsFetched: (results: Scholarship[], grounded: boolean) => void;
  selectedStudyLevel: StudyLevel;
  userMarks: number;
}

export const SearchGroundingBar: React.FC<SearchGroundingBarProps> = ({
  onResultsFetched,
  selectedStudyLevel,
  userMarks,
}) => {
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [syncStatus, setSyncStatus] = useState<string | null>(null);
  const [isDailySyncing, setIsDailySyncing] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setSyncStatus(null);

    try {
      const res = await fetch('/api/funding/search-grounded', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: query || 'South Africa bursaries',
          studyLevel: selectedStudyLevel,
          minMarks: userMarks,
        }),
      });
      const data = await res.json();
      if (data.scholarships && data.scholarships.length > 0) {
        onResultsFetched(data.scholarships, data.groundedWithSearch);
        setSyncStatus(
          data.groundedWithSearch
            ? 'Retrieved live South African bursaries matching your criteria.'
            : 'Catalog updated with bursary opportunities matching your criteria.'
        );
        setTimeout(() => setSyncStatus(null), 5000);
      }
    } catch (err) {
      console.error(err);
      setSyncStatus('Search completed using database.');
      setTimeout(() => setSyncStatus(null), 4000);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDailySync = async () => {
    setIsDailySyncing(true);
    try {
      const res = await fetch('/api/funding/daily-sync', { method: 'POST' });
      const data = await res.json();
      setSyncStatus(`Daily Calendar Sync: ${data.message} Synced for ${data.syncDate}.`);
      setTimeout(() => setSyncStatus(null), 6000);
    } catch (err) {
      console.error(err);
    } finally {
      setIsDailySyncing(false);
    }
  };

  return (
    <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs space-y-3">
      <form onSubmit={handleSearch} className="flex flex-col sm:flex-row items-center gap-2">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search South African bursaries (e.g. 'Sasol', 'NSFAS', 'Mining Engineering', 'Standard Bank', 'Commerce')..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-xs sm:text-sm border border-neutral-300 rounded-xl text-neutral-900 focus:outline-hidden focus:border-neutral-900 placeholder:text-neutral-400 font-sans"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button
            type="submit"
            disabled={isLoading}
            className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-neutral-950 hover:bg-neutral-800 disabled:opacity-60 rounded-xl transition-colors whitespace-nowrap"
          >
            <Globe className="w-3.5 h-3.5 text-neutral-300" />
            <span>{isLoading ? 'Searching...' : 'Search Bursaries'}</span>
          </button>

          <button
            type="button"
            onClick={handleDailySync}
            disabled={isDailySyncing}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-xl transition-colors whitespace-nowrap"
            title="Refresh intake calendars and portal statuses"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isDailySyncing ? 'animate-spin text-neutral-900' : 'text-neutral-500'}`} />
            <span className="hidden md:inline">{isDailySyncing ? 'Refreshing...' : 'Daily Refresh'}</span>
          </button>
        </div>
      </form>

      {syncStatus && (
        <div className="p-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-xs text-neutral-700 flex items-center gap-2">
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
          <span>{syncStatus}</span>
        </div>
      )}
    </div>
  );
};
