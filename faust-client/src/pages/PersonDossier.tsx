import React, { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate, Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import api from '../api/axios';
import {
  ChevronLeft, Shield, Edit3, Mail, Phone, RefreshCw,
  Car, ShieldCheck, Landmark, TrendingUp, Clock, X, MapPin,
  User, UserRound, Briefcase, GraduationCap, Award,
  BrainCircuit, Zap, Terminal, Network, Fingerprint,
  MessageSquare, Radio, ShieldAlert, Key, HelpCircle
} from 'lucide-react';

import type { PersonResponse, AppointmentResponse, ContactType } from '../types';

// --- SUBSIDIARY: INTELLIGENCE OVERLAY (MODAL) ---
const IntelligenceOverlay: React.FC<{ personId: string; isOpen: boolean; onClose: () => void }> = ({ personId, isOpen, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState<any | null>(null);

  const fetchReport = async (forceRescan = false) => {
    setLoading(true);
    try {
      const endpoint = forceRescan ? `/intelligence/analyze/${personId}?rescan=true` : `/intelligence/analyze/${personId}`;
      const res = await api.get(endpoint);
      setReport(res.data);
    } catch (err) {
      setReport({ analysis: "## CRITICAL ERROR\nSpojení přerušeno." });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && !report) fetchReport();
  }, [isOpen, report, personId]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="absolute inset-0 bg-[#020617]/95 backdrop-blur-md" onClick={onClose} />
      <div className="relative w-full max-w-3xl max-h-[85vh] bg-[#0a0f1e] border border-emerald-500/30 rounded-lg flex flex-col overflow-hidden shadow-2xl">
        <div className="flex items-center justify-between px-4 py-2 border-b border-white/5 bg-slate-950/50">
          <span className="text-[9px] font-mono font-bold text-emerald-500 uppercase tracking-widest">Intelligence_Briefing // Faust</span>
          <button onClick={onClose} className="p-1 hover:text-white text-slate-500 transition-colors"><X size={16} /></button>
        </div>
        <div className="flex-1 overflow-y-auto p-5 custom-scrollbar text-sm">
          {loading ? (
            <div className="h-40 flex flex-col items-center justify-center gap-2">
              <RefreshCw className="text-emerald-500 animate-spin" size={24} />
              <span className="text-[9px] font-mono text-emerald-500 animate-pulse uppercase tracking-widest">Cognitive_Analysis...</span>
            </div>
          ) : (
            <div className="prose prose-invert prose-emerald max-w-none text-slate-300">
              <ReactMarkdown>{report?.analysis || report || ""}</ReactMarkdown>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// --- HELPER FOR CONTACT ICONS ---
const getContactIcon = (type: ContactType) => {
  switch (type) {
    case 'EMAIL': return <Mail size={10} />;
    case 'CELLULAR_GSM':
    case 'VOIP': return <Phone size={10} />;
    case 'SIGNAL':
    case 'THREEMA':
    case 'COMMERCIAL_IM': return <MessageSquare size={10} />;
    case 'SATELLITE_TERMINAL':
    case 'TACTICAL_RF': return <Radio size={10} />;
    case 'CRYPTO_WALLET': return <Key size={10} />;
    default: return <HelpCircle size={10} />;
  }
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
    <div className="h-full bg-[#020617] text-slate-200 flex flex-col font-sans selection:bg-emerald-500/30 overflow-hidden">

      {/* STATUS BAR - COMPACT */}
      <div className={`px-4 py-1.5 flex justify-between items-center border-b transition-all duration-500 backdrop-blur-xl sticky top-0 z-50 ${isEditing ? 'bg-amber-500/10 border-amber-500/30' : 'bg-slate-950/80 border-slate-800'}`}>
        <div className="flex items-center gap-3 text-[9px] font-mono tracking-[0.15em] uppercase">
          <div className="flex items-center gap-1.5">
            <Shield size={10} className={isEditing ? 'animate-pulse text-amber-500' : 'text-emerald-500'} />
            <span className={isEditing ? 'text-amber-500 font-bold' : 'text-slate-400'}>{isEditing ? 'Override_Mode' : 'Intelligence_Dossier'}</span>
          </div>
          <span className="text-slate-800">|</span>
          <span className="text-slate-500">Node: {person?.publicId.split('-')[0]}</span>
          <div className="flex items-center gap-1 text-emerald-400">
            <Zap size={10} /> <span className="font-bold">Score: {influenceScore}%</span>
          </div>
        </div>

        <div className="flex gap-2">
          <Link to={`/intelligence/${id}`} className="flex items-center gap-1.5 px-2 py-0.5 bg-blue-500/10 border border-blue-500/20 rounded text-[9px] font-bold text-blue-400 hover:bg-blue-500 hover:text-white transition-all uppercase tracking-tight">
            <Network size={10} /> Network_HUD
          </Link>
          <button onClick={() => setIsIntelOpen(true)} className="flex items-center gap-1.5 px-2 py-0.5 bg-emerald-500/10 border border-emerald-500/20 rounded text-[9px] font-bold text-emerald-400 hover:bg-emerald-500 hover:text-white transition-all uppercase tracking-tight">
            <BrainCircuit size={10} /> Analysis
          </button>
          <button onClick={() => setIsEditing(!isEditing)} className="flex items-center gap-1.5 px-2 py-0.5 border border-slate-700 hover:border-emerald-500 rounded text-[9px] font-bold text-slate-300 transition-all uppercase">
            {isEditing ? <X size={10} /> : <Edit3 size={10} />} {isEditing ? 'Abort' : 'Edit'}
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 md:p-6 custom-scrollbar">
        <div className="max-w-6xl mx-auto space-y-6">

          {/* HEADER SECTION - COMPRESSED */}
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800/50 pb-4">
            <div className="flex-1">
              <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-emerald-400 transition-colors text-[8px] font-mono uppercase tracking-[0.2em] mb-2">
                <ChevronLeft size={10} /> Directory
              </button>

              <div className="flex flex-wrap items-center gap-4">
                <div>
                  <div className="text-emerald-500/50 font-mono text-[9px] tracking-widest uppercase leading-none mb-0.5">{person?.titleBefore}</div>
                  <h1 className="text-3xl font-black text-white tracking-tighter uppercase leading-none">
                    {person?.firstName} <span className="text-emerald-500">{person?.lastName}</span>
                  </h1>
                </div>

                <div className="flex flex-wrap gap-1.5 md:border-l border-slate-800 md:pl-4">
                  {person?.nameHistory?.filter(n => !n.isPrimary).map((name, idx) => (
                    <div key={idx} className="group relative bg-slate-900/40 border border-slate-800 px-2 py-0.5 rounded flex items-center gap-2">
                      <span className="text-[9px] font-bold text-slate-400 uppercase tracking-tight">
                        {name.firstName} {name.lastName}
                      </span>
                      <span className={`text-[6px] px-1 rounded font-mono font-black border ${name.type === 'ALIAS' ? 'border-amber-500/30 text-amber-500' :
                          name.type === 'MAIDEN' ? 'border-purple-500/30 text-purple-500' :
                            'border-blue-500/30 text-blue-500'
                        }`}>
                        {name.type}
                      </span>

                      {name.note && (
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block w-max max-w-[200px] bg-slate-950 border border-slate-800 p-2 rounded shadow-2xl z-50">
                          <p className="text-[8px] text-slate-400 leading-tight">{name.note}</p>
                        </div>
                      )}
                    </div>
                  ))}

                  {(!person?.nameHistory || person.nameHistory.filter(n => !n.isPrimary).length === 0) && (
                    <span className="text-[8px] font-mono text-slate-700 italic uppercase">
                      No_Aliases_Recorded
                    </span>
                  )}
                </div>
              </div>
            </div>

            <div className="flex gap-2">
              <MetricBox label="Fiscal Footprint" value={formatCZK(lifetimeEarnings)} icon={<TrendingUp size={12} />} />
              <Link to={`/appointments?personId=${id}`} className="hover:opacity-80 transition-opacity">
                <MetricBox label="Career Span" value={`${history?.length || 0} Posts`} icon={<Briefcase size={12} />} />
              </Link>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

            {/* LEFT SIDE: BIOMETRICS & TELEMETRY FOOTPRINT */}
            <div className="lg:col-span-3 space-y-4">
              <section className="bg-slate-900/30 border border-slate-800/60 rounded p-2.5 shadow-xl backdrop-blur-md">
                <div className="aspect-[3/4] bg-black rounded border border-slate-800 overflow-hidden mb-3 grayscale group hover:grayscale-0 transition-all duration-500">
                  <img src={person?.photoUrl || undefined} className="w-full h-full object-cover opacity-80" alt="Subject metadata frame" />
                </div>

                <div className="space-y-2">
                  <DataPoint label="Vital Status" value={person?.deathDate ? 'TERMINATED' : 'OPERATIONAL'} color={person?.deathDate ? 'text-rose-600' : 'text-emerald-500'} />

                  {/* AFFILIATION BOX */}
                  <div className="bg-black/60 p-2 rounded border border-emerald-500/20">
                    <div className="text-[7px] text-emerald-500/40 font-mono font-bold uppercase mb-1 tracking-widest flex items-center gap-1">
                      <div className="w-1 h-1 bg-emerald-500 animate-pulse" /> Affiliation_Node
                    </div>
                    <p className="text-[10px] text-emerald-400 font-bold uppercase pl-2 border-l border-emerald-500/30">{person?.politicalAffiliation || 'NOT_ASSIGNED'}</p>
                    <div className="mt-1.5 pt-1 border-t border-white/5 flex justify-between text-[6px] text-slate-600 font-mono">
                      <span>VERIFIED_LINK</span>
                      <span>ID: {person?.publicId?.split('-')[1] || 'UNK'}</span>
                    </div>
                  </div>

                  {/* BIOGRAPHICAL METADATA */}
                  <div className="space-y-0.5 pt-1 border-t border-slate-800/50">
                    <ContactItem icon={<Clock size={10} />} label="Birth Date" value={`${person?.birthDate} (${person?.age}y)`} />
                    <ContactItem icon={<MapPin size={10} />} label="Birthplace" value={person?.placeOfBirth} />
                    <ContactItem icon={<GraduationCap size={10} />} label="Education" value={person?.educationLevel} />
                    <ContactItem icon={<Mail size={10} />} label="Primary Email" value={person?.primaryEmail} />
                    <ContactItem icon={<Phone size={10} />} label="Primary Phone" value={person?.primaryPhone} />
                  </div>
                </div>
              </section>

              {/* NEW: DETAILED TELEMETRY FOOTPRINT LOG (COMMUNICATION VECTOR MATRIX) */}
              <section className="bg-slate-900/30 border border-slate-800/60 rounded p-2.5 shadow-xl backdrop-blur-md">
                <div className="text-[7px] font-mono font-black text-slate-500 uppercase tracking-[0.2em] mb-2 flex items-center gap-1">
                  <Terminal size={10} className="text-emerald-500" /> Technical_Signal_Traces
                </div>
                
                <div className="space-y-1.5 max-h-[280px] overflow-y-auto custom-scrollbar pr-1">
                  {person?.contactHistory && person.contactHistory.length > 0 ? (
                    person.contactHistory.map((contact) => {
                      const isDeception = contact.verificationStatus === 'DECEPTION_MARKER';
                      const isExpired = !contact.isActive || contact.verificationStatus === 'EXPIRED_DEPRECATING';
                      
                      return (
                        <div 
                          key={contact.publicId} 
                          className={`group/contact relative p-1.5 rounded border text-[10px] transition-all bg-black/40 ${
                            isDeception ? 'border-rose-500/40 hover:border-rose-500' :
                            isExpired ? 'border-slate-800/40 opacity-50' : 'border-slate-800 hover:border-emerald-500/50'
                          }`}
                        >
                          <div className="flex justify-between items-center gap-1 mb-0.5">
                            <span className={`flex items-center gap-1 text-[7px] font-mono font-bold uppercase tracking-tight ${
                              isDeception ? 'text-rose-400' : isExpired ? 'text-slate-500' : 'text-slate-400'
                            }`}>
                              {isDeception ? <ShieldAlert size={8} className="text-rose-500 animate-pulse" /> : getContactIcon(contact.contactType)}
                              {contact.contactType}
                            </span>
                            <span className="text-[6px] font-mono text-slate-600">
                              Conf: {Math.round(contact.confidenceScore * 100)}%
                            </span>
                          </div>
                          
                          <div className={`font-mono text-[9px] break-all select-all selection:bg-emerald-500/50 ${
                            isDeception ? 'text-rose-300 font-bold line-through' : isExpired ? 'text-slate-600' : 'text-white'
                          }`}>
                            {contact.contactValueRaw}
                          </div>

                          {contact.imei && (
                            <div className="text-[7px] font-mono text-slate-500 mt-0.5 flex items-center gap-0.5">
                              <span>HW_IMEI:</span> <span className="text-slate-400">{contact.imei}</span>
                            </div>
                          )}

                          {/* INTERPOLATED ANALYTICAL TOOLTIP ON HOVER */}
                          {contact.analyticalNote && (
                            <div className="absolute left-full top-0 ml-2 hidden group-hover/contact:block w-[180px] bg-slate-950 border border-slate-800 p-1.5 rounded shadow-2xl z-50 text-[8px] text-slate-400 leading-tight font-sans">
                              <span className="block text-[6px] font-bold text-emerald-500 uppercase font-mono mb-0.5">Operator_Notes:</span>
                              {contact.analyticalNote}
                            </div>
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <div className="text-[8px] font-mono text-slate-700 italic uppercase p-2 text-center border border-dashed border-slate-800 rounded">
                      No_Active_Signals_Interacted
                    </div>
                  )}
                </div>
              </section>
            </div>

            {/* RIGHT SIDE: CONTENT */}
            <div className="lg:col-span-9 space-y-6">
              <section>
                <div className="flex items-center gap-2 mb-2 opacity-50">
                  <h3 className="text-[8px] font-bold text-slate-400 uppercase tracking-[0.2em] whitespace-nowrap">Intelligence_Synopsis</h3>
                  <div className="h-px w-full bg-slate-800/50"></div>
                </div>
                <div className="bg-emerald-500/[0.02] border-l border-emerald-500/40 p-4 rounded-r shadow-inner">
                  <p className="text-base text-slate-300 leading-relaxed font-light italic text-justify first-letter:text-3xl first-letter:font-black first-letter:text-emerald-500 first-letter:mr-1 first-letter:float-left">
                    {person?.biography || 'NO SUMMARY IN DATABASE'}
                  </p>
                </div>
              </section>

              <section className="relative">
                <div className="flex items-center justify-between mb-4 bg-slate-900/40 px-3 py-1.5 border-l-2 border-emerald-500">
                  <div className="flex items-center gap-3">
                    <Fingerprint size={14} className="text-emerald-500" />
                    <h3 className="text-[10px] font-black text-white uppercase tracking-[0.3em]">Operational_Record_Log // History</h3>
                  </div>
                  <div className="text-[8px] font-mono text-emerald-500/50 uppercase">Entries: {history?.length.toString().padStart(2, '0')}</div>
                </div>

                <div className="relative space-y-3 ml-2">
                  <div className="absolute left-0 top-0 bottom-0 w-px bg-gradient-to-b from-emerald-500/50 via-slate-800 to-transparent" />

                  {history?.map((apt) => {
                    const isActive = !apt.endDate;
                    return (
                      <div key={apt.publicId} className="relative pl-6 group">
                        <div className={`absolute -left-[3px] top-4 w-1.5 h-1.5 rotate-45 border border-black z-10 ${isActive ? 'bg-cyan-400 shadow-[0_0_8px_#22d3ee]' : 'bg-slate-700'}`} />
                        <div className={`p-3 border transition-all duration-200 ${isActive ? 'bg-cyan-500/[0.03] border-cyan-500/30 border-l-2 border-l-cyan-500' : 'bg-slate-900/10 border-slate-800 border-l-2 border-l-slate-700'}`}>

                          <div className="flex justify-between items-start mb-2">
                            <div className="flex items-center gap-2">
                              <span className={`font-mono text-[9px] font-bold px-1.5 py-0.5 rounded ${isActive ? 'bg-cyan-500/20 text-cyan-400' : 'bg-slate-800 text-slate-500'}`}>
                                {apt.startDate.replace(/-/g, '.')} // {apt.endDate?.replace(/-/g, '.') || 'ACTIVE'}
                              </span>
                              {isActive && <span className="text-[8px] font-black text-cyan-400 animate-pulse uppercase tracking-tighter">Current_Assignment</span>}
                            </div>
                            <span className="font-mono text-[7px] text-slate-600 uppercase">Ref: {apt.publicId.split('-')[0]}</span>
                          </div>

                          <div className="flex flex-col md:flex-row justify-between gap-2 mb-3">
                            <div>
                              <Link to={`/occupations/${apt.occupationPublicId}`} className={`text-lg font-black tracking-tight uppercase hover:text-emerald-500 transition-colors ${isActive ? 'text-white' : 'text-slate-500'}`}>
                                {apt.occupationTitle}
                              </Link>
                              <div className="flex items-center gap-1.5 text-[8px] font-mono text-slate-500 uppercase tracking-widest">
                                <Landmark size={8} /> Authorized_Uplink
                              </div>
                            </div>
                            <div className={`p-2 border-l border-slate-800 min-w-[140px] text-right`}>
                              <div className="text-[7px] font-mono text-slate-600 uppercase mb-0.5">Monthly_Allowance</div>
                              <div className={`text-lg font-black font-mono leading-none ${isActive ? 'text-cyan-400' : 'text-slate-500'}`}>{formatCZK(apt.monthlySalary + apt.monthlyLumpSumAllowance)}</div>
                            </div>
                          </div>

                          <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-white/5 bg-black/20 -mx-3 -mb-3 px-3 py-1.5">
                            <div className="flex gap-1.5">
                              <BenefitBadge active={apt.benefitDetails?.officialCarWithDriver} icon={<Car size={10} />} label="CAR" />
                              <BenefitBadge active={apt.benefitDetails?.securityDetail} icon={<ShieldCheck size={10} />} label="GUARD" />
                              <BenefitBadge active={apt.benefitDetails?.diplomaticPassport} icon={<Landmark size={10} />} label="DIPLOM" />
                            </div>
                            <div className={`flex items-center gap-1.5 text-[9px] font-mono italic max-w-[250px] truncate ${isActive ? 'text-cyan-300/60' : 'text-slate-600'}`}>
                              <Award size={10} /> {apt.appointmentNote || 'CLEARANCE_VERIFIED'}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>

      <IntelligenceOverlay personId={person?.publicId || ""} isOpen={isIntelOpen} onClose={() => setIsIntelOpen(false)} />
    </div>
  );
};

// --- MINIFIED HELPERS ---
const MetricBox = ({ label, value, icon }: any) => (
  <div className="bg-slate-900/40 border border-slate-800 px-3 py-1.5 rounded flex items-center gap-2.5 backdrop-blur-md">
    <div className="text-emerald-500 opacity-60">{icon}</div>
    <div>
      <div className="text-[7px] text-slate-600 font-bold uppercase tracking-widest leading-tight">{label}</div>
      <div className="text-base font-mono font-bold text-white tracking-tighter leading-tight">{value}</div>
    </div>
  </div>
);

const DataPoint = ({ label, value, color = "text-slate-200" }: any) => (
  <div className="bg-black/30 p-1.5 rounded border border-slate-800/40">
    <div className="text-[6px] text-slate-600 font-bold uppercase tracking-[0.1em] mb-0.5">{label}</div>
    <div className={`text-[9px] font-mono font-bold truncate leading-none ${color}`}>{value}</div>
  </div>
);

const ContactItem = ({ icon, label, value }: any) => (
  <div className="flex items-center justify-between py-1 border-b border-white/5 last:border-0">
    <span className="flex items-center gap-1 text-[7px] text-slate-600 uppercase font-bold">
      {icon} {label}
    </span>
    <span className="text-[9px] text-slate-400 font-mono truncate max-w-[120px]">
      {value ? value : '---'}
    </span>
  </div>
);

const BenefitBadge = ({ active, icon, label }: any) => (
  <div className={`flex items-center gap-1 px-1.5 py-0.5 rounded border transition-all ${active ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-slate-900/50 border-slate-800 text-slate-600 opacity-30 grayscale'}`}>
    {icon} <span className="text-[7px] font-bold uppercase tracking-tight">{label}</span>
  </div>
);

const LoadingDossier = () => (
  <div className="h-full bg-[#020617] flex flex-col items-center justify-center">
    <RefreshCw size={24} className="animate-spin text-emerald-500/20 mb-2" />
    <div className="text-[8px] font-mono text-emerald-500 uppercase tracking-widest animate-pulse">Loading_Archive...</div>
  </div>
);

export default PersonDossier;