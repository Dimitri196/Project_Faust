import React, { useState } from 'react';
import { ChevronDown, ChevronRight, User, UserMinus } from 'lucide-react';
import type { OccupationTreeResponse } from '../../types';

const OccupationNodeVisual = ({ node, depth }: { node: OccupationTreeResponse; depth: number }) => {
  // Inicializace na false = vše sbaleno
  const [isOpen, setIsOpen] = useState(false);
  const hasSubordinates = node.subordinates && node.subordinates.length > 0;

  return (
    <div className="relative">
      {/* Vertikální linka propojující úrovně - vykresluje se jen do výšky uzlu, pokud je zavřeno */}
      {depth > 0 && (
        <div 
          className="absolute -left-6 top-0 bottom-0 w-px bg-brand-accent/20" 
          style={{ height: hasSubordinates && isOpen ? '100%' : '24px' }}
        />
      )}
      
      {/* Horizontální spojka k uzlu */}
      {depth > 0 && (
        <div className="absolute -left-6 top-[24px] w-6 h-px bg-brand-accent/20" />
      )}

      <div className="group relative flex items-center gap-3 mb-2">
        {/* Toggle Button přímo na lince */}
        {hasSubordinates && (
          <button 
            onClick={() => setIsOpen(!isOpen)} 
            className="absolute -left-[30px] top-[18px] w-3 h-3 bg-brand-dark border border-brand-accent/40 rounded-full flex items-center justify-center z-10 hover:border-brand-accent transition-colors"
          >
            {isOpen ? <ChevronDown size={8} className="text-brand-accent" /> : <ChevronRight size={8} />}
          </button>
        )}

        <div className={`
          flex-1 flex items-center gap-4 p-3 border transition-all
          ${node.isVacant ? 'bg-red-500/5 border-red-900/30' : 'bg-black/60 border-brand-border/40 hover:border-brand-accent/40'}
        `}>
          <div className={`p-1.5 rounded-sm ${node.isVacant ? 'bg-red-500/10' : 'bg-brand-accent/5'}`}>
            {node.isVacant ? <UserMinus size={14} className="text-red-500" /> : <User size={14} className="text-brand-accent" />}
          </div>
          
          <div className="flex-1 min-w-0">
            <div className="text-[10px] font-black text-white uppercase tracking-tight truncate">
              {node.title}
            </div>
            <div className="text-[8px] font-mono text-slate-500 uppercase">
              {node.isVacant ? '[ VACANT_SLOT ]' : node.currentOccupantName}
            </div>
          </div>

          <div className="text-[7px] font-mono px-2 py-0.5 border border-brand-border/50 text-slate-500">
            {node.rank}
          </div>
        </div>
      </div>

      {/* KRITICKÁ OPRAVA: Rekurze je uvnitř podmínky isOpen */}
      {isOpen && hasSubordinates && (
        <div className="ml-10 animate-in fade-in slide-in-from-left-1 duration-200">
          {node.subordinates.map((sub) => (
            <OccupationNodeVisual key={sub.publicId} node={sub} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

export default OccupationNodeVisual;