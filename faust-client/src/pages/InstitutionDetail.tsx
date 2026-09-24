import React, { useState, useMemo, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Building2, Users, MapPin, Briefcase, Info, ShieldAlert, ShieldCheck,
  Network, Globe, ChevronRight, ListTree, ChevronLeft,
  X, Terminal, Search, History,
  Activity, AlertTriangle, Lock, Eye, EyeOff, Landmark,
  FileText, Plus, Trash2, RefreshCw, TrendingUp, TrendingDown, Minus,
  Key, ExternalLink, CheckCircle2, XCircle, Zap, Scale
} from 'lucide-react';
import api from '../api/axios';
import type {
  InstitutionResponse,
  OccupationResponse,
  InstitutionTreeResponse,
  InstitutionAscendedResponse,
  LocationResponse,
  ClearanceLevel,
  VerificationStatus,
  ExternalContractResponse,
  InstitutionIdentifierResponse,
  InstitutionIdentifierRequest,
  IdentifierType
} from '../types';
import { InstitutionFlow } from '../components/institutions/InstitutionFlow';
import OrgChartTab from '../components/institutions/OrgChartTab';

type TabMode = 'personnel' | 'contracts' | 'identifiers' | 'orgchart';

const getInstitutionIcon = (type: string, size: number = 20) => {
  const cls = `shrink-0`;
  switch (type) {
    case 'INTELLIGENCE': return <ShieldAlert size={size} className={`${cls} text-red-500`} />;
    case 'MILITARY':     return <ShieldCheck size={size} className={`${cls} text-orange-500`} />;
    case 'LEGISLATIVE':  return <Landmark size={size} className={`${cls} text-purple-400`} />;
    case 'REGULATORY':   return <Zap size={size} className={`${cls} text-yellow-400`} />;
    case 'JUDICIAL':     return <Scale size={size} className={`${cls} text-blue-400`} />;
    case 'EXECUTIVE':    return <Building2 size={size} className={`${cls} text-brand-accent`} />;
    default:             return <Building2 size={size} className={`${cls} text-slate-500`} />;
  }
};

const InstitutionDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isNexusOpen, setIsNexusOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<TabMode>('personnel');
  const [contractPage, setContractPage] = useState(0);
  const [counterpartySearch, setCounterpartySearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [vacancyOnly, setVacancyOnly] = useState(false);

  // Debounce counterparty search — 350ms, reset to page 0 on change
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(counterpartySearch.trim());
      setContractPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [counterpartySearch]);

  const { data: institution, isLoading: isInstLoading, isError: isInstError } = useQuery<InstitutionResponse>({
    queryKey: ['institution', id],
    queryFn: async () => (await api.get(`/institutions/${id}`)).data
  });

  const { data: deepTree } = useQuery<InstitutionTreeResponse>({
    queryKey: ['institution-deep-tree', id],
    queryFn: async () => (await api.get(`/institutions/${id}/sub-tree`)).data,
    enabled: !!id
  });

  const { data: immediateChildren } = useQuery<InstitutionResponse[]>({
    queryKey: ['institution-children', id],
    queryFn: async () => (await api.get(`/institutions/parent/${id}`)).data,
    enabled: !!id
  });

  const { data: occupations } = useQuery<OccupationResponse[]>({
    queryKey: ['institution-occupations', id],
    queryFn: async () => (await api.get(`/occupations/institution/${id}`)).data,
    enabled: !!id
  });

  const { data: ascendedPath } = useQuery<InstitutionAscendedResponse>({
    queryKey: ['institution-path', id],
    queryFn: async () => (await api.get(`/institutions/${id}/path-to-root`)).data,
    enabled: !!id
  });

  const { data: identifiers, isLoading: isIdentifiersLoading } = useQuery<InstitutionIdentifierResponse[]>({
    queryKey: ['institution-identifiers', id],
    queryFn: async () => (await api.get(`/institutions/${id}/identifiers`)).data,
    enabled: !!id
  });

  // Server-side search — debouncedSearch passed as supplierSearch param
  // so filtering happens across ALL pages, not just the current page.
  // contractPage + debouncedSearch both in queryKey to trigger refetch.
  const { data: contractsPage, isLoading: isContractsLoading } = useQuery({
    queryKey: ['institution-contracts', id, contractPage, debouncedSearch],
    queryFn: async () => (await api.get(`/contracts/institution/${id}`, {
      params: {
        page:           contractPage,
        size:           20,
        sort:           'contractDate,desc',
        supplierSearch: debouncedSearch || undefined,
      }
    })).data,
    enabled: !!id && activeTab === 'contracts',
  });

  const contracts: ExternalContractResponse[] = contractsPage?.content ?? [];

  const addIdentifier = useMutation({
    mutationFn: async (req: InstitutionIdentifierRequest) =>
      (await api.post(`/institutions/${id}/identifiers`, req)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['institution-identifiers', id] });
      queryClient.invalidateQueries({ queryKey: ['institution-contracts', id] });
    }
  });

  const deactivateIdentifier = useMutation({
    mutationFn: async (identifierId: string) =>
      (await api.delete(`/institutions/${id}/identifiers/${identifierId}`)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['institution-identifiers', id] })
  });

  const fetchContracts = useMutation({
    mutationFn: async () =>
      (await api.get(`/contracts/institution/${id}`, {
        params: { fetchIfEmpty: true, page: 0, size: 100, sort: 'contractDate,desc' }
      })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['institution-contracts', id] })
  });

  const subordinates = useMemo<SubordinateSummary[]>(() => {
    const activeList: SubordinateSummary[] = (immediateChildren || []).map(c => ({
      publicId: c.publicId, name: c.name, active: c.active, type: c.type,
    }));
    const fromTree: SubordinateSummary[] = (deepTree?.children || []).map(c => ({
      publicId: c.publicId, name: c.name, active: c.active, type: c.type,
    }));
    const combined = [...activeList];
    fromTree.forEach(treeChild => {
      if (!combined.find(c => c.publicId === treeChild.publicId)) combined.push(treeChild);
    });
    return combined.sort((a, b) => a.name.localeCompare(b.name));
  }, [immediateChildren, deepTree]);

  const nexusData = useMemo(() => (deepTree ? [deepTree] : []), [deepTree]);

  const hasContractableIdentifier = identifiers?.some(i => i.active && i.type === 'NATIONAL_REGISTRATION') ?? false;

  const displayedOccupations = useMemo(() => {
    if (!occupations) return [];
    return vacancyOnly ? occupations.filter(o => o.vacant) : occupations;
  }, [occupations, vacancyOnly]);

  if (isInstLoading) return <LoadingState />;
  if (isInstError || !institution) return <ErrorState message="CRITICAL_FAILURE: NODE_UNREACHABLE" />;

  const isActive = institution.active;
  const themeColor = isActive ? 'text-brand-accent' : 'text-orange-500';
  const themeBorder = isActive ? 'border-brand-accent/30' : 'border-orange-500/50';
  const themeBg = isActive ? 'bg-[#05080f]' : 'bg-[#140b05]';
  const isDeceptionFlagged = institution.verificationStatus === 'DECEPTION_MARKER';

  return (
    <div className={`h-full bg-[#02040a] text-slate-300 flex flex-col overflow-hidden font-sans border-4 ${
      isDeceptionFlagged ? 'border-red-600/60' : isActive ? 'border-[#0a0f18]' : 'border-orange-900/30'
    }`}>

      {/* STATUS BAR */}
      <div className="h-6 bg-[#0a0f18] border-b border-white/10 flex items-center px-4 justify-between text-[9px] font-mono text-slate-500 uppercase tracking-[0.2em] shrink-0">
        <div className="flex gap-6">
          <span className="flex items-center gap-2">
            <Terminal size={10} className={themeColor} /> SYSTEM_CONNECTED: FAUST_v4.2
          </span>
          <span className="flex items-center gap-2">
            <Lock size={10} className={getClearanceColor(institution.clearanceLevel)} />
            CLEARANCE_LVL: {getClearanceWeight(institution.clearanceLevel)} // {institution.clearanceLevel}
          </span>
        </div>
        <div className="flex gap-4">
          {isDeceptionFlagged ? (
            <span className="text-red-500 font-bold flex items-center gap-1 animate-pulse">
              <AlertTriangle size={10} /> DECEPTION_MARKER_ACTIVE
            </span>
          ) : isActive ? (
            <span className="text-emerald-500/50 italic animate-pulse">● DATA_LINK_STABLE</span>
          ) : (
            <span className="text-orange-500/70 italic animate-pulse">▲ ARCHIVAL_RECORD_ACCESS</span>
          )}
          <span>{new Date().toISOString()}</span>
        </div>
      </div>

      {/* HEADER */}
      <header className={`h-28 ${themeBg} border-b border-white/10 flex items-center px-8 relative overflow-hidden shrink-0`}>
        <div className="flex items-center gap-8 z-10 w-full">
          <div className={`w-20 h-20 bg-white/5 border ${isDeceptionFlagged ? 'border-red-500/60' : themeBorder} p-2 relative group overflow-hidden ${!isActive && 'grayscale opacity-60'}`}>
            {institution.logoUrl
              ? <img src={institution.logoUrl} alt="logo" className="w-full h-full object-contain" />
              : getInstitutionIcon(institution.type, 32)}
          </div>

          <div className="flex-1">
            <div className="flex items-center gap-3 mb-1 flex-wrap">
              <button
                onClick={() => navigate(-1)}
                className="flex items-center gap-1 text-slate-500 hover:text-brand-accent transition-colors text-[8px] font-mono uppercase tracking-[0.2em] mr-1"
              >
                <ChevronLeft size={10} /> Back
              </button>
              <span className={`${isActive ? 'bg-brand-accent text-black' : 'bg-orange-600 text-white'} text-[10px] font-black px-2 py-0.5 uppercase italic`}>
                {isActive ? institution.level : 'DISSOLVED_UNIT'}
              </span>
              <div className="flex items-center gap-2 px-2 py-0.5 border border-white/10 bg-white/5">
                {getInstitutionIcon(institution.type, 12)}
                <span className="text-white text-[9px] font-black uppercase italic tracking-widest">{institution.type}</span>
              </div>
              {institution.stateOwned && (
                <div className="flex items-center gap-1.5 px-2 py-0.5 border border-blue-500/30 bg-blue-500/10 text-blue-400">
                  <Landmark size={10} />
                  <span className="text-[9px] font-black uppercase italic tracking-widest">State_Owned</span>
                </div>
              )}
              <VerificationBadge status={institution.verificationStatus} />
              {identifiers && identifiers.length > 0 && (
                <div className="flex items-center gap-1.5 px-2 py-0.5 border border-slate-700 bg-white/5 text-slate-400">
                  <Key size={10} />
                  <span className="text-[9px] font-mono">{identifiers.filter(i => i.active).length}_ID{identifiers.filter(i => i.active).length !== 1 ? 's' : ''}</span>
                </div>
              )}
            </div>
            <h1 className={`text-4xl font-black uppercase tracking-tighter leading-none italic ${isActive ? 'text-white' : 'text-slate-400'}`}>
              {institution.name}
            </h1>
            <div className="flex items-center gap-4 mt-2">
              <LocationPathDisplay path={institution.fullLocationPath} isLoading={isInstLoading} />
            </div>
          </div>

          <div className="flex gap-4">
            <button
              onClick={() => setIsNexusOpen(true)}
              className={`flex flex-col items-center justify-center w-28 h-20 transition-all border ${
                isActive ? 'bg-blue-600/5 border-blue-500/20 hover:bg-blue-600' : 'bg-slate-800/10 border-white/5 hover:bg-slate-700 text-slate-400'
              }`}
            >
              <Network size={20} className="mb-1" />
              <span className="text-[8px] font-black uppercase tracking-[0.2em] text-center">Open<br />Visual_Nexus</span>
            </button>
            <button
              onClick={() => navigate('/terminal')}
              className="p-4 bg-white/5 border border-white/10 hover:border-brand-accent text-slate-400"
            >
              <Search size={24} />
            </button>
          </div>
        </div>
      </header>

      {/* DECEPTION WARNING */}
      {isDeceptionFlagged && (
        <div className="bg-red-950/40 border-b border-red-600/40 px-8 py-2 flex items-center gap-3 shrink-0">
          <AlertTriangle size={14} className="text-red-500 shrink-0" />
          <span className="text-[10px] font-mono text-red-400 uppercase tracking-[0.2em]">
            WARNING: This record is flagged as a DECEPTION_MARKER. Information may be deliberately falsified or planted. Cross-reference before operational use.
          </span>
        </div>
      )}

      {/* MAIN CONTENT */}
      <main className="flex-1 grid grid-cols-12 overflow-hidden bg-[#09122a] min-h-0">

        {/* ASIDE */}
        <aside className="col-span-3 border-r border-white/5 bg-[#05080f] flex flex-col overflow-hidden p-6 gap-6">
          <section className="flex flex-col min-h-0 flex-1">
            <div className="flex items-center gap-3 mb-4 shrink-0">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isActive ? 'from-brand-accent/40' : 'from-orange-500/40'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <ListTree size={12} /> Structural_Chain
              </h3>
            </div>
            <div className={`flex flex-col gap-4 relative pl-4 border-l flex-1 min-h-0 ${isActive ? 'border-brand-accent/20' : 'border-orange-500/20'}`}>
              <div className="shrink-0">
                <span className="text-[8px] font-mono text-slate-600 uppercase block mb-1 italic">Immediate_Superior</span>
                <div className={`p-3 bg-black border border-white/5 border-l-2 ${isActive ? 'border-l-brand-accent' : 'border-l-orange-500'}`}>
                  {institution.parentId ? (
                    <Link to={`/institutions/${institution.parentId}`} className="block text-[11px] font-black text-white hover:text-brand-accent uppercase italic truncate transition-colors">
                      {ascendedPath?.parent?.name || 'SYNCING_NODE...'}
                    </Link>
                  ) : <span className="text-[11px] text-slate-600 font-mono italic">ROOT_NODE_AUTHORITY</span>}
                </div>
              </div>

              <div className="flex flex-col min-h-0 flex-1">
                <div className="flex justify-between items-end mb-2 shrink-0">
                  <span className="text-[8px] font-mono text-slate-600 uppercase italic">Subordinate_Units</span>
                  <span className={`text-[10px] font-mono ${themeColor} px-2 bg-white/5 border border-white/10`}>{subordinates.length}</span>
                </div>
                <div className="flex-1 overflow-y-auto custom-scrollbar pr-1 space-y-1 min-h-0">
                  {subordinates.map((child) => (
                    <Link
                      key={child.publicId}
                      to={`/institutions/${child.publicId}`}
                      className={`flex items-center justify-between p-2 border transition-all group ${
                        child.active
                          ? 'bg-white/[0.02] border-white/5 hover:border-brand-accent/30'
                          : 'bg-orange-500/5 border-orange-500/20 hover:border-orange-500/50'
                      }`}
                    >
                      <div className="flex flex-col truncate pr-4">
                        <span className={`text-[9px] uppercase italic transition-colors truncate ${
                          child.active ? 'text-slate-400 group-hover:text-white' : 'text-orange-400 group-hover:text-orange-300'
                        }`}>{child.name}</span>
                        {!child.active && (
                          <span className="text-[7px] font-mono text-orange-600 uppercase italic flex items-center gap-1">
                            <History size={8} /> DISSOLVED_UNIT
                          </span>
                        )}
                      </div>
                      <ChevronRight size={10} className={child.active ? 'text-slate-700 group-hover:text-brand-accent' : 'text-orange-900 group-hover:text-orange-500'} />
                    </Link>
                  ))}
                </div>
              </div>
            </div>
          </section>

          <section className="shrink-0">
            <div className="flex items-center gap-3 mb-4">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isActive ? 'from-brand-accent/50' : 'from-orange-500/50'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <Info size={12} /> Tactical_Briefing
              </h3>
            </div>
            <div className="bg-black/40 p-4 border border-white/5">
              <p className="text-[11px] leading-relaxed text-slate-400 font-serif italic mb-4">
                {institution.description || 'REPORT_SECURE: No brief provided.'}
              </p>
              <div className="space-y-3 pt-4 border-t border-white/5">
                <MetaField label="Deployment_Sector" value={institution.locationName} icon={<MapPin size={10} />} />
                <MetaField label="Node_Status" value={isActive ? 'OPERATIONAL' : 'ARCHIVED'} icon={<Activity size={10} />} />
                <MetaField label="Verification" value={institution.verificationStatus} icon={<ShieldCheck size={10} />} />
                <MetaField label="Clearance" value={institution.clearanceLevel} icon={<Lock size={10} />} />
              </div>
            </div>
          </section>
        </aside>

        {/* MAIN PANEL */}
        <section className="col-span-9 flex flex-col bg-[#02040a] relative overflow-hidden">

          {/* TAB NAVIGATION */}
          <div className="flex items-center border-b border-white/5 px-8 pt-4 gap-1 shrink-0">
            <TabButton active={activeTab === 'personnel'} onClick={() => setActiveTab('personnel')} icon={<Users size={14} />} label="Personnel_Registry" />
            <TabButton
              active={activeTab === 'contracts'}
              onClick={() => setActiveTab('contracts')}
              icon={<FileText size={14} />}
              label="Public_Contracts"
              badge={!hasContractableIdentifier ? '⚠' : undefined}
            />
            <TabButton
              active={activeTab === 'identifiers'}
              onClick={() => setActiveTab('identifiers')}
              icon={<Key size={14} />}
              label="Identifiers"
              badge={identifiers?.filter(i => i.active).length.toString()}
            />
            <TabButton
              active={activeTab === 'orgchart'}
              onClick={() => setActiveTab('orgchart')}
              icon={<Network size={14} />}
              label="Org_Chart"
            />
          </div>

          <div className={`flex-1 z-10 ${activeTab === 'orgchart' ? 'overflow-hidden flex flex-col min-h-0' : 'overflow-y-auto p-8 custom-scrollbar'}`}>

            {/* PERSONNEL TAB */}
            {activeTab === 'personnel' && (
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-[9px] font-mono text-slate-500 uppercase">
                    {displayedOccupations.length} position{displayedOccupations.length !== 1 ? 's' : ''}
                    {vacancyOnly ? ' — vacancies only' : ''}
                  </span>
                  <button
                    onClick={() => setVacancyOnly(v => !v)}
                    className={`flex items-center gap-2 px-3 py-1.5 text-[9px] font-mono font-bold uppercase tracking-widest border transition-all ${
                      vacancyOnly
                        ? 'bg-amber-500/20 border-amber-500/50 text-amber-400'
                        : 'bg-black/40 border-white/10 text-slate-500 hover:border-amber-500/40 hover:text-amber-400'
                    }`}
                  >
                    {vacancyOnly ? <Eye size={11} /> : <EyeOff size={11} />}
                    {vacancyOnly ? 'Vacancies_Only' : 'Show_All'}
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                  {displayedOccupations.map(occ => (
                    <OccupationCard
                      key={occ.publicId}
                      occupation={occ}
                      isActiveNode={isActive}
                      onClick={() => navigate(`/occupations/${occ.publicId}`)}
                    />
                  ))}
                </div>

                {displayedOccupations.length === 0 && vacancyOnly && (
                  <div className="py-20 text-center border border-dashed border-white/10">
                    <span className="text-[9px] font-mono text-slate-700 uppercase tracking-widest">
                      No_Vacancies — All_Positions_Filled
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* CONTRACTS TAB */}
            {activeTab === 'contracts' && (
              <div>
                {!hasContractableIdentifier ? (
                  <div className="flex flex-col items-center justify-center py-24 gap-4 border border-dashed border-white/10 rounded">
                    <Key size={36} className="text-slate-700" />
                    <p className="text-[11px] font-mono text-slate-500 uppercase tracking-widest text-center max-w-sm">
                      No_Registration_Identifier // Add a national registration number on the Identifiers tab to enable contract linkage
                    </p>
                    <button
                      onClick={() => setActiveTab('identifiers')}
                      className="px-4 py-2 border border-brand-accent/30 text-brand-accent text-[10px] font-mono uppercase hover:bg-brand-accent/10 transition-all"
                    >
                      Go_To_Identifiers →
                    </button>
                  </div>
                ) : (
                  <>
                    {/* Search + refresh bar */}
                    <div className="flex items-center justify-between mb-6 gap-4">
                      <div className="flex items-center gap-2 flex-1 max-w-sm bg-black/40 border border-white/10 px-3 py-2 focus-within:border-brand-accent/50 transition-colors">
                        <Search size={12} className="text-slate-600 shrink-0" />
                        <input
                          type="text"
                          placeholder="Search all contracts..."
                          value={counterpartySearch}
                          onChange={e => setCounterpartySearch(e.target.value)}
                          className="bg-transparent flex-1 text-[10px] font-mono text-slate-300 outline-none placeholder:text-slate-700 tracking-wider"
                        />
                        {counterpartySearch && (
                          <button onClick={() => setCounterpartySearch('')} className="text-slate-600 hover:text-red-400 transition-colors">
                            <X size={10} />
                          </button>
                        )}
                      </div>
                      <button
                        onClick={() => fetchContracts.mutate()}
                        disabled={fetchContracts.isPending}
                        className="flex items-center gap-2 px-4 py-1.5 border border-white/10 text-[9px] font-mono text-slate-400 hover:border-brand-accent hover:text-brand-accent uppercase transition-all shrink-0"
                      >
                        <RefreshCw size={12} className={fetchContracts.isPending ? 'animate-spin' : ''} />
                        Refresh_From_Source
                      </button>
                    </div>

                    {/* Results */}
                    {isContractsLoading ? (
                      <div className="flex items-center justify-center py-20 font-mono text-[10px] text-brand-accent animate-pulse uppercase tracking-widest">
                        Querying_Contract_Registry...
                      </div>
                    ) : contracts.length === 0 ? (
                      <div className="flex flex-col items-center justify-center py-20 gap-3 border border-dashed border-white/10 rounded">
                        <FileText size={32} className="text-slate-700" />
                        <span className="text-[10px] font-mono text-slate-600 uppercase tracking-widest">
                          {debouncedSearch
                            ? `No_Contracts_Matching // "${debouncedSearch}"`
                            : 'No_Contracts_Found // Try Refresh_From_Source'}
                        </span>
                        {debouncedSearch && (
                          <button
                            onClick={() => setCounterpartySearch('')}
                            className="mt-1 text-[9px] font-mono text-brand-accent/70 hover:text-brand-accent border border-brand-accent/20 px-3 py-1.5 transition-colors uppercase"
                          >
                            Clear_Search
                          </button>
                        )}
                      </div>
                    ) : (
                      <div className="space-y-2">
                        {contracts.map((contract: ExternalContractResponse) => (
                          <ContractRow key={contract.id} contract={contract} />
                        ))}
                      </div>
                    )}

                    {/* Pagination */}
                    {contractsPage && contractsPage.totalPages > 1 && (
                      <div className="flex items-center justify-between mt-6 font-mono text-[9px] text-slate-500 uppercase">
                        <span>
                          {contractsPage.totalElements} contract{contractsPage.totalElements !== 1 ? 's' : ''}
                          {debouncedSearch && <span className="text-slate-700"> — filtered</span>}
                        </span>
                        <div className="flex items-center gap-2">
                          <button
                            disabled={contractPage === 0}
                            onClick={() => setContractPage(p => p - 1)}
                            className="px-3 py-1 border border-white/10 disabled:opacity-30 hover:border-white/30 transition-all"
                          >
                            ← Prev
                          </button>
                          <span className="px-3 py-1 border border-white/5 bg-white/[0.02]">
                            {contractPage + 1} / {contractsPage.totalPages}
                          </span>
                          <button
                            disabled={contractPage >= contractsPage.totalPages - 1}
                            onClick={() => setContractPage(p => p + 1)}
                            className="px-3 py-1 border border-white/10 disabled:opacity-30 hover:border-white/30 transition-all"
                          >
                            Next →
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}

            {/* IDENTIFIERS TAB */}
            {activeTab === 'identifiers' && (
              <IdentifiersPanel
                identifiers={identifiers || []}
                isLoading={isIdentifiersLoading}
                onAdd={(req) => addIdentifier.mutate(req)}
                onDeactivate={(iid) => deactivateIdentifier.mutate(iid)}
                isAdding={addIdentifier.isPending}
              />
            )}

            {/* ORG CHART TAB */}
            {activeTab === 'orgchart' && (
              <OrgChartTab
                institutionId={id!}
                institutionName={institution.name}
              />
            )}
          </div>
        </section>
      </main>

      {/* NEXUS MODAL */}
      {isNexusOpen && (
        <div className="fixed inset-0 z-[100] bg-[#02040a]/98 backdrop-blur-3xl flex flex-col">
          <div className="h-16 border-b border-white/10 flex justify-between items-center px-8 bg-black/90 shrink-0">
            <div className="flex items-center gap-4">
              <Network className={themeColor} size={24} />
              <h2 className="text-[11px] font-black text-white uppercase tracking-[0.4em]">Visual_Nexus_Terminal</h2>
            </div>
            <button onClick={() => setIsNexusOpen(false)} className="px-6 py-2 border border-white/10 hover:text-red-500 transition-all">
              <X size={20} />
            </button>
          </div>
          <div className="flex-1 min-h-0">
            <InstitutionFlow data={nexusData} />
          </div>
        </div>
      )}
    </div>
  );
};

// =========================================================================
// SUB-COMPONENTS
// =========================================================================

const TabButton = ({ active, onClick, icon, label, badge }: {
  active: boolean; onClick: () => void; icon: React.ReactNode; label: string; badge?: string;
}) => (
  <button
    onClick={onClick}
    className={`flex items-center gap-2 px-4 py-2 text-[10px] font-mono uppercase tracking-widest border-b-2 transition-all ${
      active ? 'border-brand-accent text-brand-accent' : 'border-transparent text-slate-500 hover:text-slate-300'
    }`}
  >
    {icon} {label}
    {badge && (
      <span className={`px-1.5 py-0.5 rounded text-[8px] font-black ${
        badge === '⚠' ? 'bg-amber-500/20 text-amber-400' : 'bg-white/10 text-slate-400'
      }`}>
        {badge}
      </span>
    )}
  </button>
);

const ContractRow = ({ contract }: { contract: ExternalContractResponse }) => {
  const roleColor = contract.role === 'BUYER'
    ? 'text-blue-400 border-blue-500/30 bg-blue-500/5'
    : contract.role === 'SUPPLIER'
      ? 'text-emerald-400 border-emerald-500/30 bg-emerald-500/5'
      : 'text-amber-400 border-amber-500/30 bg-amber-500/5';

  const amountColor = contract.role === 'BUYER'
    ? 'text-orange-400'
    : contract.role === 'SUPPLIER'
      ? 'text-emerald-400'
      : 'text-slate-300';

  const formatAmount = (amount: number | null) => {
    if (!amount) return '—';
    return new Intl.NumberFormat('cs-CZ', { style: 'currency', currency: 'CZK', maximumFractionDigits: 0 }).format(amount);
  };

  return (
    <div className="group flex items-start gap-4 p-4 bg-white/[0.02] border border-white/5 hover:border-white/20 transition-all">
      <div className="shrink-0 mt-0.5">
        {contract.role === 'BUYER'
          ? <TrendingDown size={16} className="text-orange-400" />
          : contract.role === 'SUPPLIER'
            ? <TrendingUp size={16} className="text-emerald-400" />
            : <Minus size={16} className="text-amber-400" />}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-4">
          <p className="text-[11px] text-slate-300 font-medium leading-snug truncate flex-1">
            {contract.subjectText || 'No subject recorded'}
          </p>
          <span className={`shrink-0 text-[8px] font-black uppercase px-2 py-0.5 border rounded-sm ${roleColor}`}>
            {contract.role}
          </span>
        </div>
        <div className="flex items-center gap-6 mt-2 font-mono text-[9px] text-slate-500 uppercase flex-wrap">
          <span className="flex items-center gap-1">
            <FileText size={9} /> {contract.contractDate || '—'}
          </span>
          <span className={`font-black ${amountColor}`}>{formatAmount(contract.amountTotal)}</span>
          {contract.role === 'BUYER'
            ? <span>Supplier: <span className="text-slate-400">{contract.supplierName || contract.supplierIco || '—'}</span></span>
            : <span>Buyer: <span className="text-slate-400">{contract.buyerName || contract.buyerIco || '—'}</span></span>}
          <span className="text-slate-700">{contract.sourceSystem}</span>
        </div>
      </div>
    </div>
  );
};

const IdentifiersPanel = ({ identifiers, isLoading, onAdd, onDeactivate, isAdding }: {
  identifiers: InstitutionIdentifierResponse[];
  isLoading: boolean;
  onAdd: (req: InstitutionIdentifierRequest) => void;
  onDeactivate: (id: string) => void;
  isAdding: boolean;
}) => {
  const [type, setType] = useState<IdentifierType>('NATIONAL_REGISTRATION');
  const [value, setValue] = useState('');
  const [countryCode, setCountryCode] = useState('CZ');
  const [note, setNote] = useState('');

  const handleAdd = () => {
    if (!value.trim()) return;
    onAdd({ type, value: value.trim(), countryCode: countryCode.trim() || undefined, note: note.trim() || undefined });
    setValue('');
    setNote('');
  };

  const IDENTIFIER_LABELS: Record<IdentifierType, string> = {
    NATIONAL_REGISTRATION: 'National Registration (IČO, SIREN...)',
    VAT:                   'VAT Number (DIČ...)',
    LEI:                   'LEI — ISO 17442',
    DUNS:                  'D-U-N-S Number',
    EUID:                  'EUID (EU Unique ID)',
    GLEIF_RELATIONSHIP:    'GLEIF Relationship',
    CUSTOM:                'Custom',
  };

  return (
    <div className="space-y-8">
      <div>
        <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] mb-4 flex items-center gap-2">
          <Key size={12} /> Registered_Identifiers
        </h3>
        {isLoading ? (
          <div className="text-[10px] font-mono text-slate-600 animate-pulse">Loading...</div>
        ) : identifiers.length === 0 ? (
          <div className="py-8 text-center border border-dashed border-white/10 rounded">
            <span className="text-[9px] font-mono text-slate-700 uppercase tracking-widest">No_Identifiers_Registered</span>
          </div>
        ) : (
          <div className="space-y-2">
            {identifiers.map(ident => (
              <div key={ident.id} className={`flex items-center gap-4 p-3 border transition-all ${
                ident.active ? 'border-white/10 bg-white/[0.02]' : 'border-white/5 bg-black/20 opacity-50'
              }`}>
                <div className={`shrink-0 ${ident.active ? 'text-emerald-500' : 'text-slate-600'}`}>
                  {ident.active ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3">
                    <span className="text-[9px] font-mono text-slate-500 uppercase">{ident.type}</span>
                    {ident.countryCode && (
                      <span className="text-[8px] px-1.5 py-0.5 border border-white/10 bg-white/5 font-mono text-slate-500">{ident.countryCode}</span>
                    )}
                    {!ident.active && <span className="text-[8px] font-mono text-orange-500 uppercase">Inactive</span>}
                  </div>
                  <div className="text-[11px] font-mono font-black text-white mt-0.5">{ident.value}</div>
                  {ident.note && <div className="text-[9px] text-slate-600 italic mt-0.5">{ident.note}</div>}
                </div>
                {ident.active && (
                  <button
                    onClick={() => onDeactivate(ident.id)}
                    className="shrink-0 p-1.5 border border-white/10 hover:border-red-500/50 hover:text-red-500 text-slate-600 transition-all rounded"
                  >
                    <Trash2 size={12} />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="border border-white/10 p-6 bg-black/20">
        <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] mb-4 flex items-center gap-2">
          <Plus size={12} /> Add_Identifier
        </h3>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className="flex flex-col gap-1">
            <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Identifier_Type</label>
            <select value={type} onChange={e => setType(e.target.value as IdentifierType)}
              className="bg-black border border-white/10 py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50">
              {Object.entries(IDENTIFIER_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Country_Code (ISO 3166)</label>
            <input value={countryCode} onChange={e => setCountryCode(e.target.value.toUpperCase().slice(0, 2))}
              placeholder="CZ" maxLength={2}
              className="bg-black border border-white/10 py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50 uppercase"
            />
          </div>
          <div className="flex flex-col gap-1 col-span-2">
            <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Value</label>
            <input value={value} onChange={e => setValue(e.target.value)}
              placeholder="e.g. 00006947 for IČO, 20-digit code for LEI..."
              className="bg-black border border-white/10 py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50"
            />
          </div>
          <div className="flex flex-col gap-1 col-span-2">
            <label className="text-[8px] font-mono text-slate-500 uppercase tracking-widest">Note (optional)</label>
            <input value={note} onChange={e => setNote(e.target.value)}
              placeholder="e.g. source, sub-register, reason for CUSTOM type..."
              className="bg-black border border-white/10 py-2 px-3 text-[10px] font-mono text-slate-300 outline-none focus:border-brand-accent/50"
            />
          </div>
        </div>
        <button onClick={handleAdd} disabled={!value.trim() || isAdding}
          className="flex items-center gap-2 px-6 py-2 bg-brand-accent text-black font-black text-[10px] uppercase tracking-widest hover:bg-white transition-all disabled:opacity-40">
          <Plus size={14} /> {isAdding ? 'Adding...' : 'Register_Identifier'}
        </button>
      </div>
    </div>
  );
};

// =========================================================================
// TYPES / HELPERS
// =========================================================================

interface SubordinateSummary { publicId: string; name: string; active: boolean; type: string; }

const MetaField = ({ label, value, icon }: { label: string; value: React.ReactNode; icon: React.ReactNode }) => (
  <div className="flex items-center justify-between text-[10px] border-b border-white/[0.03] pb-2">
    <div className="flex items-center gap-2 text-slate-500 uppercase font-mono italic">{icon} <span>{label}</span></div>
    <div className="text-slate-200 font-black uppercase italic truncate ml-4">{value || 'N/A'}</div>
  </div>
);

const OccupationCard = ({ occupation, onClick, isActiveNode }: {
  occupation: OccupationResponse; onClick: () => void; isActiveNode: boolean;
}) => (
  <div
    onClick={onClick}
    className={`group bg-black border ${
      occupation.vacant
        ? 'border-amber-500/30 hover:border-amber-500/60'
        : isActiveNode ? 'border-white/5 hover:border-brand-accent/50' : 'border-white/5 hover:border-orange-500/50'
    } p-4 cursor-crosshair transition-all relative`}
  >
    {occupation.vacant && (
      <div className="absolute top-2 right-2 flex items-center gap-1 text-[8px] font-black uppercase text-amber-500">
        <EyeOff size={10} /> Vacant
      </div>
    )}
    <div className="flex items-center gap-4">
      <div className="w-12 h-12 flex items-center justify-center border border-white/10 bg-[#05080f]">
        <Briefcase size={20} className={occupation.vacant ? 'text-amber-600' : 'text-slate-600'} />
      </div>
      <div className="min-w-0">
        <div className="text-[11px] font-black text-white uppercase italic truncate">{occupation.title}</div>
        {!occupation.vacant && occupation.currentOccupantName && (
          occupation.currentOccupantId ? (
            <Link
              to={`/personnel/${occupation.currentOccupantId}`}
              onClick={e => e.stopPropagation()}
              className="flex items-center gap-1 text-[9px] font-mono text-brand-accent/70 hover:text-brand-accent uppercase truncate transition-colors"
            >
              {occupation.currentOccupantName}
              <ExternalLink size={8} />
            </Link>
          ) : (
            <div className="text-[9px] font-mono text-slate-500 uppercase truncate">{occupation.currentOccupantName}</div>
          )
        )}
      </div>
    </div>
  </div>
);

const VerificationBadge = ({ status }: { status: VerificationStatus | undefined }) => {
  const config: Partial<Record<VerificationStatus, { label: string; classes: string; icon: React.ReactNode }>> = {
    OFFICIAL_REGISTRY:   { label: 'Official_Registry',   classes: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400', icon: <ShieldCheck size={10} /> },
    TECHNICAL_INTERCEPT: { label: 'Technical_Intercept', classes: 'border-cyan-500/30 bg-cyan-500/10 text-cyan-400',          icon: <Eye size={10} /> },
    VETTED_HUMINT:       { label: 'Vetted_Humint',       classes: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400', icon: <ShieldCheck size={10} /> },
    VERIFIED_OSINT:      { label: 'Verified_Osint',      classes: 'border-blue-500/30 bg-blue-500/10 text-blue-400',          icon: <ShieldCheck size={10} /> },
    UNVERIFIED:          { label: 'Unverified',          classes: 'border-slate-500/30 bg-slate-500/10 text-slate-400',       icon: <ShieldAlert size={10} /> },
    DECEPTION_MARKER:    { label: 'Deception_Marker',    classes: 'border-red-500/50 bg-red-500/20 text-red-400 animate-pulse', icon: <AlertTriangle size={10} /> },
    EXPIRED_DEPRECATING: { label: 'Expired',             classes: 'border-orange-500/30 bg-orange-500/10 text-orange-400',    icon: <History size={10} /> },
    PENDING_REVIEW:      { label: 'Pending_Review',      classes: 'border-yellow-500/30 bg-yellow-500/10 text-yellow-400',    icon: <AlertTriangle size={10} /> },
    CONFLICTING:         { label: 'Conflicting',         classes: 'border-red-500/30 bg-red-500/10 text-red-400',             icon: <AlertTriangle size={10} /> },
  };
  const c = config[status ?? 'UNVERIFIED'] ?? config.UNVERIFIED!;
  return (
    <div className={`flex items-center gap-1.5 px-2 py-0.5 border ${c.classes}`}>
      {c.icon}
      <span className="text-[9px] font-black uppercase italic tracking-widest">{c.label}</span>
    </div>
  );
};

const getClearanceWeight = (level: ClearanceLevel): string =>
  ({ LEVEL_1_PUBLIC: '01', LEVEL_2_INTERNAL: '02', LEVEL_3_CONFIDENTIAL: '03', LEVEL_4_SECRET: '04', LEVEL_5_TOP_SECRET: '05' })[level] ?? '00';

const getClearanceColor = (level: ClearanceLevel): string =>
  ({ LEVEL_1_PUBLIC: 'text-slate-500', LEVEL_2_INTERNAL: 'text-blue-400', LEVEL_3_CONFIDENTIAL: 'text-amber-400', LEVEL_4_SECRET: 'text-orange-500', LEVEL_5_TOP_SECRET: 'text-red-500' })[level] ?? 'text-slate-500';

const LocationPathDisplay = ({ path, isLoading }: { path: LocationResponse[] | null | undefined; isLoading: boolean }) => {
  if (isLoading) return <div className="h-2 w-48 bg-white/5 animate-pulse" />;
  if (!path || path.length === 0) return <span className="text-[9px] font-mono text-slate-600 italic">UNKNOWN_LOCATION</span>;
  return (
    <div className="flex items-center gap-1.5 font-mono text-[9px] uppercase italic text-slate-500 flex-wrap">
      <Globe size={10} className="text-brand-accent shrink-0" />
      {path.map((loc, i) => (
        <React.Fragment key={loc.externalId || i}>
          <Link
            to={`/locations/${loc.externalId}`}
            className="hover:text-brand-accent transition-colors cursor-pointer text-slate-400 font-bold"
            title={`Type: ${loc.type} ${loc.isoCode ? `(${loc.isoCode})` : ''}`}
          >
            {loc.name}
          </Link>
          {i < path.length - 1 && <span className="text-slate-700 mx-0.5">/</span>}
        </React.Fragment>
      ))}
    </div>
  );
};

const LoadingState = () => (
  <div className="h-full bg-[#02040a] flex items-center justify-center text-brand-accent font-mono">LOADING...</div>
);
const ErrorState = ({ message }: { message: string }) => (
  <div className="h-full bg-[#02040a] flex items-center justify-center text-red-500 font-mono">{message}</div>
);

export default InstitutionDetail;