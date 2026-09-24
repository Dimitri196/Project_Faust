import React from 'react';
import { ChevronRight, MapPin, Terminal } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { InstitutionAscendedResponse } from '../../types';

interface BreadcrumbsProps {
  data: InstitutionAscendedResponse;
}

const InstitutionBreadcrumbs: React.FC<BreadcrumbsProps> = ({ data }) => {
  const navigate = useNavigate();

  // CHANGED: explicit InstitutionAscendedResponse[] type annotation,
  // replacing the untyped `const chain = []`. Without it, TypeScript
  // infers `never[]`/`any[]` depending on strictness settings, which can
  // produce a type error on `.unshift(current)` under strict mode since
  // `current` is a typed object but the array wasn't.
  const getChain = (node: InstitutionAscendedResponse): InstitutionAscendedResponse[] => {
    const chain: InstitutionAscendedResponse[] = [];
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
        {chain.map((item, index) => {
          const isCurrent = index === chain.length - 1;
          return (
            <React.Fragment key={item.publicId}>
              {/* CHANGED: each item is now a button that navigates to
                  /institutions/{publicId}, except the current node itself
                  (no point navigating to the page you're already on —
                  disabled instead, with a visual cue). Previously this
                  entire component was non-interactive — it displayed the
                  path to root but offered no way to actually jump to an
                  ancestor, which defeats the point of a breadcrumb trail. */}
              <button
                onClick={() => !isCurrent && navigate(`/institutions/${item.publicId}`)}
                disabled={isCurrent}
                className={`group flex items-center gap-2 ${isCurrent ? 'cursor-default' : 'cursor-pointer'}`}
              >
                {/* CHANGED: MapPin instead of ShieldCheck for the current-
                    position marker. ShieldCheck visually implies a
                    verification/security status (matching its use
                    elsewhere in this app, e.g. VerificationBadge), which
                    could mislead an analyst into reading it as "this
                    institution is verified" rather than "you are here". */}
                {isCurrent && <MapPin size={14} className="text-brand-accent mr-1" />}

                <div className="flex flex-col text-left">
                  <span className={`text-[10px] uppercase tracking-tighter transition-colors
                    ${isCurrent ? 'text-white font-black' : 'text-slate-500 group-hover:text-brand-accent group-hover:underline'}`}>
                    {item.name}
                  </span>
                  <span className="text-[7px] text-slate-600 leading-none">
                    {item.type}
                  </span>
                </div>
              </button>

              {index < chain.length - 1 && (
                <ChevronRight size={14} className="mx-2 text-slate-700" />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
};

export default InstitutionBreadcrumbs;