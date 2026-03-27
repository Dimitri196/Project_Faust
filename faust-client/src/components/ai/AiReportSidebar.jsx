import React, { useState, useEffect, useRef } from 'react';
import { ShieldAlert, History, Terminal } from 'lucide-react';

const AiReportSidebar = ({ nodeId, nodeName, onClose }) => {
  const [displayText, setDisplayText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [analysisType, setAnalysisType] = useState("NEUTRAL");
  const audioRef = useRef(null);

  // Funkce pro krátké pípnutí (synth beep)
  const playBeep = (isAlert = false) => {
    try {
      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();

      oscillator.type = isAlert ? 'square' : 'sine'; // Square zní agresivněji pro alerty
      oscillator.frequency.setValueAtTime(isAlert ? 880 : 440, audioCtx.currentTime); 
      
      gainNode.gain.setValueAtTime(0.02, audioCtx.currentTime); // Velmi potichu
      gainNode.gain.exponentialRampToValueAtTime(0.00001, audioCtx.currentTime + 0.1);

      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);

      oscillator.start();
      oscillator.stop(audioCtx.currentTime + 0.1);
    } catch (e) { /* Audio context failsafe */ }
  };

  useEffect(() => {
    let interval;
    setDisplayText("");
    setIsTyping(true);
    setAnalysisType("NEUTRAL");

    fetch(`/api/v1/intelligence/analyze/${nodeId}`)
      .then(res => res.text())
      .then(text => {
        if (text.includes("NEPOTISM") || text.includes("55 000")) setAnalysisType("CRITICAL");
        else if (text.includes("rozvedeni")) setAnalysisType("HISTORICAL");

        let i = 0;
        interval = setInterval(() => {
          const currentChar = text.charAt(i);
          setDisplayText(prev => prev + currentChar);
          
          // Audio feedback při psaní
          // Pokud narazíme na začátek alertu "[", pípne to varovně
          if (currentChar === '[') playBeep(true);
          else if (i % 5 === 0) playBeep(false); // Jemné cvakání při běžném textu

          i++;
          if (i >= text.length) {
            clearInterval(interval);
            setIsTyping(false);
          }
        }, 12);
      })
      .catch(err => {
        setDisplayText(">> SYSTEM_FAILURE: CANNOT_REACH_DATABASE_NODE");
        setIsTyping(false);
      });

    return () => { if (interval) clearInterval(interval); };
  }, [nodeId]);

  // Funkce pro zvýraznění alertů v textu pomocí barev
  const formatText = (text) => {
    const parts = text.split(/(\[.*?\])/g);
    return parts.map((part, index) => {
      if (part.startsWith('[!!!')) {
        return <span key={index} className="text-red-500 font-black animate-pulse bg-red-500/10 px-1">{part}</span>;
      }
      if (part.startsWith('[') && part.endsWith(']')) {
        return <span key={index} className="text-blue-400 font-bold">{part}</span>;
      }
      return part;
    });
  };

  return (
    <div className="w-96 bg-[#020617] border-l border-blue-500/20 h-full flex flex-col z-20 shadow-[-20px_0_50px_rgba(0,0,0,0.8)] backdrop-blur-md">
      {/* HEADER */}
      <div className={`p-4 border-b ${analysisType === 'CRITICAL' ? 'border-red-500/50 bg-red-950/20' : 'border-blue-500/30 bg-blue-950/20'} flex justify-between items-center`}>
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <Terminal size={10} className="text-blue-500" />
            <span className="text-[10px] text-slate-500 uppercase font-bold tracking-[0.2em]">Intel_Report</span>
          </div>
          <span className={`text-lg font-black uppercase tracking-tighter ${analysisType === 'CRITICAL' ? 'text-red-500' : 'text-blue-400'}`}>
            {nodeName}
          </span>
        </div>
        <button onClick={onClose} className="text-slate-600 hover:text-red-400 transition-colors border border-slate-800 p-1">
          <span className="text-[9px] font-mono">DISC_0x2</span>
        </button>
      </div>

      {/* THREAT BOX */}
      <div className="px-6 py-4">
        <div className={`border p-3 flex items-center gap-4 ${analysisType === 'CRITICAL' ? 'bg-red-500/10 border-red-500/40 animate-pulse' : 'bg-blue-500/5 border-blue-500/20'}`}>
          {analysisType === 'CRITICAL' ? <ShieldAlert className="text-red-500" /> : <History className="text-blue-400" />}
          <div>
            <div className={`text-[8px] font-bold uppercase tracking-widest ${analysisType === 'CRITICAL' ? 'text-red-400' : 'text-blue-400'}`}>
              Threat_Assessment
            </div>
            <div className="text-[11px] text-white font-mono uppercase">
              {analysisType === 'CRITICAL' ? "Priority_Alpha_Conflict" : "Standard_Background_Check"}
            </div>
          </div>
        </div>
      </div>

      {/* TEXT AREA */}
      <div className="px-6 flex-1 overflow-y-auto font-mono custom-scrollbar">
        <div className="text-[13px] leading-relaxed text-slate-300">
          {formatText(displayText)}
          {isTyping && <span className="inline-block w-2 h-4 bg-blue-500 ml-1 animate-pulse" />}
        </div>
      </div>

      {/* STATUS FOOTER */}
      <div className="p-4 bg-black/40 border-t border-slate-800 flex justify-between text-[8px] font-mono text-slate-600">
        <span>ENCRYPTION: AES-256-GCM</span>
        <span className="text-blue-900 tracking-tighter cursor-wait animate-pulse">
          {isTyping ? "PROCESSING_STREAM..." : "IDLE_COMPLETE"}
        </span>
      </div>
    </div>
  );
};

export default AiReportSidebar;