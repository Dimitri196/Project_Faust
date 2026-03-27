import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import type { PersonResponse } from '../types';
import { 
  Search, UserCheck, Mail, GraduationCap, 
  ChevronRight, Fingerprint, ShieldAlert, Zap,
  Activity, Globe, ShieldCheck, MapPin
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const SubjectRegistry = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  const { data: personnel, isLoading } = useQuery<PersonResponse[]>({
    queryKey: ['personnel-priority'],
    queryFn: async () => {
      // Endpoint vrací PersonResponse[]
      const res = await api.get('/persons?limit=60');
      return res.data;
    },
    refetchOnWindowFocus: false
  });

  // STATISTIKA: Výpočet distribuce prověrek L4 a L5 z PersonResponse
  const clearanceStats = useMemo(() => {
    if (!personnel || personnel.length === 0) return { highClearancePct: 0, count: 0 };
    const highLevelItems = personnel.filter(p => 
      p.clearanceLevel === 'LEVEL_4_SECRET' || p.clearanceLevel === 'LEVEL_5_TOP_SECRET'
    );
    return {
      highClearancePct: Math.round((highLevelItems.length / personnel.length) * 100),
      count: highLevelItems.length
    };
  }, [personnel]);

  const filteredPersonnel = useMemo(() => {
    if (!personnel) return [];
    if (!searchTerm.trim()) return personnel;
    const lowerTerm = searchTerm.toLowerCase();
    return personnel.filter(person => 
      person.lastName.toLowerCase().includes(lowerTerm) ||
      person.firstName.toLowerCase().includes(lowerTerm) ||
      person.publicId.toLowerCase().includes(lowerTerm)
    );
  }, [personnel, searchTerm]);

  return (
    <div className="h-full flex flex-col bg-brand-dark animate-in fade-in duration-700">
      
      {/* HEADER WITH STATISTICAL BAR */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative overflow-hidden">
        <div className="absolute inset-0 opacity-[0.03] pointer-events-none bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')]" />
        
        <div className="max-w-7xl mx-auto relative z-10">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-end gap-8">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-brand-accent mb-1 animate-pulse-glow">
                <Activity size={14} />
                <span className="text-[10px] font-mono tracking-[0.4em] uppercase font-black">
                  Operational_Status: LIVE_SYNCHRONIZATION
                </span>
              </div>
              <h1 className="text-5xl font-black text-white italic tracking-tighter uppercase leading-none">
                Priority_Nodes
              </h1>
            </div>

            {/* STATISTICAL BAR */}
            <div className="w-full lg:w-72 bg-black/40 border border-brand-border p-4 rounded-sm flex flex-col gap-2 shadow-2xl">
              <div className="flex justify-between items-center text-[9px] font-mono uppercase tracking-widest text-slate-400">
                <span className="flex items-center gap-2">
                  <ShieldCheck size={12} className="text-purple-500" /> 
                  High_Access_Ratio
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
                {clearanceStats.count}_Nodes_With_L4/L5_Clearance
              </div>
            </div>

            {/* SEARCH INPUT */}
            <div className="relative w-full max-w-sm group">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-brand-accent transition-colors" size={16} />
              <input 
                type="text" 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="IDENT_FILTER..." 
                className="w-full bg-black/40 border border-brand-border py-3 pl-12 pr-4 font-mono text-[11px] text-brand-accent focus:border-brand-accent/50 outline-none uppercase tracking-[0.2em]"
              />
            </div>
          </div>
        </div>
      </div>

      {/* GRID AREA */}
      <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
        <div className="max-w-7xl mx-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-32 gap-6 font-mono text-brand-accent">
               <div className="w-16 h-16 border-2 border-brand-accent/10 border-t-brand-accent rounded-full animate-spin" />
               <span className="text-[10px] tracking-[0.5em] animate-pulse">EXTRACTING_RECORDS...</span>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredPersonnel.map(person => (
                <PersonCard key={person.publicId} person={person} onClick={() => navigate(`/personnel/${person.publicId}`)} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// --- SUB-COMPONENT: PersonCard (Strictly Using PersonResponse) ---
const PersonCard = React.memo(({ person, onClick }: { person: PersonResponse, onClick: () => void }) => {
  
  // LOGIKA: Výpočet Influence Score založený na tvé PersonResponse struktuře
  const calculateInfluence = useMemo(() => {
    let score = 30; // Základ pro jakýkoliv subjekt v databázi

    // Body za vzdělání (EducationLevel)
    const eduScores: Record<string, number> = { 
      'DOCTORATE': 25, 
      'MASTER': 15, 
      'BACHELOR': 8, 
      'HIGHER_VOCATIONAL': 4 
    };
    score += eduScores[person.educationLevel] || 0;

    // Body za ClearanceLevel (Access Rights)
    const clearanceBonus: Record<string, number> = {
      'LEVEL_5_TOP_SECRET': 40,
      'LEVEL_4_SECRET': 25,
      'LEVEL_3_CONFIDENTIAL': 15,
      'LEVEL_2_INTERNAL': 5,
      'LEVEL_1_PUBLIC': 0
    };
    score += clearanceBonus[person.clearanceLevel] || 0;

    // Korekce věkem (předpoklad: zkušenost zvyšuje vliv)
    if (person.age > 40) score += 5;
    if (person.age > 55) score += 5;

    return Math.min(score, 99);
  }, [person]);

  // Vizuální styly pro Clearance Level
  const getClearanceStyle = (level: string) => {
    switch(level) {
      case 'LEVEL_5_TOP_SECRET': return { color: 'text-purple-500', border: 'border-purple-500/30', bg: 'bg-purple-500/5', label: 'L5_TOP_SECRET' };
      case 'LEVEL_4_SECRET': return { color: 'text-red-500', border: 'border-red-500/30', bg: 'bg-red-500/5', label: 'L4_SECRET' };
      case 'LEVEL_3_CONFIDENTIAL': return { color: 'text-orange-500', border: 'border-orange-500/30', bg: 'bg-orange-500/5', label: 'L3_CONFID' };
      default: return { color: 'text-brand-accent', border: 'border-brand-border', bg: 'bg-brand-panel/50', label: level.replace('LEVEL_', 'L') };
    }
  };

  const style = getClearanceStyle(person.clearanceLevel);

  return (
    <div 
      onClick={onClick}
      className={`group relative bg-[#0d0f14] border ${style.border} hover:border-brand-accent/50 transition-all cursor-pointer overflow-hidden shadow-2xl`}
    >
      <Fingerprint className="absolute -right-8 -top-8 text-white opacity-[0.02] group-hover:opacity-[0.05] transition-opacity" size={160} />
      
      <div className="p-6 flex flex-col h-full relative z-10">
        <div className="flex gap-5">
          {/* Portrait Area */}
          <div className="w-20 h-24 bg-black border border-brand-border relative overflow-hidden flex-shrink-0 shadow-inner">
            {person.photoUrl ? (
              <>
                <img src={person.photoUrl} alt={person.lastName} className="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all duration-700" />
                <div className="absolute top-0 left-0 w-full h-[1px] bg-brand-accent/50 animate-scan-line" />
              </>
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-brand-panel/30 text-slate-700">
                <UserCheck size={32} />
              </div>
            )}
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex justify-between items-start">
               <span className="text-[8px] font-mono text-brand-accent/60 tracking-widest uppercase italic">NODE: {person.publicId.slice(0,8)}</span>
               <ShieldAlert size={12} className={style.color} />
            </div>
            <h2 className="text-xl font-black text-white uppercase italic group-hover:text-brand-accent transition-colors leading-tight mt-1 truncate">
              {person.titleBefore ? `${person.titleBefore} ` : ''}{person.lastName}
            </h2>
            <p className="text-xs text-slate-400 font-mono tracking-tighter">{person.firstName} {person.titleAfter || ''}</p>
            
            <div className="mt-4">
              <div className="flex justify-between text-[7px] font-mono text-slate-500 mb-1 uppercase tracking-widest">
                <span>Influence_Factor</span>
                <span className="text-brand-accent">{calculateInfluence}%</span>
              </div>
              <div className="h-1 w-full bg-white/5 rounded-full overflow-hidden">
                 <div 
                  className={`h-full transition-all duration-1000 ${calculateInfluence > 80 ? 'bg-red-500 shadow-[0_0_8px_red]' : 'bg-brand-accent'}`} 
                  style={{ width: `${calculateInfluence}%` }}
                 />
              </div>
            </div>
          </div>
        </div>

        {/* DATA GRID: Mapování polí z PersonResponse */}
        <div className="mt-8 pt-4 border-t border-brand-border/30 grid grid-cols-2 gap-4 font-mono text-[9px]">
          <div className="flex flex-col gap-1">
            <span className="text-slate-600 text-[7px] uppercase tracking-tighter">Current_Location</span>
            <div className="flex items-center gap-2 text-slate-300">
              <MapPin size={10} className="text-brand-accent" />
              <span className="truncate">{person.currentLocationName}</span>
            </div>
          </div>
          <div className="flex flex-col gap-1 items-end text-right">
            <span className="text-slate-600 text-[7px] uppercase tracking-tighter">Education_Field</span>
            <div className="flex items-center gap-2 text-slate-300">
              <span className="truncate">{person.fieldOfStudy}</span>
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-slate-600 text-[7px] uppercase tracking-tighter">Access_Clearance</span>
            <div className={`font-black tracking-widest ${style.color}`}>
              {style.label}
            </div>
          </div>
          <div className="flex flex-col gap-1 items-end">
            <span className="text-slate-600 text-[7px] uppercase tracking-tighter">Affiliation</span>
            <div className="text-emerald-500 text-[8px] font-black truncate max-w-full">
              {person.politicalAffiliation || 'NON_AFFILIATED'}
            </div>
          </div>
        </div>

        <div className="mt-6 flex justify-between items-center">
          <div className={`px-2 py-0.5 border ${style.border} ${style.bg} text-[8px] font-black uppercase tracking-widest ${style.color}`}>
            {person.educationLevel}
          </div>
          <div className="flex items-center gap-2 text-slate-500 group-hover:text-brand-accent transition-colors">
            <span className="text-[7px] font-mono uppercase tracking-tighter opacity-0 group-hover:opacity-100 transition-opacity">Open_Dossier</span>
            <ChevronRight size={14} className="group-hover:translate-x-1 transition-transform" />
          </div>
        </div>
      </div>
    </div>
  );
});

export default SubjectRegistry;