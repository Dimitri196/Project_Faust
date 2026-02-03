import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { PeopleAPI } from '../api/PeopleAPI';
import { useAuth } from '../context/AuthContext';
import type { PersonResponse } from '../types';
import { 
  User, Mail, Phone, GraduationCap, Loader2, 
  ShieldCheck, Fingerprint, History, Lock, EyeOff 
} from 'lucide-react';

const PeoplePage = () => {
    const [selectedPersonId, setSelectedPersonId] = useState<string | null>(null);
    const { user: currentUser } = useAuth();

    // Načtení seznamu všech subjektů
    const { data: people, isLoading } = useQuery<PersonResponse[]>({
        queryKey: ['people'],
        queryFn: PeopleAPI.getAll
    });

    // Načtení detailu vybraného subjektu
    const { data: selectedPerson, isFetching: isDetailLoading } = useQuery<PersonResponse>({
        queryKey: ['person', selectedPersonId],
        queryFn: () => PeopleAPI.getById(selectedPersonId!),
        enabled: !!selectedPersonId
    });

    // Pomocná funkce pro kontrolu Clearance Levelu
    const hasAccess = (requiredLevel: string) => {
        if (!currentUser) return false;
        // Jednoduchá logika: LEVEL_5 vidí vše, LEVEL_1 skoro nic
        const levels = ['LEVEL_1_PUBLIC', 'LEVEL_2_INTERNAL', 'LEVEL_3_CONFIDENTIAL', 'LEVEL_4_SECRET', 'LEVEL_5_TOP_SECRET'];
        return levels.indexOf(currentUser.clearance) >= levels.indexOf(requiredLevel);
    };

    if (isLoading) {
        return (
            <div className="flex h-screen items-center justify-center bg-brand-dark text-brand-accent">
                <Loader2 className="animate-spin mr-3" size={32} />
                <span className="font-mono tracking-widest uppercase">Navazuji spojení s archivem...</span>
            </div>
        );
    }

    return (
        <div className="flex h-screen bg-brand-dark text-slate-200 overflow-hidden">
            
            {/* MASTER: Seznam subjektů (Levý panel) */}
            <div className="w-1/3 border-r border-brand-border/50 flex flex-col bg-brand-dark/50 backdrop-blur-md">
                <div className="p-6 border-b border-brand-border/50 bg-brand-panel/30">
                    <div className="flex items-center gap-2 mb-1">
                        <ShieldCheck size={18} className="text-brand-accent" />
                        <h2 className="text-[10px] font-mono uppercase tracking-[0.2em] text-brand-accent/70">Central_Registry</h2>
                    </div>
                    <h1 className="text-2xl font-black tracking-tighter text-white uppercase italic">Active_Subjects</h1>
                </div>

                <div className="flex-1 overflow-y-auto scrollbar-thin">
                    {people?.map(person => (
                        <div 
                            key={person.publicId}
                            onClick={() => setSelectedPersonId(person.publicId)}
                            className={`group p-5 cursor-pointer border-b border-brand-border/30 transition-all relative ${
                                selectedPersonId === person.publicId 
                                ? 'bg-brand-accent/10' 
                                : 'hover:bg-brand-panel/40'
                            }`}
                        >
                            {selectedPersonId === person.publicId && (
                                <div className="absolute left-0 top-0 bottom-0 w-1 bg-brand-accent shadow-[0_0_15px_rgba(59,130,246,0.5)]" />
                            )}
                            <div className={`font-bold text-lg transition-colors ${selectedPersonId === person.publicId ? 'text-brand-accent' : 'text-slate-100 group-hover:text-white'}`}>
                                {person.lastName.toUpperCase()}, {person.firstName}
                            </div>
                            <div className="text-[9px] text-slate-500 font-mono mt-1 tracking-wider uppercase flex items-center gap-2">
                                <Fingerprint size={10} /> {person.publicId.split('-')[0]} // STATUS: VERIFIED
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* DETAIL: Dossier (Pravý panel) */}
            <div className="flex-1 p-12 overflow-y-auto bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-brand-accent/5 via-transparent to-transparent relative">
                
                {isDetailLoading && (
                    <div className="absolute top-6 right-6 flex items-center gap-2 text-brand-accent font-mono text-[10px] animate-pulse bg-brand-dark/80 px-3 py-1 border border-brand-accent/20">
                        <Loader2 className="animate-spin" size={12} /> DECRYPTING_DOSSIER...
                    </div>
                )}

                {selectedPerson ? (
                    <div className="max-w-4xl animate-in fade-in slide-in-from-right-8 duration-700">
                        
                        {/* Header Dossieru */}
                        <div className="flex items-start gap-10 mb-12">
                            <div className="relative group">
                                <div className="p-1 bg-brand-accent/20 rounded-sm">
                                    <div className="p-6 bg-brand-panel border border-brand-border shadow-2xl relative z-10">
                                        <User size={80} className="text-brand-accent/80" />
                                    </div>
                                </div>
                                <div className="absolute -inset-4 bg-brand-accent/10 blur-2xl rounded-full group-hover:bg-brand-accent/20 transition-all" />
                            </div>
                            
                            <div className="pt-2">
                                <div className="flex items-center gap-3 mb-3">
                                    <span className="px-2 py-0.5 border border-brand-accent text-[10px] font-mono text-brand-accent uppercase tracking-widest bg-brand-accent/5">
                                        Confirmed_Identity
                                    </span>
                                    <span className="text-slate-600 font-mono text-[10px]">AUTH_REF: {selectedPerson.publicId.slice(-8)}</span>
                                </div>
                                <h1 className="text-6xl font-black tracking-tighter text-white mb-2 leading-none italic">
                                    {selectedPerson.displayName}
                                </h1>
                                <p className="text-slate-500 font-mono text-xs tracking-[0.2em] uppercase">
                                    Classification: <span className="text-brand-accent">Internal_Asset</span>
                                </p>
                            </div>
                        </div>

                        {/* GRID: Kontaktní a technické údaje s maskováním */}
                        <div className="grid grid-cols-2 gap-6 mb-12">
                            {/* Email Section */}
                            <div className="p-6 bg-brand-panel/30 border border-brand-border/50 backdrop-blur-sm relative overflow-hidden group">
                                <div className="text-slate-500 text-[9px] uppercase font-mono tracking-widest mb-3 flex items-center gap-2">
                                    <Mail size={14} className="text-brand-accent/60" /> Secure_Comms
                                </div>
                                <div className="font-mono text-sm text-slate-200">
                                    {hasAccess('LEVEL_3_CONFIDENTIAL') ? (
                                        selectedPerson.email
                                    ) : (
                                        <span className="flex items-center gap-2 text-red-500/50 italic">
                                            <Lock size={12} /> [REDACTED_BY_AUTHORITY]
                                        </span>
                                    )}
                                </div>
                                <div className="absolute bottom-0 left-0 h-[1px] w-0 bg-brand-accent transition-all group-hover:w-full" />
                            </div>

                            {/* Phone Section */}
                            <div className="p-6 bg-brand-panel/30 border border-brand-border/50 backdrop-blur-sm relative overflow-hidden group">
                                <div className="text-slate-500 text-[9px] uppercase font-mono tracking-widest mb-3 flex items-center gap-2">
                                    <Phone size={14} className="text-brand-accent/60" /> Uplink_Channel
                                </div>
                                <div className="font-mono text-sm text-slate-200">
                                    {hasAccess('LEVEL_4_SECRET') ? (
                                        selectedPerson.phone
                                    ) : (
                                        <span className="flex items-center gap-2 text-red-500/50 italic">
                                            <EyeOff size={12} /> [ACCESS_DENIED]
                                        </span>
                                    )}
                                </div>
                                <div className="absolute bottom-0 left-0 h-[1px] w-0 bg-brand-accent transition-all group-hover:w-full" />
                            </div>

                            {/* Expertise Section */}
                            <div className="p-6 bg-brand-panel/30 border border-brand-border/50 backdrop-blur-sm col-span-2">
                                <div className="text-slate-500 text-[9px] uppercase font-mono tracking-widest mb-3 flex items-center gap-2">
                                    <GraduationCap size={14} className="text-brand-accent/60" /> Qualifications_&_Expertise
                                </div>
                                <div className="text-xl font-bold text-slate-200 tracking-tight italic">{selectedPerson.educationLevel}</div>
                                <div className="text-brand-accent/70 font-mono text-[10px] mt-1 uppercase tracking-widest">
                                    Field: {selectedPerson.fieldOfStudy}
                                </div>
                            </div>
                        </div>

                        {/* SERVICE_RECORD: Časová osa jmenování */}
                        <div className="relative mt-16">
                            <div className="flex items-center gap-4 mb-8">
                                <History size={20} className="text-brand-accent" />
                                <h3 className="text-sm font-mono font-black text-white uppercase tracking-[0.4em]">Service_Record</h3>
                                <div className="h-px flex-1 bg-gradient-to-r from-brand-accent/30 to-transparent" />
                            </div>
                            
                            <div className="pl-10 border-l border-brand-border/50 space-y-8 relative">
                                {/* Tato sekce by se v budoucnu mapovala z selectedPerson.appointments */}
                                <div className="relative">
                                    <div className="absolute -left-[45px] top-1 w-3 h-3 bg-brand-accent rounded-full shadow-[0_0_10px_rgba(59,130,246,0.8)]" />
                                    <div className="text-[10px] font-mono text-brand-accent mb-1 uppercase tracking-widest">Active_Appointment</div>
                                    <div className="text-lg font-bold text-white uppercase">Sektorový Ředitel</div>
                                    <div className="text-xs text-slate-500 font-mono">Ministerstvo vnitra // Divize FAUST</div>
                                </div>

                                <div className="relative opacity-50">
                                    <div className="absolute -left-[45px] top-1 w-3 h-3 bg-brand-border rounded-full" />
                                    <div className="text-[10px] font-mono text-slate-600 mb-1 uppercase tracking-widest">2021 — 2024</div>
                                    <div className="text-lg font-bold text-slate-400 uppercase">Vedoucí analytického oddělení</div>
                                    <div className="text-xs text-slate-600 font-mono">Úřad pro kybernetickou bezpečnost</div>
                                </div>
                            </div>
                        </div>

                        {/* BIOGRAPHY: Psychologický profil */}
                        <div className="mt-16 mb-20 p-8 border-l-2 border-brand-accent/20 bg-brand-accent/5 italic">
                            <h3 className="text-[10px] font-mono font-bold text-slate-500 uppercase tracking-[0.3em] mb-4">Subject_Psych_Eval</h3>
                            <div className="prose prose-invert max-w-none font-serif text-lg leading-relaxed text-slate-400">
                                "{selectedPerson.biography || "No intelligence narrative available for this entity."}"
                            </div>
                        </div>

                    </div>
                ) : (
                    <div className="h-full flex flex-col items-center justify-center opacity-10 select-none">
                        <Fingerprint size={160} className="mb-6 text-slate-500" />
                        <p className="font-mono text-sm tracking-[0.5em] uppercase text-white">Select_Subject_for_Scanning</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PeoplePage;