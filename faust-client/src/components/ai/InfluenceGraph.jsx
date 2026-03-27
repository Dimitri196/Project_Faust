import React, { useRef, useCallback, useEffect } from 'react';
import ForceGraph2D from 'react-force-graph-2d';

const InfluenceGraph = ({ rawData, highlightPath, onNodeClick, riskMode }) => {
  const graphRef = useRef();

  // Stabilizace grafu a rozestupy
  useEffect(() => {
    if (graphRef.current) {
      graphRef.current.d3Force('charge').strength(-400);
      graphRef.current.d3Force('link').distance(100);
    }
  }, [rawData]);

  const data = React.useMemo(() => {
    if (!rawData) return { nodes: [], links: [] };
    const nodes = [{
      id: rawData.rootId,
      name: rawData.rootFullName,
      val: 12,
      color: '#3b82f6', 
      isRoot: true,
      position: 'SUBJECT_OF_INTEREST'
    }];

    const links = rawData.connections.map(conn => {
      if (!nodes.find(n => n.id === conn.targetId)) {
        nodes.push({
          id: conn.targetId,
          name: conn.targetFullName,
          val: 8,
          color: conn.isActive ? '#10b981' : '#ef4444', 
          position: conn.targetCurrentPosition
        });
      }
      return {
        source: rawData.rootId,
        target: conn.targetId,
        type: conn.type || 'UNKNOWN', // Tady už bude FAMILY_MEMBER
        score: conn.influenceScore,
        isActive: conn.isActive,
        // Pomocný flag pro Risk Scan
        isRisk: conn.type === 'FAMILY_MEMBER' || conn.targetFullName.includes('Radhi')
      };
    });
    return { nodes, links };
  }, [rawData]);

  // Vykreslování uzlů s fixním měřítkem textu
  const paintNode = useCallback((node, ctx, globalScale) => {
    const isHighlighted = highlightPath && highlightPath.has(node.id);
    const isDimmed = highlightPath && highlightPath.size > 0 && !isHighlighted;
    
    // FIX: Text už není "fontSize / globalScale". 
    // Držíme fixní hodnotu, aby se text zvětšoval přirozeně s zoomem.
    const labelFontSize = 12; 
    const subLabelFontSize = 7;

    // Glow efekt
    if (isHighlighted || node.isRoot || (riskMode && node.color === '#ef4444')) {
      ctx.shadowBlur = 15 / globalScale;
      ctx.shadowColor = node.color;
    }

    // Kresba kuličky
    ctx.beginPath();
    ctx.arc(node.x, node.y, node.isRoot ? 6 : 4, 0, 2 * Math.PI, false);
    ctx.fillStyle = isDimmed ? '#1e293b' : node.color;
    ctx.fill();
    ctx.shadowBlur = 0;

    // Vykreslení textu (pouze při rozumném zoomu)
    if (globalScale > 0.4) {
      const label = node.name.toUpperCase();
      ctx.font = `bold ${labelFontSize}px "Courier New", monospace`;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      
      const textWidth = ctx.measureText(label).width;
      
      // Pozadí pod textem pro čitelnost
      ctx.fillStyle = 'rgba(2, 6, 23, 0.85)';
      ctx.fillRect(node.x + 8, node.y - labelFontSize/2, textWidth + 4, labelFontSize + 2);

      // Jméno
      ctx.fillStyle = isDimmed ? '#475569' : (isHighlighted ? '#fff' : '#cbd5e1');
      ctx.fillText(label, node.x + 10, node.y);
      
      // Pozice (pod jménem)
      if (node.position) {
        ctx.font = `${subLabelFontSize}px "Courier New", monospace`;
        ctx.fillStyle = isDimmed ? '#1e293b' : '#64748b';
        ctx.fillText(node.position, node.x + 10, node.y + labelFontSize * 0.8);
      }
    }
  }, [highlightPath, riskMode]);

  return (
    <div className="w-full h-full">
      <ForceGraph2D
        ref={graphRef}
        graphData={data}
        backgroundColor="#020617"
        nodeCanvasObject={paintNode}
        // Definice oblasti pro klikání (fix pro tvůj "nelze udělat klik")
        nodePointerAreaPaint={(node, color, ctx) => {
          ctx.fillStyle = color;
          ctx.beginPath();
          ctx.arc(node.x, node.y, 10, 0, 2 * Math.PI, false);
          ctx.fill();
        }}
        onNodeClick={onNodeClick}
        
        // --- DYNAMICKÁ LINK LOGIC ---
        linkColor={l => {
          if (riskMode && l.isRisk) return '#ff0000'; // Risk Scan barva
          const isPath = highlightPath && highlightPath.has(l.source.id) && highlightPath.has(l.target.id);
          return isPath ? '#3b82f6' : (l.isActive ? '#334155' : '#ef444433');
        }} 
        linkWidth={l => {
          if (riskMode && l.isRisk) return 4;
          return highlightPath?.has(l.target.id) ? 3 : 1;
        }}
        linkDashArray={l => (l.isActive || (riskMode && l.isRisk)) ? [] : [3, 2]}
        
        // Popisky na linkách
        linkCanvasObjectMode={() => 'after'}
        linkCanvasObject={(link, ctx, globalScale) => {
          if (globalScale < 1.5 || !link.type) return;
          const start = link.source;
          const end = link.target;
          const midX = start.x + (end.x - start.x) * 0.5;
          const midY = start.y + (end.y - start.y) * 0.5;
          
          ctx.font = `${7}px "Courier New", monospace`;
          ctx.fillStyle = (riskMode && link.isRisk) ? '#ff4444' : '#60a5fa';
          ctx.textAlign = 'center';
          
          // Pokud je riskMode, text se změní na ALERT
          const label = (riskMode && link.isRisk) ? `!!! ${link.type} !!!` : link.type;
          ctx.fillText(label, midX, midY - 4);
        }}

        // Částice (tok vlivu)
        linkDirectionalParticles={l => {
          if (riskMode && l.isRisk) return 8; // Masivní tok při alertu
          return l.isActive ? 2 : 0;
        }}
        linkDirectionalParticleWidth={l => (riskMode && l.isRisk) ? 3 : 2}
        linkDirectionalParticleSpeed={l => (riskMode && l.isRisk) ? 0.04 : l.score * 0.01}
        linkDirectionalParticleColor={l => (riskMode && l.isRisk) ? '#ff0000' : '#3b82f6'}
      />
    </div>
  );
};
export default InfluenceGraph;