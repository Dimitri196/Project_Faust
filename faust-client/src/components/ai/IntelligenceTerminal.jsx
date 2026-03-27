import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Zap, Target, X, ShieldAlert, Activity, Wifi, Search } from 'lucide-react';
import InfluenceGraph from './InfluenceGraph'; 
import AiReportSidebar from './AiReportSidebar';

const IntelligenceTerminal = ({ subjectId }) => {
  const navigate = useNavigate();
  const [mapData, setMapData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedNode, setSelectedNode] = useState(null);
  const [riskMode, setRiskMode] = useState(false);
  
  const [targetId, setTargetId] = useState("");
  const [highlightPath, setHighlightPath] = useState(new Set());
  const [pathInfo, setPathInfo] = useState(null);

  useEffect(() => {
    fetch(`/api/v1/intelligence/influence-map/${subjectId}`)
      .then(res => res.json())
      .then(data => {
        setMapData(data);
        setLoading(false);
      })
      .catch(err => console.error("Map load failed", err));
  }, [subjectId]);

  const handleTracePath = async () => {
    if (!targetId) return;
    try {
      const res = await fetch(`/api/v1/intelligence/path?source=${subjectId}&target=${targetId}`);
      const data = await res.json();
      if (data.found) {
        setHighlightPath(new Set(data.pathIds));
        setPathInfo(data);
      } else {
        alert("ACCESS DENIED: NO CONNECTION FOUND.");
      }
    } catch (err) {
      console.error("Path trace failed:", err);
    }
  };

  if (loading) return (
    <div className="h-screen w-full bg-[#020617] flex flex-col items-center justify-center text-blue-400 font-mono">
      <Zap className="animate-bounce mb-4" size={32} />
      <div className="animate-pulse tracking-[0.3em] uppercase text-xs">Uplinking to Faust Network...</div>
    </div>
  );

  return (
    <div className="flex h-screen w-full bg-[#020617] overflow-hidden font-mono relative text-slate-300">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:40px_40px] opacity-20 pointer-events-none" />
      
      {/* TOP HUD BAR */}
      <div className="absolute top-6 left-6 right-6 z-20 flex justify-between items-start pointer-events-none">
        <div className="space-y-1 pointer-events-auto">
          <div className="flex gap-2">
            <button onClick={() => navigate(`/personnel/${subjectId}`)} className="flex items-center gap-2 text-[10px] text-blue-400 bg-blue-500/10 border border-blue-500/30 px-3 py-1 rounded-sm backdrop-blur-md hover:text-white transition-all">
              <ChevronLeft size={12} /> DOSSIER
            </button>
            <button onClick={() => navigate(`/terminal`)} className="flex items-center gap-2 text-[10px] text-cyan-400 bg-cyan-500/10 border border-cyan-500/30 px-3 py-1 rounded-sm backdrop-blur-md hover:text-white transition-all">
              <Search size={12} /> NEW_SEARCH
            </button>
          </div>
          <h2 className="text-white text-2xl font-black tracking-tighter uppercase mt-2 drop-shadow-[0_0_10px_rgba(59,130,246,0.5)] flex items-center gap-3">
            <Wifi size={18} className="text-blue-500 animate-pulse" />
            NETWORK_SCAN: <span className="text-blue-500">{mapData?.rootFullName}</span>
          </h2>
          <div className="flex gap-2 mt-2">
            <div className="flex items-center gap-2 bg-black/50 border border-slate-800 px-2 py-1">
              <Activity size={10} className="text-green-500" />
              <span className="text-[9px] text-slate-500 uppercase italic">Active_Uplink</span>
            </div>
            <button 
              onClick={() => setRiskMode(!riskMode)}
              className={`flex items-center gap-2 px-2 py-1 border text-[9px] font-bold transition-all ${riskMode ? 'bg-red-500 border-red-400 text-white animate-pulse' : 'bg-black/50 border-slate-800 text-slate-500 hover:border-red-500 hover:text-red-500'}`}
            >
              <ShieldAlert size={10} /> {riskMode ? 'RISK_SCAN_ACTIVE' : 'RUN_RISK_SCAN'}
            </button>
          </div>
        </div>

        {/* TRACE CONTROL PANEL */}
        <div className="flex gap-2 pointer-events-auto bg-slate-900/90 p-2 border border-slate-700 backdrop-blur-xl shadow-2xl rounded-sm">
          <input 
            type="text" 
            placeholder="TARGET_UUID"
            className="bg-black border border-slate-700 px-3 py-1 text-[10px] text-blue-400 outline-none w-48 focus:border-blue-500 transition-all"
            value={targetId}
            onChange={(e) => setTargetId(e.target.value)}
          />
          <button onClick={handleTracePath} className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-1 text-[10px] font-bold uppercase tracking-widest transition-all flex items-center gap-2">
            <Target size={12} /> Trace
          </button>
          {pathInfo && (
            <button onClick={() => {setHighlightPath(new Set()); setPathInfo(null);}} className="text-red-500 px-2 hover:bg-red-500/10">
              <X size={14} />
            </button>
          )}
        </div>
      </div>

      {/* MAIN GRAPH AREA */}
      <div className="relative flex-1">
        <InfluenceGraph 
          rawData={mapData} 
          highlightPath={highlightPath}
          onNodeClick={(node) => setSelectedNode(node)} 
          riskMode={riskMode} // Tady předáváme stav riskMode do tvého grafu
        />

        {/* PATH INFO OVERLAY */}
        {pathInfo && (
          <div className="absolute bottom-10 left-10 z-10 bg-black/80 border-l-4 border-blue-500 p-4 backdrop-blur-md animate-in slide-in-from-left duration-500 shadow-[0_0_30px_rgba(59,130,246,0.2)] max-w-md">
            <div className="text-[10px] text-blue-400 font-bold tracking-[0.2em] mb-2 uppercase flex items-center gap-2">
              <Zap size={10} fill="currentColor" /> Verified Path: {pathInfo.degrees} Deg
            </div>
            <div className="flex flex-wrap items-center gap-2 text-[11px] font-bold text-white uppercase italic tracking-tighter">
              {pathInfo.pathNames.map((name, i) => (
                <React.Fragment key={i}>
                  <span className={i === 0 || i === pathInfo.pathNames.length - 1 ? "text-blue-400 underline decoration-blue-500/50" : ""}>{name}</span>
                  {i < pathInfo.pathNames.length - 1 && <span className="text-slate-600 font-black">»</span>}
                </React.Fragment>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* SIDEBAR ANALYSIS */}
      {selectedNode && (
        <AiReportSidebar 
          nodeId={selectedNode.id} 
          nodeName={selectedNode.name} 
          onClose={() => setSelectedNode(null)} 
        />
      )}
      
      {/* SCANLINE EFFECT */}
      <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-blue-500/20 shadow-[0_0_15px_rgba(59,130,246,0.5)] animate-scanline pointer-events-none" />
    </div>
  );
};

export default IntelligenceTerminal;
