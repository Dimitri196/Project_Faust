// hooks/useInstitutionExport.ts
import { useCallback } from 'react';
import { toPng } from 'html-to-image';

export const useInstitutionExport = (flowRef: React.RefObject<HTMLDivElement>) => {
  
  // LOGIKA PRO PNG
  const exportAsPng = useCallback(() => {
    if (!flowRef.current) return;
    const viewport = flowRef.current.querySelector('.react-flow__viewport') as HTMLElement;
    
    toPng(viewport, { backgroundColor: '#0f172a' }).then((dataUrl) => {
      const link = document.createElement('a');
      link.download = `faust-export-${Date.now()}.png`;
      link.href = dataUrl;
      link.click();
    });
  }, [flowRef]);

  // LOGIKA PRO MERMAID
  const exportAsMermaid = useCallback((nodes: any[], edges: any[]) => {
    let code = "graph TD\n";
    // ... sem vložíš tu logiku s classDef a cykly, co jsem poslal minule ...
    
    navigator.clipboard.writeText(code);
    alert("Struktura zkopírována jako Mermaid kód!");
  }, []);

  return { exportAsPng, exportAsMermaid };
};