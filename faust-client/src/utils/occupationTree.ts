import type { OccupationTreeResponse } from '../types';

// NEW: shared tree utilities, extracted from duplicated implementations
// in OccupationsPage.tsx and HierarchyPage.tsx (which had slightly
// different parameter styles — `nodes ? countNodes(nodes) : 0` vs
// `countNodes(node.subordinates || [])` — identical logic, copy-pasted
// twice. Centralizing here means a future fix only needs to happen once.

/**
 * Recursively counts every node in an occupation tree, including the
 * roots and all nested subordinates.
 */
export const countNodes = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + 1 + countNodes(node.subordinates || []), 0);
};

/**
 * Recursively counts vacant positions across an occupation tree.
 *
 * Uses `node.vacant` (renamed from `isVacant`) — the previous field name
 * was always `undefined` on the frontend type, silently producing a
 * vacancy count of 0 regardless of actual data.
 */
export const countVacancies = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + (node.vacant ? 1 : 0) + countVacancies(node.subordinates || []), 0);
};