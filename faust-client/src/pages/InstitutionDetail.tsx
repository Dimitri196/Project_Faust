import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Building2, Users, MapPin, ExternalLink, ChevronLeft, Briefcase, 
  Info, ShieldAlert, ShieldCheck, Landmark, Zap, Scale, Network, 
  Globe, Fingerprint, Link as LinkIcon, ChevronRight, ListTree
} from 'lucide-react';
import axios from 'axios';
import type {
  InstitutionResponse,
  OccupationResponse,
  InstitutionType,
  LocationResponse
} from '../types';

// --- UTILS ---
const getInstitutionIcon = (type: InstitutionType | undefined, size = 16) => {
  const iconClass = "text-slate-400";
  switch (type) {
    case 'INTELLIGENCE': return <ShieldAlert size={size} className="text-red-500" />;
    case 'MILITARY': return <ShieldCheck size={size} className="text-amber-600" />;
    case 'LEGISLATIVE': return <Landmark size={size} className={iconClass} />;
    case 'REGULATORY': return <Zap size={size} className="text-blue-500" />;
    case 'JUDICIAL': return <Scale size={size} className={iconClass} />;
    case 'EXECUTIVE': return <Building2 size={size} className="text-brand-accent" />;
    default: return <Building2 size={size} className={iconClass} />;
  }
};

const InstitutionDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: institution, isLoading: isInstLoading, isError: isInstError } = useQuery<InstitutionResponse>({
    queryKey: ['institution', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/institutions/${id}`);
      return res.data;
    }
  });

  const { data: parentNode } = useQuery<InstitutionResponse>({
    queryKey: ['institution', institution?.parentId],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/institutions/${institution?.parentId}`);
      return res.data;
    },
    enabled: !!institution?.parentId
  });

  const { data: childNodes, isLoading: isChildrenLoading } = useQuery<InstitutionResponse[]>({
    queryKey: ['institution-children', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/institutions/search`, { params: { parentId: id } });
      return res.data;
    },
    enabled: !!id && institution?.hasChildren === true
  });

  const { data: occupations, isLoading: isOccLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['institution-occupations', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/occupations/institution/${id}`);
      return res.data;
    },
    enabled: !!id
  });

  if (isInstLoading) return <LoadingState />;
  if (isInstError || !institution) return <ErrorState message="NODE_OFFLINE_OR_UNAUTHORIZED" />;

  return (
    <div className="h-full bg-[#0f172a] text-slate-200 flex flex-col">
      
      {/* PROFESSIONAL HEADER */}
      <div className="bg-slate-900/50 border-b border-slate-800 p-8 shadow-sm">
        <div className="max-w-7xl mx-auto">
          <button 
            onClick={() => navigate(-1)} 
            className="flex items-center gap-2 text-slate-500 hover:text-white transition-colors mb-6 font-mono text-[10px] uppercase tracking-widest group"
          >
            <ChevronLeft size={14} className="group-hover:-translate-x-1 transition-transform" /> Back to Directory
          </button>

          <div className="flex flex-col md:flex-row items-center justify-between gap-8">
            <div className="flex flex-col md:flex-row gap-8 items-center">
              {/* Logo Box - Clean & Stable */}
              <div className="w-28 h-28 bg-white p-4 rounded shadow-lg flex items-center justify-center shrink-0 border border-slate-700">
                {institution.logoUrl ? (
                  <img src={institution.logoUrl} alt="logo" className="w-full h-full object-contain" />
                ) : (
                  getInstitutionIcon(institution.type, 48)
                )}
              </div>

              <div className="text-center md:text-left">
                <div className="flex items-center justify-center md:justify-start gap-2 mb-2">
                  <span className="px-2 py-0.5 bg-brand-accent/10 border border-brand-accent/30 text-[9px] font-bold text-brand-accent uppercase rounded">
                    {institution.level}
                  </span>
                  <span className="text-slate-500 text-[10px] font-mono tracking-tighter">
                    {institution.type} // {institution.countryCode}
                  </span>
                </div>
                <h1 className="text-4xl font-bold text-white tracking-tight mb-2 uppercase">
                  {institution.name}
                </h1>
                <div className="flex items-center justify-center md:justify-start gap-2 text-slate-400">
                  <Globe size={14} className="text-brand-accent" />
                  <LocationPathDisplay path={institution.fullLocationPath} isLoading={isInstLoading} />
                </div>
              </div>
            </div>

            {/* System Info Stats */}
            <div className="hidden lg:flex gap-10 border-l border-slate-800 pl-10">
              <div className="text-center">
                <div className="text-slate-500 text-[9px] uppercase tracking-widest mb-1">Status</div>
                <div className="text-emerald-500 text-xs font-bold flex items-center gap-2 justify-center">
                  <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full" /> ACTIVE
                </div>
              </div>
              <div className="text-center">
                <div className="text-slate-500 text-[9px] uppercase tracking-widest mb-1">Node ID</div>
                <div className="text-slate-300 text-xs font-mono font-bold">{institution.publicId.split('-')[0]}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* LEFT: HIERARCHY & SPECS (Col 4) */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* Hierarchical Chain Section */}
            <section className="bg-slate-900/40 border border-slate-800 rounded-lg p-6">
              <h3 className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-6 flex items-center gap-2">
                <ListTree size={14} /> Structural Hierarchy
              </h3>

              <div className="relative space-y-4">
                {/* Visual Line Connectors */}
                <div className="absolute left-4 top-4 bottom-4 w-px bg-slate-800" />

                {/* Parent Node */}
                <div className="relative pl-10">
                  <div className="absolute left-[13px] top-3 w-1.5 h-1.5 rounded-full bg-slate-700" />
                  <span className="text-[9px] font-mono text-slate-500 uppercase block mb-1">Superior Authority</span>
                  {institution.parentId ? (
                    <Link to={`/institutions/${institution.parentId}`} className="block p-3 bg-slate-800/50 border border-slate-700 hover:border-brand-accent/50 rounded transition-all">
                      <div className="text-[11px] font-bold text-white truncate">{parentNode?.name || 'Loading...'}</div>
                    </Link>
                  ) : (
                    <div className="text-[10px] text-slate-600 italic">Root Authority Level</div>
                  )}
                </div>

                {/* Current Node Marker */}
                <div className="relative pl-10">
                  <div className="absolute left-[11px] top-2 w-2.5 h-2.5 rounded-full bg-brand-accent border-2 border-[#0f172a]" />
                  <div className="p-3 bg-brand-accent/5 border border-brand-accent/20 rounded">
                    <span className="text-[11px] font-bold text-brand-accent">Current Node (This Station)</span>
                  </div>
                </div>

                {/* Subordinate Nodes */}
                <div className="relative pl-10">
                  <div className="absolute left-[13px] top-3 w-1.5 h-1.5 rounded-full bg-slate-700" />
                  <span className="text-[9px] font-mono text-slate-500 uppercase block mb-1">Subordinate Units</span>
                  {institution.hasChildren ? (
                    <div className="space-y-2 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                      {isChildrenLoading ? <div className="animate-pulse h-10 bg-slate-800" /> : childNodes?.map(child => (
                        <Link key={child.publicId} to={`/institutions/${child.publicId}`} className="flex items-center justify-between p-2 bg-slate-800/30 border border-slate-700 hover:border-slate-500 rounded text-[10px] transition-all">
                          <span className="truncate">{child.name}</span>
                          <ChevronRight size={12} />
                        </Link>
                      ))}
                    </div>
                  ) : (
                    <div className="text-[10px] text-slate-600 italic">No subordinate entities</div>
                  )}
                </div>
              </div>
            </section>

            {/* Entity Briefing */}
            <section className="bg-slate-900/40 border border-slate-800 rounded-lg p-6">
              <h3 className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
                <Info size={14} /> Description
              </h3>
              <p className="text-sm text-slate-400 leading-relaxed font-light">
                {institution.description || "Official classification data not provided for this entity."}
              </p>
              
              <div className="mt-6 pt-6 border-t border-slate-800 space-y-3">
                <MetaField label="Sector" value={institution.locationName} icon={<MapPin size={12}/>} />
                <MetaField label="Type" value={institution.isStateOwned ? 'State Entity' : 'Private Entity'} icon={<Network size={12}/>} />
              </div>
            </section>
          </div>

          {/* RIGHT: PERSONNEL (Col 8) */}
          <div className="lg:col-span-8">
            <section className="bg-slate-900/40 border border-slate-800 rounded-lg overflow-hidden">
              <div className="p-6 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
                <h3 className="text-[11px] font-bold text-white uppercase tracking-[0.2em] flex items-center gap-2">
                  <Users size={16} className="text-brand-accent" /> Personnel & Deployment
                </h3>
                <span className="text-[10px] font-mono text-slate-500">
                  Total Slots: <span className="text-white">{occupations?.length || 0}</span>
                </span>
              </div>

              <div className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {isOccLoading ? (
                    <LoadingPersonnel />
                  ) : occupations && occupations.length > 0 ? (
                    occupations.map((occ) => (
                      <OccupationCard key={occ.publicId} occupation={occ} onClick={() => navigate(`/occupations/${occ.publicId}`)} />
                    ))
                  ) : (
                    <div className="col-span-full py-20 text-center text-slate-600 font-mono text-xs uppercase italic border border-dashed border-slate-800">
                      No personnel assigned to this node
                    </div>
                  )}
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

