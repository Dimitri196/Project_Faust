import React, { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Building2, Users, MapPin,
  ExternalLink, ChevronLeft, Briefcase, Info,
  ShieldAlert, ShieldCheck, Landmark, Zap, Scale,
  Network, Globe, Fingerprint, Link as LinkIcon,
  Globe2
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
  switch (type) {
    case 'INTELLIGENCE': return <ShieldAlert size={size} className="text-red-500 animate-pulse" />;
    case 'MILITARY': return <ShieldCheck size={size} className="text-orange-500" />;
    case 'LEGISLATIVE': return <Landmark size={size} className="text-purple-400" />;
    case 'REGULATORY': return <Zap size={size} className="text-yellow-400" />;
    case 'JUDICIAL': return <Scale size={size} className="text-blue-400" />;
    case 'EXECUTIVE': return <Building2 size={size} className="text-brand-accent" />;
    default: return <Building2 size={size} className="text-slate-500" />;
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

  const { data: occupations, isLoading: isOccLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['institution-occupations', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/occupations/institution/${id}`);
      return res.data;
    },
    enabled: !!id
  });

  // Debugging log pro ověření dat z backendu
  useEffect(() => {
    if (institution) {
      console.log(`[FAUST_NODE_DIAGNOSTICS] ${institution.name}:`, {
        isStateOwned: institution.isStateOwned,
        website: institution.websiteUrl
      });
    }
  }, [institution]);

  if (isInstLoading) return <LoadingState />;
  if (isInstError || !institution) return <ErrorState message="NODE_OFFLINE_OR_UNAUTHORIZED" />;

  return (
    <div className="h-full bg-brand-dark text-slate-200 flex flex-col animate-in fade-in duration-500">

      {/* HEADER: Operational Context */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative overflow-hidden">
        <div className="absolute inset-0 opacity-5 pointer-events-none bg-[radial-gradient(#38bdf8_1px,transparent_1px)] [background-size:16px_16px]" />

        <div className="max-w-7xl mx-auto relative z-10">
          <Link to="/institutions" className="flex items-center gap-2 text-brand-accent/60 hover:text-brand-accent transition-colors mb-6 font-mono text-[10px] uppercase tracking-widest group">
            <ChevronLeft size={14} className="group-hover:-translate-x-1 transition-transform" /> Back_to_Nexus_Core
          </Link>

          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-8">
            <div className="flex flex-col md:flex-row gap-8 items-start md:items-center">
              <div className="relative group">
                <div className="absolute -inset-1 bg-brand-accent/20 rounded blur opacity-25 group-hover:opacity-50 transition duration-1000"></div>
                {institution.logoUrl ? (
                  <img
                    src={institution.logoUrl}
                    alt={`${institution.name} logo`}
                    className="relative w-28 h-28 object-contain bg-black/40 p-3 border border-brand-border/50 rounded shadow-2xl backdrop-blur-md"
                  />
                ) : (
                  <div className="relative w-28 h-28 bg-brand-panel/40 border border-brand-border/50 flex items-center justify-center rounded">
                    {getInstitutionIcon(institution.type, 48)}
                  </div>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  {getInstitutionIcon(institution.type, 20)}
                  <span className="px-2 py-0.5 bg-brand-accent/10 border border-brand-accent/30 text-[9px] font-mono text-brand-accent uppercase tracking-[0.2em]">
                    {institution.level} // {institution.type} // {institution.countryCode}
                  </span>
                </div>
                <div className="flex items-baseline gap-4">
                  <h1 className="text-5xl font-black text-white italic tracking-tighter uppercase leading-none">
                    {institution.name}
                  </h1>
                  
                </div>

                <div className="flex items-center gap-2 border-l-2 border-brand-accent/30 pl-4 py-1">
                  <Globe size={12} className="text-brand-accent/50" />
                  <LocationPathDisplay path={institution.fullLocationPath} isLoading={isInstLoading} />
                </div>
              </div>
            </div>

            <div className="flex gap-8 font-mono text-[10px]">
              <div className="text-right border-r border-brand-border/50 pr-8">
                <div className="text-slate-500 uppercase mb-1">Access_Status</div>
                <div className="text-brand-success flex items-center gap-2 justify-end font-bold">
                  <div className="w-1.5 h-1.5 bg-brand-success rounded-full animate-pulse" />
                  DATA_ENCRYPTED_LIVE
                </div>
              </div>
              <div className="text-right">
                <div className="text-slate-500 uppercase mb-1">Registry_UID</div>
                <div className="text-brand-accent font-bold tracking-tighter uppercase flex items-center gap-2">
                   <Fingerprint size={10} className="opacity-50" />
                   {institution.publicId.split('-')[0]}... [SECURED]
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">

          {/* LEFT COL: Metadata & Documentation */}
          <div className="lg:col-span-1 space-y-6">
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm relative group">
              <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.3em] mb-6 flex items-center gap-2">
                <Info size={14} className="text-brand-accent" /> Entity_Specifications
              </h3>

              <div className="space-y-4">
                <div className="py-3 border-b border-brand-border/20">
                  <div className="flex items-center gap-2 text-[10px] font-mono text-slate-500 uppercase tracking-tighter mb-1">
                    <MapPin size={12} className="text-brand-accent" /> Operational_Sector
                  </div>
                  <div className="text-xl font-black text-white uppercase tracking-tight font-mono">
                    {institution.locationName || 'UNKNOWN_SECTOR'}
                  </div>
                  <div className="mt-2 pt-2 border-t border-white/5">
                    <LocationPathDisplay path={institution.fullLocationPath} isLoading={isInstLoading} compact />
                  </div>
                </div>

                <MetaField
                  label="Ownership"
                  value={institution.isStateOwned ? 'STATE_CONTROLLED' : 'PRIVATE_SECTOR'}
                  icon={<Network size={12} />}
                />
                <MetaField
                  label="Structure"
                  value={institution.hasChildren ? 'HIERARCHICAL_PARENT' : 'END_NODE'}
                />

                {institution.websiteUrl ? (
                  <div className="pt-2">
                    <a
                      href={institution.websiteUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center justify-center gap-2 w-full py-2 bg-brand-accent/5 border border-brand-accent/20 text-brand-accent hover:bg-brand-accent/10 transition-all font-mono text-[10px] uppercase tracking-widest group/link"
                    >
                      <LinkIcon size={12} className="group-hover/link:rotate-12 transition-transform" /> Official_Web_Gateway
                      <ExternalLink size={10} className="ml-1 opacity-50" />
                    </a>
                  </div>
                ) : (
                  <div className="pt-2">
                    <div className="flex items-center justify-center gap-2 w-full py-2 bg-slate-500/5 border border-slate-500/20 text-slate-500 font-mono text-[10px] uppercase tracking-widest cursor-not-allowed">
                      <LinkIcon size={12} /> Web_Gateway_Offline
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-8 pt-8 border-t border-brand-border/50">
                <div className="flex items-center gap-2 mb-3 text-[9px] font-mono text-slate-500 uppercase italic">
                  <div className="w-4 h-[1px] bg-brand-accent/50" /> Briefing_Data
                </div>
                <p className="text-sm text-slate-400 font-serif italic leading-relaxed first-letter:text-2xl first-letter:font-black first-letter:mr-1 first-letter:text-brand-accent">
                  {institution.description || "No classification data available for this node."}
                </p>
              </div>
            </section>
          </div>

          {/* RIGHT COL: Personnel Nexus */}
          <div className="lg:col-span-2 space-y-6">
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm relative">
              <div className="flex items-center justify-between mb-8 border-b border-brand-border/30 pb-4">
                <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.4em] flex items-center gap-2">
                  <Users size={16} className="text-brand-accent" /> Personnel_Deployment_Matrix
                </h3>
                <div className="flex items-center gap-4 font-mono text-[9px]">
                  <span className="text-slate-500">ALLOCATED_SLOTS: <span className="text-brand-accent">{occupations?.length || 0}</span></span>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-3">
                {isOccLoading ? (
                  <LoadingPersonnel />
                ) : occupations && occupations.length > 0 ? (
                  occupations.map((occ) => (
                    <OccupationCard key={occ.publicId} occupation={occ} onClick={() => navigate(`/occupations/${occ.publicId}`)} />
                  ))
                ) : (
                  <div className="py-12 text-center border border-dashed border-brand-border/50 font-mono text-[10px] text-slate-600 uppercase">
                    No active personnel nodes detected
                  </div>
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

// --- HELPER COMPONENTS ---

const LocationPathDisplay = ({ path, isLoading, compact }: { path?: LocationResponse[] | null, isLoading: boolean, compact?: boolean }) => {
  if (isLoading) return <div className="h-3 w-48 bg-white/5 animate-pulse rounded" />;
  if (!path || path.length === 0) return <span className="text-slate-600 italic text-[9px] font-mono uppercase">SECTOR_MAPPING_NULL</span>;

  return (
    <div className={`flex flex-wrap items-center font-mono ${compact ? 'text-[9px] opacity-70' : 'text-[10px]'}`}>
      {path.map((loc, index) => (
        <React.Fragment key={loc.externalId}>
          <Link to={`/locations/${loc.externalId}`} className="text-brand-accent hover:text-white transition-colors uppercase">
            {loc.name}
          </Link>
          {index < path.length - 1 && <span className="text-slate-600 px-1.5">/</span>}
        </React.Fragment>
      ))}
    </div>
  );
};

const MetaField = ({ label, value, icon }: { label: string, value: string, icon?: React.ReactNode }) => (
  <div className="flex items-center justify-between py-2.5 border-b border-brand-border/20 last:border-0 px-1">
    <div className="flex items-center gap-2 text-[10px] font-mono text-slate-500 uppercase tracking-tighter">
      {icon} {label}
    </div>
    <div className="text-xs font-bold text-white uppercase tracking-tight font-mono">{value}</div>
  </div>
);

const OccupationCard = ({ occupation, onClick }: { occupation: OccupationResponse, onClick: () => void }) => (
  <div
    onClick={onClick}
    className="group flex items-center justify-between p-4 bg-black/40 border border-brand-border/50 hover:border-brand-accent/50 hover:bg-brand-accent/5 transition-all cursor-pointer relative"
  >
    <div className="absolute left-0 top-0 bottom-0 w-[2px] bg-brand-accent scale-y-0 group-hover:scale-y-100 transition-transform origin-bottom duration-300" />
    <div className="flex items-center gap-4 relative z-10">
      <div className={`p-2 border ${occupation.isVacant ? 'border-red-500/30 bg-red-500/5' : 'border-brand-accent/30 bg-brand-accent/5'}`}>
        <Briefcase size={18} className={occupation.isVacant ? 'text-red-500' : 'text-brand-accent'} />
      </div>
      <div>
        <div className="text-xs font-black text-white uppercase tracking-tight group-hover:text-brand-accent transition-colors flex items-center gap-2">
          {occupation.title}
          {!occupation.isVacant && <div className="w-1 h-1 bg-brand-success rounded-full animate-pulse" />}
        </div>
        <div className="flex items-center gap-3 mt-1 font-mono text-[9px] text-slate-500 uppercase">
          <span>RANK: <span className="text-slate-300">{occupation.rank}</span></span>
          <span className="text-brand-accent/30">//</span>
          <span>CODE: <span className="text-slate-300">{occupation.code}</span></span>
        </div>
      </div>
    </div>
    <div className="flex items-center gap-6 relative z-10 font-mono text-right">
      <div className={`text-[10px] font-bold uppercase ${occupation.isVacant ? 'text-red-500/70 animate-pulse' : 'text-brand-success/70'}`}>
        {occupation.isVacant ? 'VACANT' : 'OCCUPIED'}
      </div>
      <ExternalLink size={14} className="text-slate-700 group-hover:text-white transition-colors" />
    </div>
  </div>
);

const ErrorState = ({ message }: { message: string }) => (
  <div className="h-full bg-brand-dark flex flex-col items-center justify-center gap-4">
    <ShieldAlert className="text-red-500 animate-bounce" size={48} />
    <div className="font-mono text-red-500 text-xs uppercase tracking-widest">{message}</div>
    <Link to="/institutions" className="text-brand-accent text-[10px] font-mono border border-brand-accent/30 px-4 py-2 hover:bg-brand-accent/10 transition-colors uppercase">
      Return_to_Nexus_Core
    </Link>
  </div>
);

const LoadingPersonnel = () => (
  <div className="py-20 flex flex-col items-center justify-center gap-4">
    <div className="w-12 h-12 border-2 border-brand-accent/20 border-t-brand-accent rounded-full animate-spin" />
    <span className="font-mono text-[10px] text-brand-accent animate-pulse uppercase tracking-widest">Scanning_Personnel_Database...</span>
  </div>
);

const LoadingState = () => (
  <div className="h-full bg-brand-dark flex flex-col items-center justify-center gap-6 font-mono">
    <div className="relative w-24 h-24">
      <div className="absolute inset-0 border-2 border-brand-accent/20 rounded-full" />
      <div className="absolute inset-0 border-t-2 border-brand-accent rounded-full animate-spin" />
      <Fingerprint className="absolute inset-0 m-auto text-brand-accent animate-pulse" size={32} />
    </div>
    <div className="text-xs text-brand-accent uppercase tracking-[1em] animate-pulse">
      Decrypting_Node_Data...
    </div>
  </div>
);

export default InstitutionDetail;