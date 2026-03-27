import React, { useState, useEffect, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate, Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import api from '../api/axios';
import {
  ChevronLeft, Shield, Edit3,
  Mail, Phone, RefreshCw,
  ExternalLink, Car, ShieldCheck, Landmark,
  TrendingUp, Clock, X, MapPin, User, UserRound,
  Briefcase, GraduationCap, Award, BrainCircuit, Zap, Terminal, Network
} from 'lucide-react';

import type { PersonResponse, AppointmentResponse } from '../types';

// --- SUBSIDIARY: INTELLIGENCE OVERLAY (MODAL) ---

const IntelligenceOverlay: React.FC<{ personId: string; isOpen: boolean; onClose: () => void }> = ({ personId, isOpen, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState<any | null>(null);

  const fetchReport = async (forceRescan = false) => {
    setLoading(true);
    try {
      // Pokud je forceRescan true, můžeme přidat query param nebo volat jiný endpoint
      const endpoint = forceRescan
        ? `/intelligence/analyze/${personId}?rescan=true`
        : `/intelligence/analyze/${personId}`;
      const res = await api.get(endpoint);
      setReport(res.data);
    } catch (err) {
      setReport({ analysis: "## CRITICAL ERROR\nSpojení s analytickým modulem FAUST bylo přerušeno. Prověřte integritu databáze." });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && !report) {
      fetchReport();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 md:p-8 animate-in fade-in duration-300">
      <div className="absolute inset-0 bg-[#020617]/95 backdrop-blur-xl" onClick={onClose} />

      <div className="relative w-full max-w-4xl max-h-[90vh] bg-[#0a0f1e] border border-emerald-500/30 rounded-xl shadow-[0_0_50px_rgba(0,0,0,0.8)] flex flex-col overflow-hidden">
        <div className="flex items-center justify-between px-6 py-3 border-b border-white/5 bg-slate-950/50">
          <div className="flex flex-col">
            <div className="flex items-center gap-3">
              <Terminal size={16} className="text-emerald-500" />
              <span className="text-[10px] font-mono font-bold text-emerald-500 uppercase tracking-[0.3em]">
                Classified Intelligence Briefing // Project Faust
              </span>
            </div>
            {report?.generatedAt && (
              <span className="text-[8px] font-mono text-slate-500 uppercase mt-0.5">
                Generated: {new Date(report.generatedAt).toLocaleString('cs-CZ')} | Model: {report.aiModel}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            <button
              onClick={() => fetchReport(true)}
              disabled={loading}
              className="flex items-center gap-2 px-3 py-1 bg-emerald-500/5 hover:bg-emerald-500/20 border border-emerald-500/20 rounded text-[9px] font-mono text-emerald-500 transition-all disabled:opacity-50"
            >
              <RefreshCw size={12} className={loading ? "animate-spin" : ""} /> RESCAN_NODE
            </button>
            <button onClick={onClose} className="p-1 hover:bg-white/10 rounded-full transition-colors text-slate-500 hover:text-white">
              <X size={18} />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6 custom-scrollbar">
          {loading ? (
            <div className="h-64 flex flex-col items-center justify-center space-y-4">
              <RefreshCw className="text-emerald-500 animate-spin" size={32} />
              <span className="text-xs font-mono text-emerald-500 animate-pulse uppercase tracking-widest">Running Cognitive Analysis...</span>
            </div>
          ) : (
            <div className="prose prose-invert prose-emerald max-w-none font-sans text-sm leading-relaxed text-justify">
              <ReactMarkdown
                components={{
                  h2: ({ ...props }) => <h2 className="text-emerald-500 text-lg font-black uppercase mt-6 mb-3 border-b border-emerald-500/20 pb-1 tracking-tighter" {...props} />,
                  h3: ({ ...props }) => <h3 className="text-white text-md font-bold mt-4 mb-2" {...props} />,
                  strong: ({ ...props }) => <strong className="text-emerald-400 font-bold" {...props} />,
                  ul: ({ ...props }) => <ul className="list-disc pl-5 space-y-1 my-3 text-slate-300" {...props} />,
                  p: ({ ...props }) => <p className="mb-4 text-slate-300" {...props} />,
                }}
              >
                {report?.analysis || report || ""}
              </ReactMarkdown>

              <div className="mt-8 pt-4 border-t border-white/5 flex items-center justify-between opacity-30">
                <div className="flex items-center gap-2 text-[9px] uppercase font-bold tracking-[0.2em]">
                  <ShieldCheck size={12} /> Intelligence Verified
                </div>
                <span className="text-[9px] font-mono tracking-tighter">REF: {personId.split('-')[0]}</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// --- MAIN COMPONENT ---

const PersonDossier: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isEditing, setIsEditing] = useState(false);
  const [isIntelOpen, setIsIntelOpen] = useState(false);

  const { data: person, isLoading: isPersonLoading } = useQuery<PersonResponse>({
    queryKey: ['person', id],
    queryFn: async () => {
      const res = await api.get(`/persons/${id}`);
      return res.data;
    }
  });

  const { data: history } = useQuery<AppointmentResponse[]>({
    queryKey: ['person-history', id],
    queryFn: async () => {
      const res = await api.get(`/appointments/person/${id}`);
      return res.data;
    },
    enabled: !!id
  });

  // LOGIKA: Influence Score
  const influenceScore = useMemo(() => {
    if (!person) return 0;
    let score = 35;
    const eduMap: any = { 'DOCTORATE': 25, 'MASTER': 15, 'BACHELOR': 10 };
    const clrMap: any = { 'LEVEL_5_TOP_SECRET': 40, 'LEVEL_4_SECRET': 25, 'LEVEL_3_CONFIDENTIAL': 15 };
    score += (eduMap[person.educationLevel] || 0);
    score += (clrMap[person.clearanceLevel] || 0);
    return Math.min(score, 99);
  }, [person]);

  const formatCZK = (val: number) => new Intl.NumberFormat('cs-CZ', {
    style: 'currency', currency: 'CZK', maximumFractionDigits: 0
  }).format(val);

  const lifetimeEarnings = useMemo(() => {
    if (!history || !person) return 0;
    return history.reduce((total, apt) => {
      const start = new Date(apt.startDate);
      const end = apt.endDate ? new Date(apt.endDate) : (person.deathDate ? new Date(person.deathDate) : new Date());
      const months = Math.max(1, (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth()) + 1);
      return total + (months * ((apt.monthlySalary || 0) + (apt.monthlyLumpSumAllowance || 0)));
    }, 0);
  }, [history, person]);

  if (isPersonLoading) return <LoadingDossier />;

  return (
    <div className="h-full bg-[#020617] bg-grid-pattern text-slate-200 flex flex-col font-sans selection:bg-emerald-500/30 overflow-hidden">

      {/* STATUS BAR */}
      <div className={`px-6 py-2 flex justify-between items-center border-b transition-all duration-500 backdrop-blur-xl sticky top-0 z-50 ${isEditing ? 'bg-amber-500/10 border-amber-500/30' : 'bg-slate-950/80 border-slate-800'
        }`}>
        <div className="flex items-center gap-4 text-[9px] font-mono tracking-[0.2em] uppercase">
          <div className="flex items-center gap-2">
            <Shield size={10} className={isEditing ? 'animate-pulse text-amber-500' : 'text-emerald-500'} />
            <span className={isEditing ? 'text-amber-500 font-bold' : 'text-slate-400'}>
              {isEditing ? 'Override Mode' : 'Intelligence Dossier'}
            </span>
          </div>
          <span className="text-slate-800">|</span>
          <span className="text-slate-500 truncate max-w-[100px]">Node: {person?.publicId.split('-')[0]}</span>
          <div className="flex items-center gap-1 text-brand-accent">
            <Zap size={10} /> <span className="font-bold">Score: {influenceScore}%</span>
          </div>
        </div>

        <div className="flex gap-2">
          <Link to={`/intelligence/${id}`} className="flex items-center gap-2 px-3 py-1 bg-blue-500/10 border border-blue-500/30 rounded text-[9px] font-bold text-blue-400 hover:bg-blue-500 hover:text-white transition-all uppercase tracking-widest shadow-[0_0_15px_rgba(59,130,246,0.1)]">
            <Network size={12} /> Network HUD
          </Link>
          <button onClick={() => setIsIntelOpen(true)} className="flex items-center gap-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/30 rounded text-[9px] font-bold text-emerald-400 hover:bg-emerald-500 hover:text-white transition-all uppercase tracking-widest">
            <BrainCircuit size={12} /> Analysis
          </button>
          <button onClick={() => setIsEditing(!isEditing)} className="flex items-center gap-2 px-3 py-1 border border-slate-700 hover:border-emerald-500 rounded text-[9px] font-bold text-slate-300 transition-all uppercase">
            {isEditing ? <X size={12} /> : <Edit3 size={12} />} {isEditing ? 'Abort' : 'Edit'}
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 md:p-8 custom-scrollbar">
        <div className="max-w-6xl mx-auto space-y-8">

          {/* HEADER SECTION */}
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800 pb-6">
            <div className="space-y-2">
              <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-emerald-400 transition-colors text-[9px] font-mono uppercase tracking-widest">
                <ChevronLeft size={12} /> Directory
              </button>
              <div>
                <div className="text-emerald-500/60 font-mono text-[10px] mb-0 tracking-widest uppercase leading-none">{person?.titleBefore}</div>
                <h1 className="text-4xl font-black text-white tracking-tighter uppercase leading-tight">
                  {person?.firstName} <span className="text-emerald-500">{person?.lastName}</span>
                </h1>
              </div>
            </div>

            <div className="flex gap-3">
              <MetricBox label="Fiscal Footprint" value={formatCZK(lifetimeEarnings)} icon={<TrendingUp size={16} />} />
              <MetricBox label="Career Span" value={`${history?.length || 0} Posts`} icon={<Briefcase size={16} />} />
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

            {/* LEFT SIDE: BIOMETRICS (OPRAVENO PROTI PŘETÉKÁNÍ) */}
            <div className="lg:col-span-4 xl:col-span-3 space-y-6">
              <section className="bg-slate-900/40 border border-slate-800 rounded-lg p-4 shadow-2xl backdrop-blur-md relative overflow-hidden">
                <div className="relative group bg-black rounded border border-slate-700/50 overflow-hidden mb-6">
                  <div className="aspect-[3/4] bg-slate-950 relative flex items-center justify-center">
                    {person?.photoUrl ? (
                      <img src={person.photoUrl} alt="" className="w-full h-full object-cover grayscale brightness-75 group-hover:grayscale-0 group-hover:brightness-100 transition-all duration-500" />
                    ) : (
                      <div className="flex flex-col items-center text-slate-800">
                        {person?.gender === 'FEMALE' ? <UserRound size={100} strokeWidth={0.5} /> : <User size={100} strokeWidth={0.5} />}
                      </div>
                    )}
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="grid grid-cols-1 gap-3">
                    <DataPoint label="Vital Status" value={person?.deathDate ? 'TERMINATED' : 'OPERATIONAL'} color={person?.deathDate ? 'text-rose-600' : 'text-emerald-500'} />

                    {/* AFFILIATION BOX - OPRAVENÁ VIDITELNOST */}
{/* AFFILIATION BOX - STATICKÝ DEŠIFROVANÝ ZÁZNAM */}
<div className="bg-[#020617] p-3 rounded border border-emerald-500/20 relative group">
  {/* Indikátor v rohu jako u vojenských systémů */}
  <div className="absolute top-0 right-0 w-2 h-2 border-t border-r border-emerald-500/40"></div>
  
  <div className="text-[8px] text-emerald-500/40 font-mono font-bold uppercase mb-2 tracking-[0.2em] flex items-center gap-2">
    <div className="w-1 h-1 bg-emerald-500 animate-pulse"></div>
    Affiliation_Data_String
  </div>
  
  <div className="font-mono leading-relaxed">
    {/* Rozdělení textu na segmenty pro lepší čitelnost bez animace */}
    <p className="text-[11px] text-emerald-400 font-bold uppercase tracking-tight">
      {person?.politicalAffiliation?.split(',').map((part, index) => (
        <span key={index} className="block border-l border-emerald-500/30 pl-2 mb-1 last:mb-0">
          {part.trim()}
        </span>
      )) || 'NO_DATA_AVAILABLE'}
    </p>
  </div>

  {/* Spodní metadata */}
  <div className="mt-2 pt-2 border-t border-white/5 flex justify-between items-center">
    <span className="text-[7px] text-slate-600 font-mono">STATUS: VERIFIED</span>
    <span className="text-[7px] text-slate-600 font-mono">ID: {person?.publicId?.split('-')[1] || 'UNK'}</span>
  </div>
</div>
                  </div>

                  <div className="space-y-1 pt-4 border-t border-slate-800/50">
                    <ContactItem icon={<Clock size={10} />} label="Birth Date" value={`${person?.birthDate} (${person?.age}y)`} />
                    <ContactItem icon={<MapPin size={10} />} label="Birthplace" value={person?.placeOfBirth} />
                    <ContactItem icon={<GraduationCap size={10} />} label="Education" value={person?.educationLevel} />
                    <div className="h-4"></div>
                    <ContactItem icon={<Mail size={10} />} label="Mail" value={person?.email} />
                    <ContactItem icon={<Phone size={10} />} label="Secure" value={person?.phone} />
                  </div>
                </div>
              </section>
            </div>

            {/* RIGHT SIDE: CONTENT */}
            <div className="lg:col-span-8 xl:col-span-9 space-y-10">
              <section>
                <div className="flex items-center gap-3 mb-4">
                  <h3 className="text-[9px] font-bold text-slate-500 uppercase tracking-[0.2em] whitespace-nowrap">Intelligence Synopsis</h3>
                  <div className="h-px w-full bg-slate-800/50"></div>
                </div>
                <div className="bg-emerald-500/5 border-l-2 border-emerald-500 p-6 rounded-r-lg backdrop-blur-sm relative group overflow-hidden">
                  <p className="text-lg text-slate-300 leading-relaxed font-light italic relative z-10 first-letter:text-4xl first-letter:font-black first-letter:text-emerald-500 first-letter:mr-2 first-letter:float-left text-justify">
                    {person?.biography || 'NO SUMMARY IN DATABASE'}
                  </p>
                </div>
              </section>

              <section>
                <div className="flex items-center gap-3 mb-6">
                  <h3 className="text-[9px] font-bold text-slate-500 uppercase tracking-[0.2em] whitespace-nowrap">Service History</h3>
                  <div className="h-px w-full bg-slate-800/50"></div>
                </div>

                <div className="relative space-y-4 ml-2">
                  <div className="absolute left-0 top-2 bottom-2 w-px bg-slate-800" />
                  {history?.map((apt) => (
                    <div key={apt.publicId} className="relative pl-8 group">
                      <div className={`absolute -left-[3.5px] top-4 w-2 h-2 rounded-full border border-[#020617] z-10 transition-all ${!apt.endDate ? 'bg-emerald-500 shadow-[0_0_10px_#10b981]' : 'bg-slate-700'
                        }`} />
                      <div className={`p-4 rounded-lg border transition-all duration-300 ${!apt.endDate ? 'bg-emerald-500/5 border-emerald-500/20' : 'bg-slate-900/40 border-slate-800'
                        }`}>
                        <div className="flex flex-col md:flex-row justify-between items-start gap-2 mb-4">
                          <div>
                            <div className="font-mono text-[9px] font-bold text-slate-500 uppercase mb-1">
                              {apt.startDate} // {apt.endDate || 'PRESENT'}
                            </div>
                            <Link to={`/occupations/${apt.occupationPublicId}`} className="text-lg font-bold text-white hover:text-emerald-400 flex items-center gap-2 transition-colors uppercase leading-tight">
                              {apt.occupationTitle} <ExternalLink size={14} className="opacity-0 group-hover:opacity-100 text-emerald-500" />
                            </Link>
                          </div>
                          <div className="bg-black/40 px-3 py-1.5 rounded border border-white/5 text-right">
                            <div className="text-emerald-400 font-mono font-bold text-md tracking-tighter">{formatCZK(apt.monthlySalary + apt.monthlyLumpSumAllowance)}</div>
                            <div className="text-[7px] text-slate-600 font-bold uppercase tracking-widest">Monthly Total</div>
                          </div>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-white/5">
                          <div className="flex gap-2">
                            <BenefitBadge active={apt.benefitDetails?.officialCarWithDriver} icon={<Car size={12} />} label="Car" />
                            <BenefitBadge active={apt.benefitDetails?.securityDetail} icon={<ShieldCheck size={12} />} label="Guard" />
                            <BenefitBadge active={apt.benefitDetails?.diplomaticPassport} icon={<Landmark size={12} />} label="Diplom" />
                          </div>
                          <div className="text-[10px] text-slate-500 font-mono italic flex items-center gap-1">
                            <Award size={10} className="text-emerald-500" />
                            <span className="truncate max-w-[200px]">{apt.appointmentNote || 'Record Verified'}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>

      <IntelligenceOverlay
        personId={person?.publicId || ""}
        isOpen={isIntelOpen}
        onClose={() => setIsIntelOpen(false)}
      />
    </div>
  );
};

// --- COMPACT HELPERS ---

const MetricBox = ({ label, value, icon }: any) => (
  <div className="bg-slate-900/60 border border-slate-800 px-4 py-2 rounded-lg flex items-center gap-3 backdrop-blur-md">
    <div className="p-2 bg-emerald-500/10 rounded text-emerald-500">{icon}</div>
    <div>
      <div className="text-[8px] text-slate-500 font-bold uppercase tracking-widest">{label}</div>
      <div className="text-lg font-mono font-bold text-white tracking-tighter">{value}</div>
    </div>
  </div>
);

const DataPoint = ({ label, value, color = "text-slate-200" }: any) => (
  <div className="bg-black/40 p-2 rounded border border-slate-800/50">
    <div className="text-[7px] text-slate-600 font-bold uppercase mb-0.5 tracking-widest">{label}</div>
    <div className={`text-[9px] font-mono font-bold truncate ${color}`}>{value}</div>
  </div>
);

// NOVÝ KONTAKTNÍ ITEM (Vertikální pro zamezení přetékání)
const ContactItem = ({ icon, label, value }: any) => (
  <div className="flex flex-col py-2 border-b border-white/5 last:border-0 group">
    <span className="flex items-center gap-2 text-[7px] text-slate-600 uppercase font-bold group-hover:text-emerald-500 transition-colors mb-0.5">
      {icon} {label}
    </span>
    <span className="text-[10px] text-slate-300 font-mono break-all leading-tight">
      {value || '---'}
    </span>
  </div>
);

const BenefitBadge = ({ active, icon, label }: any) => (
  <div className={`flex items-center gap-1.5 px-2 py-0.5 rounded border transition-all ${active ? 'bg-emerald-500/10 border-emerald-500/40 text-emerald-400' : 'bg-slate-900/50 border-slate-800 text-slate-600 grayscale'
    }`}>
    {icon} <span className="text-[8px] font-bold uppercase tracking-tight">{label}</span>
  </div>
);

const LoadingDossier = () => (
  <div className="h-full bg-[#020617] flex flex-col items-center justify-center">
    <RefreshCw size={32} className="animate-spin text-emerald-500/20 mb-3" />
    <div className="text-[9px] font-mono text-emerald-500 uppercase tracking-widest animate-pulse">Loading Archive...</div>
  </div>
);

export default PersonDossier;