// --- SUBSIDIARY COMPONENTS ---

const MetaField = ({ label, value, icon }: { label: string, value: string | null | undefined, icon?: React.ReactNode }) => (
  <div className="flex items-center justify-between text-[11px]">
    <div className="flex items-center gap-2 text-slate-500 uppercase font-medium">{icon} {label}</div>
    <div className="text-slate-200 font-bold">{value || '---'}</div>
  </div>
);

const LocationPathDisplay = ({ path, isLoading }: { path?: LocationResponse[] | null, isLoading: boolean }) => {
  if (isLoading) return <div className="h-3 w-32 bg-slate-800 animate-pulse rounded" />;
  if (!path) return <span className="text-slate-600 italic">Sector Unmapped</span>;
  return (
    <div className="flex items-center gap-1 font-mono text-[10px] uppercase">
      {path.map((loc, i) => (
        <React.Fragment key={loc.externalId}>
          <Link to={`/locations/${loc.externalId}`} className="hover:text-white transition-colors underline underline-offset-4 decoration-slate-700">{loc.name}</Link>
          {i < path.length - 1 && <span className="text-slate-700">/</span>}
        </React.Fragment>
      ))}
    </div>
  );
};

const OccupationCard = ({ occupation, onClick }: { occupation: OccupationResponse, onClick: () => void }) => (
  <div
    onClick={onClick}
    className="group bg-slate-900/80 border border-slate-800 hover:border-slate-600 p-4 rounded-md transition-all cursor-pointer flex items-center justify-between"
  >
    <div className="flex items-center gap-4">
      <div className={`w-10 h-10 flex items-center justify-center rounded border ${occupation.isVacant ? 'border-red-900/50 bg-red-950/20' : 'border-slate-700 bg-slate-800'}`}>
        <Briefcase size={18} className={occupation.isVacant ? 'text-red-800' : 'text-slate-400'} />
      </div>
      <div>
        <div className="text-xs font-bold text-white group-hover:text-brand-accent transition-colors">{occupation.title}</div>
        <div className="text-[9px] text-slate-500 uppercase mt-1 font-mono">{occupation.rank} // {occupation.code}</div>
      </div>
    </div>
    <div className="text-right shrink-0">
      <span className={`text-[8px] font-black px-2 py-1 rounded ${occupation.isVacant ? 'bg-red-900/20 text-red-500' : 'bg-emerald-900/20 text-emerald-500'}`}>
        {occupation.isVacant ? 'VACANT' : 'OCCUPIED'}
      </span>
    </div>
  </div>
);

// Loading & Error states
const LoadingState = () => (
  <div className="h-full bg-[#0f172a] flex flex-col items-center justify-center font-mono">
    <div className="w-12 h-12 border-2 border-slate-800 border-t-brand-accent rounded-full animate-spin mb-4" />
    <span className="text-[10px] text-slate-500 uppercase tracking-widest">Accessing Node Database...</span>
  </div>
);

const ErrorState = ({ message }: { message: string }) => (
  <div className="h-full bg-[#0f172a] flex flex-col items-center justify-center gap-4">
    <ShieldAlert size={40} className="text-red-500" />
    <div className="text-xs font-mono text-red-500">{message}</div>
    <Link to="/institutions" className="text-[10px] uppercase font-bold border border-slate-700 px-4 py-2 hover:bg-slate-800 transition-colors">Emergency Exit</Link>
  </div>
);

const LoadingPersonnel = () => (
  <div className="col-span-full py-10 flex flex-col items-center">
    <div className="w-8 h-8 border-2 border-slate-800 border-t-slate-500 rounded-full animate-spin mb-4" />
    <span className="text-[9px] text-slate-600 uppercase font-mono tracking-widest">Scanning Registry...</span>
  </div>
);

export default InstitutionDetail;