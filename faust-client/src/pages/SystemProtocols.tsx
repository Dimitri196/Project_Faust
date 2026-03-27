import React from 'react';
import { ShieldCheck, Lock, Search, AlertTriangle, EyeOff, Zap } from 'lucide-react';

const SystemProtocols = () => {
  return (
    <div className="min-h-screen bg-[#0a0c10] text-slate-300 p-8 font-mono relative overflow-hidden">
      {/* Background Decorative Grid */}
      <div className="absolute inset-0 opacity-5 pointer-events-none bg-[linear-gradient(to_right,#808080_1px,transparent_1px),linear-gradient(to_bottom,#808080_1px,transparent_1px)] bg-[size:40px_40px]" />

      <div className="max-w-5xl mx-auto relative z-10 space-y-10">
        
        {/* TOP BAR / CLASSIFICATION */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end border-b border-brand-accent/30 pb-6 gap-4">
          <div>
            <div className="inline-block px-2 py-0.5 bg-brand-accent text-brand-dark text-[10px] font-black mb-2 uppercase tracking-widest">
              Top_Secret // Project_Faust
            </div>
            <h1 className="text-4xl font-black text-white uppercase tracking-tighter italic">System_Protocols</h1>
          </div>
          <div className="text-right">
            <p className="text-[10px] text-slate-500 uppercase">Rev_Date: 2026.02.06</p>
            <p className="text-[10px] text-brand-accent uppercase font-bold tracking-widest">Status: Operational_Uplink</p>
          </div>
        </div>

        {/* ACTIVE DIRECTIVES */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <ProtocolCard 
            id="FAUST-01" 
            title="Search_Integrity" 
            icon={<Search size={20}/>}
            content="Omni-Search Terminal utilizes Hybrid TSVector + Trigram Similarity. Minimal entropy threshold: 03 symbols."
            color="border-blue-500/30"
          />
          <ProtocolCard 
            id="FAUST-02" 
            title="Data_Sanitization" 
            icon={<EyeOff size={20}/>}
            content="Automatic PII masking active on all Level 1 outputs. 'Redacted' blocks represent high-sensitivity biography segments."
            color="border-yellow-500/30"
          />
          <ProtocolCard 
            id="FAUST-03" 
            title="Counter_Intel" 
            icon={<ShieldCheck size={20}/>}
            content="Session monitoring is persistent. Any attempt to export Personnel Registry bulk data triggers immediate uplink burn."
            color="border-red-500/30"
          />
        </div>

        {/* DETAILED GUIDELINES SECTION */}
        <div className="bg-black/40 border border-brand-border p-8 space-y-8">
          <h2 className="text-brand-accent text-sm font-black uppercase tracking-[0.3em] flex items-center gap-3">
            <Zap size={16} /> Technical_Specifications
          </h2>
          
          <div className="grid md:grid-cols-2 gap-12 text-[11px] leading-relaxed uppercase">
            <section className="space-y-4">
              <h3 className="text-white border-b border-white/10 pb-2">Operational_Tradecraft</h3>
              <p>1. <span className="text-brand-accent font-bold">The Alias Rule:</span> Subject names are normalized to bypass diacritic masking (e.g., 'Babiš' matches 'Babis').</p>
              <p>2. <span className="text-brand-accent font-bold">Dossier Linking:</span> Personnel are linked to Institutions via 'Engagement Vectors'. Check the 'Hierarchy' page for chain-of-command analysis.</p>
              <p className="p-3 bg-red-500/5 border-l-2 border-red-500 text-red-400 italic">
                Notice: Unauthorized signal duplication is an act of espionage under Protocol 44-S.
              </p>
            </section>

            <section className="space-y-4">
              <h3 className="text-white border-b border-white/10 pb-2">System_Clearance_Table</h3>
              <div className="space-y-2 opacity-70">
                <div className="flex justify-between border-b border-white/5 pb-1">
                  <span>Level 1: Provisional</span>
                  <span className="text-green-500">ACTIVE</span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-1">
                  <span>Level 3: Field Operative</span>
                  <span className="text-slate-600">LOCKED</span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-1">
                  <span>Level 5: Directorate</span>
                  <span className="text-slate-600 font-bold italic">REDACTED</span>
                </div>
              </div>
              <button 
                className="mt-4 px-4 py-2 border border-brand-accent text-brand-accent text-[10px] hover:bg-brand-accent hover:text-brand-dark transition-all"
                onClick={() => window.history.back()}
              >
                Return_to_Safe_Zone
              </button>
            </section>
          </div>
        </div>

        {/* FOOTER REDACTION */}
        <div className="flex justify-center pt-10">
          <div className="h-6 w-full max-w-xs bg-slate-800 relative">
             <span className="absolute inset-0 flex items-center justify-center text-[8px] text-slate-900 font-black">END_OF_DOCUMENT_SIGNATURE</span>
          </div>
        </div>
      </div>
    </div>
  );
};

const ProtocolCard = ({ id, title, content, icon, color }: { id: string, title: string, content: string, icon: any, color: string }) => (
  <div className={`border ${color} bg-black/20 p-6 flex flex-col gap-4 hover:bg-brand-accent/5 transition-all group`}>
    <div className="flex justify-between items-start">
      <div className="p-2 bg-slate-800/50 text-brand-accent group-hover:scale-110 transition-transform">
        {icon}
      </div>
      <span className="text-[9px] text-slate-600 font-bold">{id}</span>
    </div>
    <div>
      <h4 className="text-white font-black text-xs uppercase mb-2 tracking-widest">{title}</h4>
      <p className="text-[10px] text-slate-500 leading-tight uppercase tracking-tighter">
        {content}
      </p>
    </div>
  </div>
);

export default SystemProtocols;