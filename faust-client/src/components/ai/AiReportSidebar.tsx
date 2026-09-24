import React, { useState, useEffect } from 'react';
import { ShieldAlert, History, Terminal, X } from 'lucide-react';
import api from '../../api/axios';

// Intelligence flag patterns matching AiAnalystService.applyIntelligenceFlags()
const CRITICAL_FLAGS = [
  'CRITICAL_CONFLICT_OF_INTEREST_DETECTED',
  'NEPOTISM_FLAG_ALPHA',
  'CORRUPTION_SIGNAL',
  'BRIBERY_FLAG',
];
const ANOMALY_FLAGS = ['ANOMALY_DETECTED'];

type AnalysisType = 'NEUTRAL' | 'ANOMALY' | 'CRITICAL';

interface Props {
  nodeId: string;
  nodeName: string;
  onClose: () => void;
}

const AiReportSidebar = ({ nodeId, nodeName, onClose }: Props) => {
  const [displayText, setDisplayText]   = useState('');
  const [isTyping, setIsTyping]         = useState(false);
  const [isLoading, setIsLoading]       = useState(true);
  const [analysisType, setAnalysisType] = useState<AnalysisType>('NEUTRAL');
  const [error, setError]               = useState<string | null>(null);

  const playBeep = (isAlert = false) => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioCtx.createOscillator();
      const gainNode   = audioCtx.createGain();
      oscillator.type = isAlert ? 'square' : 'sine';
      oscillator.frequency.setValueAtTime(isAlert ? 880 : 440, audioCtx.currentTime);
      gainNode.gain.setValueAtTime(0.02, audioCtx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.00001, audioCtx.currentTime + 0.1);
      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);
      oscillator.start();
      oscillator.stop(audioCtx.currentTime + 0.1);
    } catch { /* AudioContext not available */ }
  };

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    setDisplayText('');
    setIsLoading(true);
    setIsTyping(false);
    setAnalysisType('NEUTRAL');
    setError(null);

    // FIXED: api (JWT interceptor) instead of raw fetch()
    api.get<string>(`/intelligence/analyze/${nodeId}`, { responseType: 'text' })
      .then(res => {
        const text = res.data;

        // FIXED: detect from actual flags, not hardcoded Czech strings
        const hasCritical = CRITICAL_FLAGS.some(f => text.includes(f));
        const hasAnomaly  = ANOMALY_FLAGS.some(f => text.includes(f));
        if (hasCritical)     setAnalysisType('CRITICAL');
        else if (hasAnomaly) setAnalysisType('ANOMALY');

        setIsLoading(false);
        setIsTyping(true);
        playBeep(hasCritical);

        let i = 0;
        interval = setInterval(() => {
          const char = text.charAt(i);
          setDisplayText(prev => prev + char);
          // FIXED: only beep on flag boundary, not every 5th char
          if (char === '[') playBeep(hasCritical);
          i++;
          if (i >= text.length) { clearInterval(interval); setIsTyping(false); }
        }, 12);
      })
      .catch(err => {
        setIsLoading(false);
        setIsTyping(false);
        setError('SYSTEM_FAILURE: CANNOT_REACH_INTELLIGENCE_NODE');
        console.error('AiReportSidebar:', err);
      });

    return () => { if (interval) clearInterval(interval); };
  }, [nodeId]);

  const formatText = (text: string) => {
    const parts = text.split(/(\[.*?\])/g);
    return parts.map((part, index) => {
      if (part.startsWith('[!!!'))
        return <span key={index} className="text-red-500 font-black animate-pulse bg-red-500/10 px-1">{part}</span>;
      if (part.startsWith('[') && part.endsWith(']')) {
        const isCritical = CRITICAL_FLAGS.some(f => part.includes(f));
        return <span key={index} className={isCritical ? 'text-red-400 font-bold bg-red-500/10 px-0.5' : 'text-brand-accent font-bold'}>{part}</span>;
      }
      return part;
    });
  };

  const borderClass = analysisType === 'CRITICAL'
    ? 'border-red-500/50 bg-red-950/20'
    : analysisType === 'ANOMALY'
      ? 'border-amber-500/50 bg-amber-950/20'
      : 'border-brand-accent/30 bg-brand-panel/20';

  const accentClass = analysisType === 'CRITICAL'
    ? 'text-red-500'
    : analysisType === 'ANOMALY'
      ? 'text-amber-400'
      : 'text-brand-accent';

  return (
    <div className="w-96 bg-[#020617] border-l border-brand-accent/20 h-full flex flex-col z-20 shadow-[-20px_0_50px_rgba(0,0,0,0.8)] backdrop-blur-md animate-in slide-in-from-right duration-300">

      {/* HEADER */}
      <div className={`p-4 border-b ${borderClass} flex justify-between items-center`}>
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <Terminal size={10} className={accentClass} />
            <span className="text-[10px] text-slate-500 uppercase font-bold tracking-[0.2em]">Intel_Report</span>
          </div>
          <span className={`text-lg font-black uppercase tracking-tighter ${accentClass}`}>{nodeName}</span>
        </div>
        <button onClick={onClose} className="text-slate-600 hover:text-red-400 transition-colors border border-slate-800 p-1.5 hover:border-red-500/40">
          <X size={12} />
        </button>
      </div>

      {/* THREAT BOX */}
      <div className="px-6 py-4">
        <div className={`border p-3 flex items-center gap-4 ${borderClass} ${analysisType === 'CRITICAL' ? 'animate-pulse' : ''}`}>
          {analysisType === 'CRITICAL'
            ? <ShieldAlert className="text-red-500 shrink-0" />
            : <History className={`${accentClass} shrink-0`} />}
          <div>
            <div className={`text-[8px] font-bold uppercase tracking-widest ${accentClass}`}>Threat_Assessment</div>
            <div className="text-[11px] text-white font-mono uppercase">
              {analysisType === 'CRITICAL' ? 'Priority_Alpha_Conflict'
                : analysisType === 'ANOMALY' ? 'Anomaly_Detected'
                : 'Standard_Background_Check'}
            </div>
          </div>
        </div>
      </div>

      {/* CONTENT */}
      <div className="px-6 flex-1 overflow-y-auto font-mono custom-scrollbar">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full gap-4">
            <div className="relative w-8 h-8">
              <div className="absolute inset-0 border border-brand-accent/10 rounded-full" />
              <div className="absolute inset-0 border-t border-brand-accent rounded-full animate-spin" />
            </div>
            <span className="text-[9px] text-brand-accent/60 uppercase tracking-[0.3em] animate-pulse">
              Querying_AI_Analyst...
            </span>
          </div>
        ) : error ? (
          <div className="text-red-500/70 text-[11px] font-mono py-8 text-center">{error}</div>
        ) : (
          <div className="text-[13px] leading-relaxed text-slate-300 py-2">
            {formatText(displayText)}
            {isTyping && <span className="inline-block w-2 h-4 bg-brand-accent ml-1 animate-pulse" />}
          </div>
        )}
      </div>

      {/* STATUS FOOTER */}
      <div className="p-4 bg-black/40 border-t border-slate-800 flex justify-between text-[8px] font-mono text-slate-600">
        <span>ENCRYPTION: AES-256-GCM</span>
        <span className={`tracking-tighter ${isLoading || isTyping ? 'animate-pulse text-brand-accent/40' : ''}`}>
          {isLoading ? 'AWAITING_AI_RESPONSE...' : isTyping ? 'PROCESSING_STREAM...' : 'IDLE_COMPLETE'}
        </span>
      </div>
    </div>
  );
};

export default AiReportSidebar;