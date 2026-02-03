import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { 
  Building2, Users, Shield, MapPin, 
  ExternalLink, ChevronLeft, Briefcase, Info
} from 'lucide-react';
import axios from 'axios';
import type { InstitutionResponse, OccupationResponse } from '../types';

const InstitutionDetail = () => {
  const { id } = useParams<{ id: string }>();

  // 1. Základní data instituce
  const { data: institution, isLoading: isInstLoading } = useQuery<InstitutionResponse>({
    queryKey: ['institution', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/institutions/${id}`);
      return res.data;
    }
  });

  // 2. Personální obsazení (Occupations) v této instituci
  const { data: occupations, isLoading: isOccLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['institution-occupations', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/occupations/institution/${id}`);
      return res.data;
    },
    enabled: !!id
  });

  if (isInstLoading) return <LoadingState />;

  return (
    <div className="h-full bg-brand-dark text-slate-200 flex flex-col animate-in fade-in duration-500">
      
      {/* HEADER: Navigace a název */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20">
        <div className="max-w-7xl mx-auto">
          <Link to="/institutions" className="flex items-center gap-2 text-brand-accent/60 hover:text-brand-accent transition-colors mb-6 font-mono text-[10px] uppercase tracking-widest">
            <ChevronLeft size={14} /> Back_to_Nexus
          </Link>
          
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <span className="px-2 py-0.5 bg-brand-accent/10 border border-brand-accent/30 text-[9px] font-mono text-brand-accent uppercase tracking-[0.2em]">
                  {institution?.level} // {institution?.type}
                </span>
              </div>
              <h1 className="text-5xl font-black text-white italic tracking-tighter uppercase leading-none">
                {institution?.name}
              </h1>
            </div>
            
            <div className="flex gap-4 font-mono text-[10px]">
              <div className="text-right">
                <div className="text-slate-500 uppercase">Registry_UID</div>
                <div className="text-brand-accent">{institution?.publicId}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-8">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT COL: Metadata & Description */}
          <div className="lg:col-span-1 space-y-6">
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm">
              <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.3em] mb-6 flex items-center gap-2">
                <Info size={14} className="text-brand-accent" /> Entity_Specs
              </h3>
              
              <div className="space-y-4">
                <MetaField label="Origin" value={institution?.countryCode || 'N/A'} icon={<MapPin size={14}/>} />
                <MetaField label="Ownership" value={institution?.isStateOwned ? 'STATE_ENTITY' : 'PRIVATE_SUBJECT'} />
                <MetaField label="Children_Nodes" value={institution?.hasChildren ? 'DETECTED' : 'NONE'} />
              </div>

              <div className="mt-8 pt-8 border-t border-brand-border/50">
                <p className="text-sm text-slate-400 font-serif italic leading-relaxed">
                  {institution?.description}
                </p>
              </div>
            </section>
          </div>

          {/* RIGHT COL: Occupations & Personnel Nexus */}
          <div className="lg:col-span-2 space-y-6">
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm">
              <div className="flex items-center justify-between mb-8">
                <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.4em] flex items-center gap-2">
                  <Users size={16} className="text-brand-accent" /> Institutional_Personnel_Nodes
                </h3>
                <span className="font-mono text-[9px] text-brand-accent/40 uppercase">Count: {occupations?.length || 0}</span>
              </div>

              <div className="grid grid-cols-1 gap-3">
                {isOccLoading ? (
                  <div className="animate-pulse font-mono text-[10px] text-slate-600 uppercase tracking-widest">Scanning_Network...</div>
                ) : (
                  occupations?.map((occ) => (
                    <OccupationCard key={occ.publicId} occupation={occ} />
                  ))
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

const MetaField = ({ label, value, icon }: { label: string, value: string, icon?: React.ReactNode }) => (
  <div className="flex items-center justify-between py-2 border-b border-brand-border/30 last:border-0">
    <div className="flex items-center gap-2 text-[10px] font-mono text-slate-500 uppercase tracking-tighter">
      {icon} {label}
    </div>
    <div className="text-xs font-bold text-white uppercase tracking-tight font-mono">{value}</div>
  </div>
);

const OccupationCard = ({ occupation }: { occupation: OccupationResponse }) => (
  <div className="group flex items-center justify-between p-4 bg-black/20 border border-brand-border/50 hover:border-brand-accent/50 transition-all cursor-pointer">
    <div className="flex items-center gap-4">
      <div className={`p-2 border ${occupation.isVacant ? 'border-red-500/30 bg-red-500/5' : 'border-brand-accent/30 bg-brand-accent/5'}`}>
        <Briefcase size={18} className={occupation.isVacant ? 'text-red-500' : 'text-brand-accent'} />
      </div>
      <div>
        <div className="text-xs font-black text-white uppercase tracking-tight group-hover:text-brand-accent transition-colors">
          {occupation.title}
        </div>
        <div className="flex items-center gap-3 mt-1 font-mono text-[9px] text-slate-500 uppercase">
          <span>Rank: {occupation.rank}</span>
          <span className="text-brand-accent/30">//</span>
          <span>Code: {occupation.code}</span>
        </div>
      </div>
    </div>
    
    <div className="flex items-center gap-4">
      {occupation.isVacant ? (
        <span className="font-mono text-[9px] text-red-500 bg-red-500/10 px-2 py-0.5 rounded-sm animate-pulse">VACANT</span>
      ) : (
        <span className="font-mono text-[9px] text-brand-success bg-brand-success/10 px-2 py-0.5 rounded-sm">OCCUPIED</span>
      )}
      <ExternalLink size={14} className="text-slate-600 group-hover:text-white transition-colors" />
    </div>
  </div>
);

const LoadingState = () => (
  <div className="h-full bg-brand-dark flex items-center justify-center font-mono text-xs text-brand-accent uppercase tracking-[1em] animate-pulse">
    Establishing_Encrypted_Datalink...
  </div>
);

export default InstitutionDetail;