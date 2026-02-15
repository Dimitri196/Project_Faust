import React from 'react';
import { ChevronRight, ShieldCheck, Terminal } from 'lucide-react';
import type { InstitutionAscendedResponse } from '../../types';

interface BreadcrumbsProps {
  data: InstitutionAscendedResponse;
}

const InstitutionBreadcrumbs: React.FC<BreadcrumbsProps> = ({ data }) => {
  // We need to flatten the recursive "parent" structure into an array for rendering
  const getChain = (node: InstitutionAscendedResponse): InstitutionAscendedResponse[] => {
    const chain = [];
    let current: InstitutionAscendedResponse | null = node;
    while (current) {
      chain.unshift(current);
      current = current.parent;
    }
    return chain;
  };

  const chain = getChain(data);

  return (
    <div className="w-full bg-brand-panel/20 border border-brand-border p-4 backdrop-blur-sm">
      <div className="flex items-center gap-2 mb-3 text-brand-accent opacity-70">
        <Terminal size={12} />
        <span className="text-[9px] font-mono uppercase tracking-[0.2em]">Ascended_Path_To_Root</span>
      </div>
      
      <div className="flex flex-wrap items-center gap-y-2 font-mono">
        {chain.map((item, index) => (
          <React.Fragment key={item.publicId}>
            <div className="group flex items-center gap-2">
              {index === chain.length - 1 && <ShieldCheck size={14} className="text-brand-success mr-1" />}
              
              <div className="flex flex-col">
                <span className={`text-[10px] uppercase tracking-tighter transition-colors
                  ${index === chain.length - 1 ? 'text-white font-black' : 'text-slate-500 group-hover:text-slate-300'}`}>
                  {item.name}
                </span>
                <span className="text-[7px] text-slate-600 leading-none">
                  {item.type}
                </span>
              </div>
            </div>

            {index < chain.length - 1 && (
              <ChevronRight size={14} className="mx-2 text-slate-700" />
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};

export default InstitutionBreadcrumbs;