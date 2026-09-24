import React, { useState, useMemo, useRef, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Users, ChevronDown, ChevronRight, AlertTriangle,
  Calendar, User, Clock, ExternalLink
} from 'lucide-react';
import api from '../../api/axios';

// ── Types ─────────────────────────────────────────────────────────────────────

interface AppointmentResponse {
  externalId: string;
  personPublicId: string;
  personDisplayName: string;
  personPhotoUrl?: string | null;
  occupationPublicId: string;
  occupationTitle: string;
  occupationCode?: string;
  occupationCategory: string;
  reportsToPublicId?: string | null;
  startDate: string;
  endDate?: string | null;
  acting: boolean;
  exOffoAccess: boolean;
  verificationStatus: string;
}

interface OccupationSlot {
  publicId: string;
  title: string;
  category: string;
  reportsToPublicId?: string | null;
}

interface OrgNode {
  occupationPublicId: string;
  occupationTitle: string;
  occupationCategory: string;
  reportsToPublicId?: string | null;
  appointment?: AppointmentResponse | null;
  children: OrgNode[];
  depth: number;
}

interface OrgChartTabProps {
  institutionId: string;
  institutionName: string;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const formatDate = (iso: string) => {
  const d = new Date(iso);
  return d.toLocaleDateString('cs-CZ', { day: 'numeric', month: 'numeric', year: 'numeric' });
};

const getCategoryColor = (category: string) => {
  switch (category) {
    case 'EXECUTIVE':    return 'bg-brand-accent/20 border-brand-accent/50 text-brand-accent';
    case 'MILITARY':     return 'bg-orange-500/20 border-orange-500/50 text-orange-400';
    case 'INTELLIGENCE': return 'bg-red-500/20 border-red-500/50 text-red-400';
    case 'LEGISLATIVE':  return 'bg-purple-500/20 border-purple-500/50 text-purple-400';
    case 'JUDICIAL':     return 'bg-blue-500/20 border-blue-500/50 text-blue-400';
    case 'REGULATORY':   return 'bg-yellow-500/20 border-yellow-500/50 text-yellow-400';
    case 'GOVERNANCE':   return 'bg-teal-500/20 border-teal-500/50 text-teal-400';
    default:             return 'bg-slate-500/20 border-slate-500/40 text-slate-400';
  }
};

const getCategoryBg = (category: string) => {
  switch (category) {
    case 'EXECUTIVE':    return 'bg-brand-accent/5 border-brand-accent/20';
    case 'MILITARY':     return 'bg-orange-500/5 border-orange-500/20';
    case 'INTELLIGENCE': return 'bg-red-500/5 border-red-500/20';
    case 'LEGISLATIVE':  return 'bg-purple-500/5 border-purple-500/20';
    case 'GOVERNANCE':   return 'bg-teal-500/5 border-teal-500/20';
    default:             return 'bg-white/[0.02] border-white/10';
  }
};

// ── Tree builder ──────────────────────────────────────────────────────────────

function buildTree(appointments: AppointmentResponse[], occupations: OccupationSlot[]): OrgNode[] {
  const apptMap = new Map<string, AppointmentResponse>();
  appointments.forEach(a => apptMap.set(a.occupationPublicId, a));

  const nodeMap = new Map<string, OrgNode>();
  occupations.forEach(occ => {
    nodeMap.set(occ.publicId, {
      occupationPublicId: occ.publicId,
      occupationTitle:    occ.title,
      occupationCategory: occ.category,
      reportsToPublicId:  occ.reportsToPublicId,
      appointment:        apptMap.get(occ.publicId) ?? null,
      children:           [],
      depth:              0,
    });
  });

  const roots: OrgNode[] = [];
  nodeMap.forEach(node => {
    if (node.reportsToPublicId && nodeMap.has(node.reportsToPublicId)) {
      nodeMap.get(node.reportsToPublicId)!.children.push(node);
    } else {
      roots.push(node);
    }
  });

  const setDepth = (node: OrgNode, depth: number) => {
    node.depth = depth;
    node.children.forEach(c => setDepth(c, depth + 1));
  };
  roots.forEach(r => setDepth(r, 0));

  return roots;
}

// ── Node Card ─────────────────────────────────────────────────────────────────

const OrgNodeCard = ({
  node, isExpanded, onToggle
}: {
  node: OrgNode;
  isExpanded: boolean;
  onToggle: () => void;
}) => {
  const { appointment } = node;
  const hasChildren = node.children.length > 0;
  const isVacant    = !appointment;
  const isActing    = appointment?.acting;

  return (
    <div className={`relative w-[220px] border rounded-sm transition-all ${
      isVacant
        ? 'bg-black/30 border-white/8 opacity-70'
        : isActing
          ? 'bg-amber-950/30 border-amber-500/40'
          : getCategoryBg(node.occupationCategory)
    }`}>

      {/* Acting badge */}
      {isActing && (
        <div className="absolute -top-2.5 -right-2 bg-amber-500 text-black text-[8px] font-black uppercase px-2 py-0.5 rounded-sm z-10 shadow-lg">
          Acting
        </div>
      )}

      {/* Category header — click to open OccupationDetail */}
      <Link
        to={`/occupations/${node.occupationPublicId}`}
        className={`block px-3 py-1.5 text-[9px] font-bold uppercase tracking-widest border-b border-white/8 hover:opacity-80 transition-opacity ${getCategoryColor(node.occupationCategory)}`}
      >
        {node.occupationCategory}
        <ExternalLink size={8} className="inline ml-1 opacity-60" />
      </Link>

      {/* Position title */}
      <div className="px-3 pt-2 pb-1">
        <Link
          to={`/occupations/${node.occupationPublicId}`}
          className="block text-[12px] font-bold text-white leading-snug mb-2 hover:text-brand-accent transition-colors"
        >
          {node.occupationTitle}
        </Link>

        {/* Person section */}
        {appointment ? (
          <Link
            to={`/personnel/${appointment.personPublicId}`}
            className="flex items-start gap-2.5 p-2 bg-black/40 border border-white/8 rounded-sm hover:border-brand-accent/40 hover:bg-black/60 transition-all group"
          >
            {appointment.personPhotoUrl ? (
              <img
                src={appointment.personPhotoUrl}
                alt={appointment.personDisplayName}
                className="w-10 h-10 rounded-sm object-cover shrink-0 grayscale group-hover:grayscale-0 transition-all"
              />
            ) : (
              <div className="w-10 h-10 rounded-sm bg-white/10 flex items-center justify-center shrink-0">
                <User size={16} className="text-slate-500" />
              </div>
            )}
            <div className="min-w-0 flex-1">
              <div className="text-[12px] font-bold text-slate-100 group-hover:text-brand-accent transition-colors leading-snug">
                {appointment.personDisplayName}
              </div>
              <div className="text-[9px] text-slate-500 font-mono mt-0.5">
                {formatDate(appointment.startDate)}
                {appointment.endDate ? ` – ${formatDate(appointment.endDate)}` : ' – present'}
              </div>
              <div className="text-[12px] font-bold text-slate-100 group-hover:text-brand-accent transition-colors leading-snug">
                // Add any additional info here if needed
              </div>
            </div>
          </Link>
        ) : (
          <div className="flex items-center gap-2.5 p-2 bg-black/20 border border-dashed border-white/8 rounded-sm">
            <div className="w-10 h-10 rounded-sm bg-white/5 flex items-center justify-center shrink-0">
              <User size={16} className="text-slate-800" />
            </div>
            <span className="text-[10px] font-mono text-slate-700 uppercase tracking-wide">Vacant</span>
          </div>
        )}

        {/* Flags row */}
        {appointment && (appointment.exOffoAccess || appointment.verificationStatus === 'DECEPTION_MARKER') && (
          <div className="flex gap-1 mt-1.5 flex-wrap">
            {appointment.exOffoAccess && (
              <span className="text-[8px] bg-blue-500/20 text-blue-400 border border-blue-500/30 px-1.5 py-0.5 uppercase font-bold rounded-sm">
                Ex Officio
              </span>
            )}
            {appointment.verificationStatus === 'DECEPTION_MARKER' && (
              <span className="text-[8px] bg-red-500/20 text-red-400 border border-red-500/30 px-1.5 py-0.5 uppercase font-bold rounded-sm flex items-center gap-0.5">
                <AlertTriangle size={8} /> Deception
              </span>
            )}
          </div>
        )}
      </div>

      {/* Expand/collapse button */}
      {hasChildren && (
        <button
          onClick={onToggle}
          className="w-full flex items-center justify-center gap-1.5 py-1.5 border-t border-white/8 text-slate-600 hover:text-brand-accent hover:bg-white/[0.02] transition-all text-[9px] font-mono uppercase"
        >
          {isExpanded ? (
            <><ChevronDown size={12} /> Hide {node.children.length}</>
          ) : (
            <><ChevronRight size={12} /> Show {node.children.length}</>
          )}
        </button>
      )}
    </div>
  );
};

// ── Tree renderer ─────────────────────────────────────────────────────────────

const OrgTree = ({
  nodes, expandedIds, onToggle
}: {
  nodes: OrgNode[];
  expandedIds: Set<string>;
  onToggle: (id: string) => void;
}) => {
  if (nodes.length === 0) return null;

  return (
    <div className="flex gap-8 justify-start items-start">
      {nodes.map(node => {
        const isExpanded = expandedIds.has(node.occupationPublicId);
        return (
          <div key={node.occupationPublicId} className="flex flex-col items-center shrink-0">
            <OrgNodeCard
              node={node}
              isExpanded={isExpanded}
              onToggle={() => onToggle(node.occupationPublicId)}
            />
            {node.children.length > 0 && isExpanded && (
              <>
                {/* Vertical line down */}
                <div className="w-px h-6 bg-white/15 shrink-0" />
                {/* Horizontal bar + children */}
                <div className="relative flex gap-8 items-start">
                  {node.children.length > 1 && (
                    <div
                      className="absolute top-0 h-px bg-white/15"
                      style={{
                        left:  '110px',
                        right: '110px',
                      }}
                    />
                  )}
                  {/* Vertical drops to each child */}
                  {node.children.map(child => (
                    <div key={child.occupationPublicId} className="flex flex-col items-center shrink-0">
                      <div className="w-px h-6 bg-white/15 shrink-0" />
                      <OrgNodeCard
                        node={child}
                        isExpanded={expandedIds.has(child.occupationPublicId)}
                        onToggle={() => onToggle(child.occupationPublicId)}
                      />
                      {child.children.length > 0 && expandedIds.has(child.occupationPublicId) && (
                        <>
                          <div className="w-px h-6 bg-white/15 shrink-0" />
                          <div className="relative flex gap-8 items-start">
                            {child.children.length > 1 && (
                              <div
                                className="absolute top-0 h-px bg-white/15"
                                style={{ left: '110px', right: '110px' }}
                              />
                            )}
                            {child.children.map(grandchild => (
                              <div key={grandchild.occupationPublicId} className="flex flex-col items-center shrink-0">
                                <div className="w-px h-6 bg-white/15 shrink-0" />
                                <OrgNodeCard
                                  node={grandchild}
                                  isExpanded={expandedIds.has(grandchild.occupationPublicId)}
                                  onToggle={() => onToggle(grandchild.occupationPublicId)}
                                />
                              </div>
                            ))}
                          </div>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        );
      })}
    </div>
  );
};

// ── Timeline scrubber ─────────────────────────────────────────────────────────

const TimelineScrubber = ({
  minDate, maxDate, selectedDate, significantDates, onChange
}: {
  minDate: string;
  maxDate: string;
  selectedDate: string;
  significantDates: string[];
  onChange: (date: string) => void;
}) => {
  const min = new Date(minDate).getTime();
  const max = new Date(maxDate).getTime();
  const selected = new Date(selectedDate).getTime();
  const pct = max > min ? ((selected - min) / (max - min)) * 100 : 0;
  const containerRef = useRef<HTMLDivElement>(null);

  const getDateFromX = useCallback((clientX: number) => {
    if (!containerRef.current) return null;
    const rect = containerRef.current.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const ts = min + ratio * (max - min);
    return new Date(ts).toISOString().split('T')[0];
  }, [min, max]);

  const handleClick = useCallback((e: React.MouseEvent) => {
    const date = getDateFromX(e.clientX);
    if (date) onChange(date);
  }, [getDateFromX, onChange]);

  // Drag support
  const isDragging = useRef(false);
  const handleMouseDown = () => { isDragging.current = true; };
  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!isDragging.current) return;
    const date = getDateFromX(e.clientX);
    if (date) onChange(date);
  }, [getDateFromX, onChange]);
  const handleMouseUp = () => { isDragging.current = false; };

  return (
    <div className="px-8 py-4 bg-black/40 border-b border-white/8 select-none">
      <div className="flex justify-between text-[9px] font-mono text-slate-600 mb-3">
        <span>{minDate.substring(0, 4)}</span>
        <span className="text-brand-accent font-bold text-[11px]">{selectedDate}</span>
        <span>{maxDate.substring(0, 4)}</span>
      </div>

      <div
        ref={containerRef}
        className="relative h-8 cursor-pointer"
        onClick={handleClick}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        {/* Track */}
        <div className="absolute top-1/2 -translate-y-1/2 w-full h-[2px] bg-white/10 rounded-full" />

        {/* Progress */}
        <div
          className="absolute top-1/2 -translate-y-1/2 h-[2px] bg-brand-accent/50 rounded-full"
          style={{ width: `${pct}%` }}
        />

        {/* Tick marks */}
        {significantDates.map(date => {
          const ts = new Date(date).getTime();
          const tickPct = max > min ? ((ts - min) / (max - min)) * 100 : 0;
          return (
            <div
              key={date}
              className="absolute top-1/2 -translate-y-1/2 w-px h-4 bg-white/20 hover:bg-brand-accent/60 transition-colors cursor-pointer"
              style={{ left: `${tickPct}%` }}
              onClick={e => { e.stopPropagation(); onChange(date); }}
              title={date}
            />
          );
        })}

        {/* Thumb */}
        <div
          className="absolute top-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-brand-accent border-2 border-[#02040a] shadow-[0_0_10px_rgba(56,189,248,0.9)] -translate-x-1/2 cursor-grab active:cursor-grabbing transition-none"
          style={{ left: `${pct}%` }}
        />
      </div>
    </div>
  );
};

// ── Main component ────────────────────────────────────────────────────────────

const OrgChartTab: React.FC<OrgChartTabProps> = ({ institutionId }) => {
  const today = new Date().toISOString().split('T')[0];
  const [selectedDate, setSelectedDate] = useState(today);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [showVacant, setShowVacant] = useState(true);
  const [zoom, setZoom] = useState(1.0);

  const { data: appointments = [], isLoading: isApptLoading } = useQuery<AppointmentResponse[]>({
    queryKey: ['institution-appointments', institutionId, selectedDate],
    queryFn: async () => (await api.get(`/appointments/institution/${institutionId}`, {
      params: { atDate: selectedDate }
    })).data,
    enabled: !!institutionId,
  });

  const { data: occupations = [] } = useQuery<OccupationSlot[]>({
    queryKey: ['institution-occupations-slots', institutionId],
    queryFn: async () => {
      const res = await api.get(`/occupations/institution/${institutionId}`);
      return res.data.map((o: any) => ({
        publicId:          o.publicId,
        title:             o.title,
        category:          o.category,
        reportsToPublicId: o.reportsToPublicId,
      }));
    },
    enabled: !!institutionId,
  });

  const { data: significantDates = [] } = useQuery<string[]>({
    queryKey: ['institution-significant-dates', institutionId],
    queryFn: async () => (await api.get(`/appointments/institution/${institutionId}/dates`)).data,
    enabled: !!institutionId,
  });

  const minDate = significantDates.length > 0 ? significantDates[0] : today;
  const maxDate = today;

  const orgTree = useMemo(() => {
    const visible = showVacant
      ? occupations
      : occupations.filter(o => appointments.some(a => a.occupationPublicId === o.publicId));
    return buildTree(appointments, visible);
  }, [appointments, occupations, showVacant]);

  // Auto-expand root nodes on first load
  React.useEffect(() => {
    if (orgTree.length > 0 && expandedIds.size === 0) {
      setExpandedIds(new Set(orgTree.map(n => n.occupationPublicId)));
    }
  }, [orgTree]);

  const toggleNode = useCallback((id: string) => {
    setExpandedIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }, []);

  const expandAll = () => {
    const all = new Set<string>();
    const collect = (nodes: OrgNode[]) => nodes.forEach(n => { all.add(n.occupationPublicId); collect(n.children); });
    collect(orgTree);
    setExpandedIds(all);
  };

  const collapseAll = () => setExpandedIds(new Set());

  const filledCount  = appointments.length;
  const vacantCount  = occupations.length - filledCount;
  const actingCount  = appointments.filter(a => a.acting).length;

  return (
    <div className="flex flex-col h-full min-h-0">

      {/* Timeline */}
      {significantDates.length > 0 && (
        <TimelineScrubber
          minDate={minDate}
          maxDate={maxDate}
          selectedDate={selectedDate}
          significantDates={significantDates}
          onChange={setSelectedDate}
        />
      )}

      {/* Controls */}
      <div className="flex items-center justify-between px-6 py-2.5 border-b border-white/5 bg-black/20 shrink-0 flex-wrap gap-2">
        <div className="flex items-center gap-5 text-[10px] font-mono uppercase">
          <span className="flex items-center gap-1.5 text-brand-accent font-bold">
            <Users size={12} /> {filledCount} Filled
          </span>
          <span className="flex items-center gap-1.5 text-slate-600">
            <User size={12} /> {vacantCount} Vacant
          </span>
          {actingCount > 0 && (
            <span className="flex items-center gap-1.5 text-amber-400 font-bold">
              <AlertTriangle size={12} /> {actingCount} Acting
            </span>
          )}
          <span className="flex items-center gap-1.5 text-slate-700">
            <Calendar size={12} /> {selectedDate}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => setShowVacant(v => !v)}
            className={`text-[9px] font-mono uppercase px-3 py-1 border transition-all ${
              showVacant
                ? 'border-white/8 text-slate-600 hover:text-slate-300'
                : 'border-brand-accent/30 text-brand-accent bg-brand-accent/5'
            }`}>
            {showVacant ? 'Hide vacant' : 'Show vacant'}
          </button>
          <button onClick={expandAll}   className="text-[9px] font-mono uppercase px-3 py-1 border border-white/8 text-slate-500 hover:text-slate-300 transition-all">Expand all</button>
          <button onClick={collapseAll} className="text-[9px] font-mono uppercase px-3 py-1 border border-white/8 text-slate-500 hover:text-slate-300 transition-all">Collapse</button>
          <div className="flex items-center border border-white/8 overflow-hidden">
            <button
              onClick={() => setZoom(z => Math.max(0.4, +(z - 0.1).toFixed(1)))}
              className="px-3 py-1 text-slate-500 hover:text-slate-200 hover:bg-white/5 transition-all text-[14px] font-bold border-r border-white/8"
              title="Zoom out"
            >−</button>
            <span className="px-2 text-[9px] font-mono text-slate-600">{Math.round(zoom * 100)}%</span>
            <button
              onClick={() => setZoom(z => Math.min(2.0, +(z + 0.1).toFixed(1)))}
              className="px-3 py-1 text-slate-500 hover:text-slate-200 hover:bg-white/5 transition-all text-[14px] font-bold border-l border-white/8"
              title="Zoom in"
            >+</button>
          </div>
        </div>
      </div>

      {/* Chart — horizontally and vertically scrollable */}
      <div className="flex-1 overflow-auto min-h-0">
        <div
          className="p-8 inline-block min-w-full origin-top-left transition-transform"
          style={{ transform: `scale(${zoom})`, transformOrigin: 'top left' }}
        >
          {isApptLoading ? (
            <div className="flex items-center justify-center h-64 font-mono text-[11px] text-brand-accent animate-pulse uppercase tracking-widest">
              Rendering structure at {selectedDate}...
            </div>
          ) : orgTree.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 gap-3 border border-dashed border-white/10">
              <Clock size={32} className="text-slate-800" />
              <p className="text-[12px] text-slate-700 font-mono uppercase">
                No appointments recorded at {selectedDate}
              </p>
              {significantDates.length > 0 && (
                <button
                  onClick={() => setSelectedDate(significantDates[0])}
                  className="text-[10px] text-brand-accent/70 hover:text-brand-accent border border-brand-accent/20 px-3 py-1.5 transition-colors font-mono uppercase"
                >
                  Jump to first record ({significantDates[0]})
                </button>
              )}
            </div>
          ) : (
            <OrgTree
              nodes={orgTree}
              expandedIds={expandedIds}
              onToggle={toggleNode}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default OrgChartTab;