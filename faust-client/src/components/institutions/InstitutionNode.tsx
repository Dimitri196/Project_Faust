import React, { useState, useEffect, useCallback } from 'react';
import {
  ChevronRight,
  ChevronDown,
  Building2,
  Loader2,
  ShieldAlert,
  Zap,
  Scale,
  Landmark,
  ShieldCheck
} from 'lucide-react';
import type { InstitutionTreeResponse } from '../../types';
import { institutionService } from '../institutions/institutionService';

interface NodeProps {
  node: InstitutionTreeResponse;
  depth: number;
}

// 3. STATICKÉ MAPOVÁNÍ IKON (Alokováno v paměti pouze jednou mimo renderovací cyklus)
const ICON_MAP: Record<string, React.ReactNode> = {
  INTELLIGENCE: <ShieldAlert size={14} className="text-red-500 animate-pulse shrink-0" />,
  MILITARY: <ShieldCheck size={14} className="text-orange-500 shrink-0" />,
  LEGISLATIVE: <Landmark size={14} className="text-purple-400 shrink-0" />,
  REGULATORY: <Zap size={14} className="text-yellow-400 shrink-0" />,
  JUDICIAL: <Scale size={14} className="text-blue-400 shrink-0" />,
  EXECUTIVE: <Building2 size={14} className="text-brand-accent shrink-0" />,
};
const DEFAULT_ICON = <Building2 size={14} className="text-slate-500 shrink-0" />;

// --- KOMPONENTA UZLU ---
const InstitutionNodeComponent: React.FC<NodeProps> = ({ node, depth }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [childNodes, setChildNodes] = useState<InstitutionTreeResponse[]>(node.children || []);
  const [isLoading, setIsLoading] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [imgError, setImgError] = useState(false);

  // 2. RESET CHYBY OBRÁZKU při změně identity uzlu nebo URL
  useEffect(() => {
    setImgError(false);
    setChildNodes(node.children || []);
  }, [node.publicId, node.logoUrl, node.children]);

  // 4. BEZPEČNÝ TOGGLE HANDLER s ochranou proti vícenásobnému kliknutí
  const toggleOpen = async (e: React.MouseEvent) => {
    e.stopPropagation();

    // Pokud už načítání běží, okamžitě ignoruj další kliknutí
    if (isLoading) return;

    if (!isOpen && !hasLoaded && node.hasChildren) {
      setIsLoading(true);
      try {
        const data = await institutionService.getChildren(node.publicId);
        setChildNodes(data);
        setHasLoaded(true);
      } catch (error) {
        console.error("FAUST_ERROR: Failed to fetch sub-hierarchy", error);
      } finally {
        setIsLoading(false);
      }
    }
    setIsOpen(prev => !prev);
  };

  const renderBranding = () => {
    if (node.logoUrl && !imgError) {
      return (
        <div className="w-5 h-5 bg-black/40 rounded-sm border border-white/5 overflow-hidden flex items-center justify-center p-0.5 group-hover:border-brand-accent/30 transition-colors shrink-0">
          <img
            src={node.logoUrl}
            alt=""
            crossOrigin="anonymous"
            className="w-full h-full object-contain grayscale group-hover:grayscale-0 transition-all duration-300"
            onError={() => setImgError(true)}
          />
        </div>
      );
    }
    return ICON_MAP[node.type] || DEFAULT_ICON;
  };

  return (
    <div className={`select-none ${depth > 0 ? 'ml-4 border-l border-brand-border/10' : ''}`}>
      <div
        onClick={toggleOpen}
        className={`
          group flex items-center gap-3 py-2.5 px-4 cursor-pointer 
          hover:bg-brand-accent/5 transition-all relative
          w-[420px] min-h-[48px]
          ${isOpen ? 'bg-white/[0.02]' : ''}
        `}
      >
        {depth > 0 && (
          <div className="absolute left-0 top-1/2 w-4 h-px bg-brand-border/10" />
        )}

        {/* Indikátor / Loader */}
        <div className="flex items-center justify-center w-4 shrink-0 z-10">
          {isLoading ? (
            <Loader2 size={10} className="animate-spin text-brand-accent" />
          ) : node.hasChildren ? (
            <div className="transition-transform duration-200">
              {isOpen ? (
                <ChevronDown size={12} className="text-brand-accent" />
              ) : (
                <ChevronRight size={12} className="text-slate-500 group-hover:text-slate-300" />
              )}
            </div>
          ) : (
            <div className="w-1 h-1 rounded-full bg-slate-700" />
          )}
        </div>

        {/* Branding slot */}
        <div className="shrink-0 flex items-center justify-center w-5">
          {renderBranding()}
        </div>

        {/* Informační blok se zalamováním */}
        <div className="flex flex-col min-w-0 flex-1 py-0.5">
          <span className={`
            text-[10px] font-bold tracking-tight transition-colors leading-relaxed
            block break-words
            ${node.active ? 'text-slate-300' : 'text-slate-500 line-through italic'}
            ${isOpen && node.hasChildren ? 'text-brand-accent' : 'group-hover:text-white'}
          `}>
            {node.name.toUpperCase()}
          </span>
          <div className="flex items-center gap-2 mt-0.5">
            <span className="text-[7px] font-mono text-slate-500 uppercase tracking-tighter shrink-0">
              {node.level}
            </span>
          </div>
        </div>

        {/* State Property Badge */}
        {node.isStateOwned && (
          <div className="ml-auto shrink-0 flex items-center gap-1.5 pl-2">
            <div className="w-1 h-1 rounded-full bg-brand-accent/30 animate-pulse hidden group-hover:block" />
            <span className="
              text-[6px] px-1.5 py-0.5
              bg-brand-accent/5 border border-brand-accent/20 
              text-brand-accent font-mono font-bold tracking-tighter 
              opacity-40 group-hover:opacity-100 transition-opacity
            ">
              S_ENT
            </span>
          </div>
        )}
      </div>

      {/* Rekurzivní vykreslení dětí */}
      {isOpen && childNodes.length > 0 && (
        // Změna: nahrazeno optimalizovanou animací 'animate-node-appear' s podporou 'will-change-transform'
        <div className="animate-node-appear will-change-transform">
          {childNodes.map((child) => (
            <InstitutionNode
              key={child.publicId}
              node={child}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
      {/* Empty State */}
      {isOpen && hasLoaded && childNodes.length === 0 && (
        <div className="ml-10 py-1 text-[8px] font-mono text-slate-600 italic border-l border-slate-800/50 pl-2 w-[380px]">
          &gt; NULL_CHILDREN_DETECTED
        </div>
      )}
    </div>
  );
};

// 1. MEMOIZACE S POROVNÁVACÍ FUNKCÍ pro maximální výkon v rekurzi
export const InstitutionNode = React.memo(InstitutionNodeComponent, (prevProps, nextProps) => {
  return (
    prevProps.depth === nextProps.depth &&
    prevProps.node.publicId === nextProps.node.publicId &&
    prevProps.node.active === nextProps.node.active &&
    prevProps.node.logoUrl === nextProps.node.logoUrl &&
    prevProps.node.name === nextProps.node.name &&
    prevProps.node.children?.length === nextProps.node.children?.length
  );
});

// Zajištění korektního názvu komponenty pro debuggování v React DevTools
InstitutionNode.displayName = 'InstitutionNode';

export default InstitutionNode;