import React, { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import api from '../api/axios';
import type { PersonResponse, ClearanceLevel, EducationLevel, Page } from '../types';
import {
  Search, ChevronRight, Fingerprint, ShieldAlert,
  Activity, ShieldCheck, MapPin, Loader2, Filter, X
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

type Gender = 'MALE' | 'FEMALE' | 'OTHER';

interface PersonFilters {
  query: string;
  clearanceLevel: ClearanceLevel | 'ALL';
  minClearanceLevel: ClearanceLevel | 'ALL';
  educationLevel: EducationLevel | 'ALL';
  nationality: string;
  placeOfBirth: string;
  gender: Gender | 'ALL';
  politicalAffiliation: string;
}

const EMPTY_FILTERS: PersonFilters = {
  query: '',
  clearanceLevel: 'ALL',
  minClearanceLevel: 'ALL',
  educationLevel: 'ALL',
  nationality: '',
  placeOfBirth: '',
  gender: 'ALL',
  politicalAffiliation: '',
};

const PAGE_SIZE = 30;

const SubjectRegistry = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<PersonFilters>(EMPTY_FILTERS);
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedQuery(filters.query), 400);
    return () => clearTimeout(handler);
  }, [filters.query]);

  const hasActiveCriteria =
    debouncedQuery.trim().length >= 3 ||
    filters.clearanceLevel !== 'ALL' ||
    filters.minClearanceLevel !== 'ALL' ||
    filters.educationLevel !== 'ALL' ||
    filters.nationality.trim().length > 0 ||
    filters.placeOfBirth.trim().length > 0 ||
    filters.gender !== 'ALL' ||
    filters.politicalAffiliation.trim().length > 0;

  const {
    data,
    isLoading,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
    isError,
  } = useInfiniteQuery<Page<PersonResponse>>({
    queryKey: [
      'persons-filtered', debouncedQuery, filters.clearanceLevel, filters.minClearanceLevel,
      filters.educationLevel, filters.nationality, filters.placeOfBirth, filters.gender,
      filters.politicalAffiliation,
    ],
    queryFn: async ({ pageParam }) => {
      const res = await api.get('/persons/search/filtered', {
        params: {
          query: debouncedQuery.trim() || undefined,
          clearanceLevel: filters.clearanceLevel !== 'ALL' ? filters.clearanceLevel : undefined,
          minClearanceLevel: filters.minClearanceLevel !== 'ALL' ? filters.minClearanceLevel : undefined,
          educationLevel: filters.educationLevel !== 'ALL' ? filters.educationLevel : undefined,
          nationality: filters.nationality.trim() || undefined,
          placeOfBirth: filters.placeOfBirth.trim() || undefined,
          gender: filters.gender !== 'ALL' ? filters.gender : undefined,
          politicalAffiliation: filters.politicalAffiliation.trim() || undefined,
          page: pageParam,
          size: PAGE_SIZE,
          sort: 'lastName,asc',
        },
      });
      return res.data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
    enabled: hasActiveCriteria,
  });

  const personnel = useMemo(() => data?.pages.flatMap(p => p.content) ?? [], [data]);
  const totalElements = data?.pages[0]?.totalElements ?? 0;

  const clearanceStats = useMemo(() => {
    if (personnel.length === 0) return { highClearancePct: 0, count: 0 };
    const highLevelItems = personnel.filter(p =>
      p.clearanceLevel === 'LEVEL_4_SECRET' || p.clearanceLevel === 'LEVEL_5_TOP_SECRET'
    );
    return {
      highClearancePct: Math.round((highLevelItems.length / personnel.length) * 100),
      count: highLevelItems.length
    };
  }, [personnel]);

  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const target = entries[0];
    if (target.isIntersecting && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  useEffect(() => {
    const observer = new IntersectionObserver(handleObserver, { threshold: 0.1 });
    if (sentinelRef.current) observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [handleObserver]);

  const clearFilters = () => setFilters(EMPTY_FILTERS);

  const activeFilterCount = [
    filters.clearanceLevel !== 'ALL',
    filters.minClearanceLevel !== 'ALL',
    filters.educationLevel !== 'ALL',
    filters.nationality.trim().length > 0,
    filters.placeOfBirth.trim().length > 0,
    filters.gender !== 'ALL',
    filters.politicalAffiliation.trim().length > 0,
  ].filter(Boolean).length;

  return (
    <div className="h-full flex flex-col bg-brand-dark animate-in fade-in duration-700">

      {/* HEADER WITH SEARCH + FILTERS */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative overflow-hidden shrink-0">
        <div className="absolute inset-0 opacity-[0.03] pointer-events-none bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')]" />

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-end gap-8">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-brand-accent mb-1">
                <Activity size={14} />
                <span className="text-[10px] font-mono tracking-[0.4em] uppercase font-black">
                  Operational_Status: LIVE_SYNCHRONIZATION
                </span>
              </div>
              <h1 className="text-5xl font-black text-white italic tracking-tighter uppercase leading-none">
                Subject_Registry
              </h1>
            </div>

            {personnel.length > 0 && (
              <div className="w-full lg:w-72 bg-black/40 border border-brand-border p-4 rounded-sm flex flex-col gap-2 shadow-2xl">
                <div className="flex justify-between items-center text-[9px] font-mono uppercase tracking-widest text-slate-400">
                  <span className="flex items-center gap-2">
                    <ShieldCheck size={12} className="text-purple-500" />
                    High_Access_Ratio_In_View
                  </span>
                  <span className="text-white font-black">{clearanceStats.highClearancePct}%</span>
                </div>
                <div className="h-1.5 w-full bg-slate-900 rounded-full overflow-hidden flex">
                  <div
                    className="h-full bg-purple-600 shadow-[0_0_8px_rgba(147,51,234,0.5)] transition-all duration-1000"
                    style={{ width: `${clearanceStats.highClearancePct}%` }}
                  />
                </div>
                <div className="text-[7px] font-mono text-slate-600 uppercase text-right tracking-tighter">
                  {clearanceStats.count}_Of_{personnel.length}_Loaded // {totalElements}_Total_Matches
                </div>
              </div>
            )}
          </div>

          {/* SEARCH + FILTER ROW */}
          <div className="mt-6 flex flex-col md:flex-row gap-3">
            <div className="relative flex-1 group">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-brand-accent transition-colors" size={16} />
              <input
                type="text"
                value={filters.query}
                onChange={(e) => setFilters(f => ({ ...f, query: e.target.value }))}
                placeholder="IDENT_FILTER_BY_NAME... (min 3 characters)"
                className="w-full bg-black/40 border border-brand-border py-3 pl-12 pr-4 font-mono text-[11px] text-brand-accent focus:border-brand-accent/50 outline-none uppercase tracking-[0.2em]"
              />
            </div>

            <button
              onClick={() => setShowFilters(s => !s)}
              className={`flex items-center gap-2 px-5 py-3 border font-mono text-[10px] uppercase tracking-widest transition-all ${
                showFilters || activeFilterCount > 0
                  ? 'border-brand-accent/50 text-brand-accent bg-brand-accent/5'
                  : 'border-brand-border text-slate-400 hover:border-slate-600'
              }`}
            >
              <Filter size={14} />
              Filters {activeFilterCount > 0 && `(${activeFilterCount})`}
            </button>

            {(activeFilterCount > 0 || filters.query) && (
              <button
                onClick={clearFilters}
                className="flex items-center gap-2 px-4 py-3 border border-red-500/30 text-red-500/80 hover:bg-red-500/10 font-mono text-[10px] uppercase tracking-widest transition-all"
              >
                <X size={14} /> Clear
              </button>
            )}
          </div>

          {/* EXPANDABLE FILTER PANEL */}
          {showFilters && (
            <div className="mt-3 p-4 bg-black/40 border border-brand-border grid grid-cols-1 md:grid-cols-4 gap-4">
              <FilterSelect
                label="Clearance_Exact"
                value={filters.clearanceLevel}
                onChange={(v) => setFilters(f => ({ ...f, clearanceLevel: v as ClearanceLevel | 'ALL' }))}
                options={[
                  ['ALL', 'All_Levels'], ['LEVEL_1_PUBLIC', 'L1_Public'], ['LEVEL_2_INTERNAL', 'L2_Internal'],
                  ['LEVEL_3_CONFIDENTIAL', 'L3_Confidential'], ['LEVEL_4_SECRET', 'L4_Secret'], ['LEVEL_5_TOP_SECRET', 'L5_Top_Secret'],
                ]}
              />
              <FilterSelect
                label="Clearance_Minimum"
                value={filters.minClearanceLevel}
                onChange={(v) => setFilters(f => ({ ...f, minClearanceLevel: v as ClearanceLevel | 'ALL' }))}
                options={[
                  ['ALL', 'No_Minimum'], ['LEVEL_2_INTERNAL', 'L2+'], ['LEVEL_3_CONFIDENTIAL', 'L3+'],
                  ['LEVEL_4_SECRET', 'L4+'], ['LEVEL_5_TOP_SECRET', 'L5_Only'],
                ]}
              />
              <FilterSelect
                label="Education"
                value={filters.educationLevel}
                onChange={(v) => setFilters(f => ({ ...f, educationLevel: v as EducationLevel | 'ALL' }))}
                options={[
                  ['ALL', 'All_Levels'], ['SECONDARY', 'Secondary'], ['HIGHER_VOCATIONAL', 'Higher_Vocational'],
                  ['BACHELOR', 'Bachelor'], ['MASTER', 'Master'], ['DOCTORATE', 'Doctorate'],
                ]}
              />
              <FilterSelect
                label="Gender"
                value={filters.gender}
                onChange={(v) => setFilters(f => ({ ...f, gender: v as Gender | 'ALL' }))}
                options={[
                  ['ALL', 'All'], ['MALE', 'Male'], ['FEMALE', 'Female'], ['OTHER', 'Other'],
                ]}
              />
              <div className="flex flex-col gap-1">
                <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Nationality</label>
                <input
                  type="text"
                  value={filters.nationality}
                  onChange={(e) => setFilters(f => ({ ...f, nationality: e.target.value }))}
                  placeholder="e.g. Czech"
                  className="bg-black/60 border border-brand-border py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50"
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Place_Of_Birth</label>
                <input
                  type="text"
                  value={filters.placeOfBirth}
                  onChange={(e) => setFilters(f => ({ ...f, placeOfBirth: e.target.value }))}
                  placeholder="e.g. Prague"
                  className="bg-black/60 border border-brand-border py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50"
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Political_Affiliation</label>
                <input
                  type="text"
                  value={filters.politicalAffiliation}
                  onChange={(e) => setFilters(f => ({ ...f, politicalAffiliation: e.target.value }))}
                  placeholder="e.g. party name"
                  className="bg-black/60 border border-brand-border py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50"
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* RESULTS AREA */}
      <div className="flex-1 overflow-y-auto custom-scrollbar">
        <div className="max-w-7xl mx-auto px-8 py-6">

          {!hasActiveCriteria && (
            <div className="flex flex-col items-center justify-center py-32 gap-4 text-center">
              <Fingerprint size={48} className="text-slate-700" />
              <p className="font-mono text-[11px] text-slate-500 uppercase tracking-[0.3em] max-w-md">
                Enter a name (3+ characters) or apply a filter to query the registry.
              </p>
              <p className="font-mono text-[9px] text-slate-700 uppercase tracking-widest">
                Registry size makes unfiltered browsing impractical — narrow your search to begin.
              </p>
            </div>
          )}

          {hasActiveCriteria && isLoading && (
            <div className="flex flex-col items-center justify-center py-32 gap-6 font-mono text-brand-accent">
              <div className="w-16 h-16 border-2 border-brand-accent/10 border-t-brand-accent rounded-full animate-spin" />
              <span className="text-[10px] tracking-[0.5em] animate-pulse">EXTRACTING_RECORDS...</span>
            </div>
          )}

          {hasActiveCriteria && isError && (
            <div className="flex flex-col items-center justify-center py-32 gap-4 text-red-500/70">
              <ShieldAlert size={40} />
              <span className="text-[10px] font-mono uppercase tracking-[0.3em]">Query_Failed // Insufficient_Clearance_Or_Server_Error</span>
            </div>
          )}

          {hasActiveCriteria && !isLoading && !isError && personnel.length === 0 && (
            <div className="flex flex-col items-center justify-center py-32 gap-4 text-center">
              <Search size={40} className="text-slate-700" />
              <span className="text-[10px] font-mono text-slate-600 uppercase tracking-[0.3em]">Zero_Matches_In_Registry</span>
            </div>
          )}

          {/* NEW: dense table/row layout, replacing the card grid.
              Mirrors ArchivePage's table structure — a sticky header row,
              one row per person, hover highlight, click-to-navigate. At
              30+ rows per page this shows far more matches per screen
              than 3-per-row cards, which is what a filtered search-results
              page over a large registry actually needs. */}
          {hasActiveCriteria && !isLoading && personnel.length > 0 && (
            <>
              <div className="bg-brand-panel/10 border border-brand-border rounded-sm overflow-hidden">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-black/40 border-b border-brand-border">
                      <th className="p-3 pl-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Subject</th>
                      <th className="p-3 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Clearance</th>
                      <th className="p-3 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Education</th>
                      <th className="p-3 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Nationality</th>
                      <th className="p-3 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Current_Location</th>
                      <th className="p-3 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Affiliation</th>
                      <th className="p-3 pr-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500 text-right">Influence</th>
                      <th className="p-3 w-8"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-border/30">
                    {personnel.map(person => (
                      <PersonRow
                        key={person.publicId}
                        person={person}
                        onClick={() => navigate(`/personnel/${person.publicId}`)}
                      />
                    ))}
                  </tbody>
                </table>
              </div>

              <div ref={sentinelRef} className="h-20 flex items-center justify-center mt-4">
                {isFetchingNextPage && (
                  <div className="flex items-center gap-3 text-brand-accent/60 font-mono text-[10px] uppercase tracking-widest">
                    <Loader2 size={16} className="animate-spin" /> Loading_More_Records...
                  </div>
                )}
                {!hasNextPage && personnel.length > 0 && (
                  <span className="text-[9px] font-mono text-slate-700 uppercase tracking-widest">
                    End_Of_Results // {totalElements}_Total_Matches
                  </span>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

const FilterSelect = ({ label, value, onChange, options }: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: [string, string][];
}) => (
  <div className="flex flex-col gap-1">
    <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">{label}</label>
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="bg-black/60 border border-brand-border py-2 px-3 text-[10px] font-mono text-slate-300 uppercase outline-none focus:border-brand-accent/50 cursor-pointer"
    >
      {options.map(([val, lbl]) => <option key={val} value={val}>{lbl}</option>)}
    </select>
  </div>
);

// NEW: dense row component, replacing PersonCard as the primary list item.
// Computes the same calculateInfluence/getClearanceStyle logic as before —
// the underlying data and scoring didn't change, only the layout.
const PersonRow = React.memo(({ person, onClick }: { person: PersonResponse, onClick: () => void }) => {

  const calculateInfluence = useMemo(() => {
    let score = 30;

    const eduScores: Record<string, number> = {
      'DOCTORATE': 25, 'MASTER': 15, 'BACHELOR': 8, 'HIGHER_VOCATIONAL': 4
    };
    score += eduScores[person.educationLevel] || 0;

    const clearanceBonus: Record<string, number> = {
      'LEVEL_5_TOP_SECRET': 40, 'LEVEL_4_SECRET': 25, 'LEVEL_3_CONFIDENTIAL': 15,
      'LEVEL_2_INTERNAL': 5, 'LEVEL_1_PUBLIC': 0
    };
    score += clearanceBonus[person.clearanceLevel] || 0;

    if (person.age > 40) score += 5;
    if (person.age > 55) score += 5;

    return Math.min(score, 99);
  }, [person]);

  const getClearanceStyle = (level: string) => {
    switch(level) {
      case 'LEVEL_5_TOP_SECRET': return { color: 'text-purple-500', border: 'border-purple-500/30', bg: 'bg-purple-500/5', label: 'L5_TOP_SECRET' };
      case 'LEVEL_4_SECRET': return { color: 'text-red-500', border: 'border-red-500/30', bg: 'bg-red-500/5', label: 'L4_SECRET' };
      case 'LEVEL_3_CONFIDENTIAL': return { color: 'text-orange-500', border: 'border-orange-500/30', bg: 'bg-orange-500/5', label: 'L3_CONFID' };
      default: return { color: 'text-brand-accent', border: 'border-brand-border', bg: 'bg-brand-panel/50', label: level.replace('LEVEL_', 'L') };
    }
  };

  const style = getClearanceStyle(person.clearanceLevel);
  const displayName = `${person.titleBefore ? person.titleBefore + ' ' : ''}${person.lastName} ${person.firstName}${person.titleAfter ? ', ' + person.titleAfter : ''}`;

  return (
    <tr
      onClick={onClick}
      className="group hover:bg-brand-accent/5 transition-all cursor-pointer"
    >
      <td className="p-3 pl-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-brand-dark border border-brand-border flex items-center justify-center rounded-sm text-slate-700 group-hover:text-brand-accent group-hover:border-brand-accent/30 transition-all shrink-0 overflow-hidden">
            {person.photoUrl ? (
              <img src={person.photoUrl} alt={person.lastName} className="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all" />
            ) : (
              <Fingerprint size={14} />
            )}
          </div>
          <div className="min-w-0">
            <div className="text-xs font-black text-slate-200 uppercase tracking-tight group-hover:text-white truncate">
              {displayName}
            </div>
            <div className="text-[9px] text-slate-600 font-mono italic">NODE: {person.publicId.slice(0, 8)}</div>
          </div>
        </div>
      </td>
      <td className="p-3">
        <span className={`px-2 py-0.5 border text-[9px] font-black font-mono tracking-widest rounded-sm ${style.border} ${style.bg} ${style.color}`}>
          {style.label}
        </span>
      </td>
      <td className="p-3">
        <span className="text-[10px] font-mono text-slate-400 uppercase">{person.educationLevel || '—'}</span>
      </td>
      <td className="p-3">
        <span className="text-[10px] font-mono text-slate-400">{person.nationality || '—'}</span>
      </td>
      <td className="p-3">
        <div className="flex items-center gap-1.5 text-[10px] font-mono text-slate-400 truncate max-w-[160px]">
          {person.currentLocationName && <MapPin size={10} className="text-brand-accent shrink-0" />}
          <span className="truncate">{person.currentLocationName || '—'}</span>
        </div>
      </td>
      <td className="p-3">
        <span className="text-[9px] font-mono text-emerald-500 font-black truncate max-w-[140px] block">
          {person.politicalAffiliation || 'NON_AFFILIATED'}
        </span>
      </td>
      <td className="p-3 pr-4 text-right">
        <div className="flex items-center justify-end gap-2">
          <div className="h-1 w-12 bg-white/5 rounded-full overflow-hidden">
            <div
              className={`h-full ${calculateInfluence > 80 ? 'bg-red-500' : 'bg-brand-accent'}`}
              style={{ width: `${calculateInfluence}%` }}
            />
          </div>
          <span className="text-[9px] font-mono text-slate-400 w-7 text-right">{calculateInfluence}%</span>
        </div>
      </td>
      <td className="p-3 text-right text-slate-700 group-hover:text-brand-accent transition-all">
        <ChevronRight size={16} />
      </td>
    </tr>
  );
});

export default SubjectRegistry;