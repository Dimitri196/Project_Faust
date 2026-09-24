import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { 
  User, Mail, Phone, Loader2, 
  ShieldCheck, Fingerprint, History, Lock, EyeOff 
} from 'lucide-react';
import type { PersonResponse, AppointmentResponse } from '../types';

const PeoplePage = () => {
  const [selectedPersonId, setSelectedPersonId] = useState<string | null>(null);

  const { data: people, isLoading } = useQuery<PersonResponse[]>({
    queryKey: ['people'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/persons');
      return res.data;
    }
  });

  const { data: selectedPerson, isFetching: isDetailLoading } = useQuery<PersonResponse>({
    queryKey: ['person', selectedPersonId],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/persons/${selectedPersonId}`);
      return res.data;
    },
    enabled: !!selectedPersonId
  });

  const { data: history } = useQuery<AppointmentResponse[]>({
    queryKey: ['person-history', selectedPersonId],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/appointments/person/${selectedPersonId}`);
      return res.data;
    },
    enabled: !!selectedPersonId
  });

  if (isLoading) return <LoadingScreen />;

  return (
    <div className="flex h-screen bg-brand-dark text-slate-200 overflow-hidden">
      
      {/* MASTER PANEL */}
      <div className="w-1/3 border-r border-brand-border/50 flex flex-col bg-brand-dark/50 backdrop-blur-md">
        <div className="p-6 border-b border-brand-border/50 bg-brand-panel/30">
          <div className="flex items-center gap-2 mb-1">
            <ShieldCheck size={14} className="text-brand-accent" />
            <h2 className="text-[9px] font-mono uppercase tracking-[0.3em] text-brand-accent/70">Central_Registry</h2>
          </div>
          <h1 className="text-2xl font-black tracking-tighter text-white uppercase italic">Active_Subjects</h1>
        </div>

        <div className="flex-1 overflow-y-auto scrollbar-thin">
          {people?.map(person => (
            <div 
              key={person.publicId}
              onClick={() => setSelectedPersonId(person.publicId)}
              className={`group p-5 cursor-pointer border-b border-brand-border/30 transition-all relative ${
                selectedPersonId === person.publicId ? 'bg-brand-accent/10' : 'hover:bg-brand-panel/40'
              }`}
            >
              {selectedPersonId === person.publicId && (
                <div className="absolute left-0 top-0 bottom-0 w-1 bg-brand-accent shadow-[0_0_15px_#3b82f6]" />
              )}
              <div className={`font-bold text-lg ${selectedPersonId === person.publicId ? 'text-brand-accent' : 'text-slate-100'}`}>
                {person.lastName.toUpperCase()}, {person.firstName}
              </div>
              <div className="text-[9px] text-slate-500 font-mono mt-1 flex items-center gap-2">
                <Fingerprint size={10} /> {person.publicId.slice(0, 8)} // SECURE_ID
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* DETAIL PANEL */}
      <div className="flex-1 p-12 overflow-y-auto relative">
        {selectedPerson ? (
          <div className="max-w-4xl animate-in fade-in slide-in-from-right-8 duration-700">
            <div className="flex items-start gap-10 mb-12">
              <div className="relative group p-1 bg-brand-accent/20">
                <div className="w-40 h-52 bg-brand-panel border border-brand-border overflow-hidden relative">
                  {selectedPerson.photoUrl ? (
                    <img src={selectedPerson.photoUrl} className="w-full h-full object-cover grayscale contrast-110" alt="Visual" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center"><User size={60} className="text-brand-accent/20" /></div>
                  )}
                  <div className="absolute top-0 left-0 w-full h-[1px] bg-brand-accent animate-scan-line opacity-30" />
                </div>
              </div>
              
              <div>
                <h1 className="text-6xl font-black tracking-tighter text-white leading-none italic uppercase mb-2">
                  {selectedPerson.displayName}
                </h1>
                <p className="text-brand-accent font-mono text-[10px] tracking-[0.4em] uppercase">Status: In_Registry</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6 mb-12">
              <DataBox label="Comms_Channel" value={selectedPerson.primaryEmail || 'N/A'} icon={<Mail size={14}/>} />
              <DataBox label="Field_of_Expertise" value={selectedPerson.fieldOfStudy || 'N/A'} icon={<History size={14}/>} />
            </div>

            {/* Reálná Historie Jmenování */}
            <div className="mt-16">
              <div className="flex items-center gap-4 mb-8">
                <History size={18} className="text-brand-accent" />
                <h3 className="text-[10px] font-mono font-black text-white uppercase tracking-[0.4em]">Historical_Service_Record</h3>
              </div>
              <div className="pl-10 border-l border-brand-border/50 space-y-8">
                {history?.map((apt, idx) => (
                  <div key={apt.publicId} className="relative">
                    <div className={`absolute -left-[45px] top-1 w-2.5 h-2.5 rounded-full ${idx === 0 ? 'bg-brand-accent shadow-[0_0_8px_#38bdf8]' : 'bg-brand-border'}`} />
                    <div className="text-[10px] font-mono text-brand-accent/60 mb-1">{apt.startDate} — {apt.endDate || 'ACTUAL'}</div>
                    <div className="text-lg font-bold text-white uppercase">{apt.occupationTitle}</div>
                    <div className="text-[10px] text-slate-600 font-mono tracking-tighter uppercase italic">{apt.appointmentNote}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="h-full flex flex-col items-center justify-center opacity-10">
            <Fingerprint size={120} className="mb-4 text-slate-500" />
            <p className="font-mono text-xs tracking-widest uppercase">Awaiting_Selection</p>
          </div>
        )}
      </div>
    </div>
  );
};

const DataBox = ({ label, value, icon }: { label: string, value: string, icon: React.ReactNode }) => (
  <div className="p-6 bg-brand-panel/30 border border-brand-border/50 backdrop-blur-sm relative group overflow-hidden">
    <div className="text-slate-500 text-[9px] uppercase font-mono tracking-widest mb-3 flex items-center gap-2">{icon} {label}</div>
    <div className="font-mono text-sm text-slate-200">{value}</div>
    <div className="absolute bottom-0 left-0 h-[1px] w-0 bg-brand-accent transition-all group-hover:w-full" />
  </div>
);

const LoadingScreen = () => (
  <div className="h-screen bg-brand-dark flex items-center justify-center font-mono text-brand-accent animate-pulse uppercase tracking-[1em]">
    Syncing_Archive...
  </div>
);

export default PeoplePage;