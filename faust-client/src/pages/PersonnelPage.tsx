import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import type { PersonResponse } from '../types';
import { Search, UserCheck, Mail, Phone, GraduationCap, ChevronRight } from 'lucide-react';

const PersonnelPage = () => {
  const { data: personnel, isLoading } = useQuery<PersonResponse[]>({
    queryKey: ['personnel'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/persons'); // Předpokládaný endpoint
      return res.data;
    }
  });

  return (
    <div className="h-full flex flex-col bg-brand-dark animate-in fade-in duration-500">
      {/* Header s vyhledáváním */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-6">
          <div>
            <div className="flex items-center gap-2 text-brand-accent mb-1">
              <UserCheck size={14} />
              <span className="text-[10px] font-mono tracking-[0.3em] uppercase">Security Personnel</span>
            </div>
            <h1 className="text-3xl font-black text-white italic tracking-tighter uppercase">Subject_Archive</h1>
          </div>
          
          <div className="relative w-full max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input 
              type="text" 
              placeholder="FILTER_BY_SURNAME..." 
              className="w-full bg-black/40 border border-brand-border py-3 pl-12 pr-4 font-mono text-xs focus:border-brand-accent outline-none transition-all"
            />
          </div>
        </div>
      </div>

      {/* Grid karet */}
      <div className="flex-1 overflow-y-auto p-8">
        <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {isLoading ? (
            <div className="col-span-full font-mono text-brand-accent animate-pulse uppercase text-xs">&gt; Accessing_Encrypted_Files...</div>
          ) : (
            personnel?.map(person => (
              <div key={person.publicId} className="group relative bg-brand-panel/30 border border-brand-border p-6 hover:border-brand-accent/50 transition-all cursor-pointer">
                {/* ID Tag v rohu */}
                <div className="absolute top-2 right-2 font-mono text-[8px] text-slate-700 tracking-tighter">
                  {person.publicId.split('-')[0]}
                </div>

                <div className="flex items-start gap-4">
                  <div className="w-16 h-16 bg-slate-800 border border-slate-700 flex items-center justify-center grayscale group-hover:grayscale-0 transition-all">
                    <UserCheck size={32} className="text-slate-600 group-hover:text-brand-accent" />
                  </div>
                  <div className="flex-1">
                    <h2 className="text-lg font-bold text-white leading-none mb-1 group-hover:text-brand-accent transition-colors">
                      {person.titleBefore} {person.lastName.toUpperCase()}
                    </h2>
                    <p className="text-xs text-slate-400 font-mono tracking-tighter">{person.firstName} {person.titleAfter}</p>
                  </div>
                </div>

                <div className="mt-6 space-y-2 border-t border-brand-border/50 pt-4 font-mono text-[10px]">
                  <div className="flex items-center gap-2 text-slate-400">
                    <GraduationCap size={12} className="text-brand-accent" />
                    <span>{person.educationLevel}: {person.fieldOfStudy}</span>
                  </div>
                  <div className="flex items-center gap-2 text-slate-500">
                    <Mail size={12} />
                    <span>{person.email}</span>
                  </div>
                </div>

                <div className="mt-4 flex justify-end">
                  <div className="text-[9px] font-mono text-brand-accent flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity uppercase tracking-widest">
                    View Dossier <ChevronRight size={10} />
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default PersonnelPage;