import axios from 'axios';
import type { InstitutionTreeResponse } from '../../types';

const API_BASE_URL = '/api/v1/institutions';

export const institutionService = {
  // Pro hierarchický strom (Lazy Loading)
  getChildren: async (parentPublicId: string): Promise<InstitutionTreeResponse[]> => {
    const response = await axios.get<InstitutionTreeResponse[]>(`${API_BASE_URL}/parent/${parentPublicId}`);
    return response.data;
  },

  // Pro kořenové prvky (ČR, EU)
  getRootNodes: async (): Promise<InstitutionTreeResponse[]> => {
    const response = await axios.get<InstitutionTreeResponse[]>(`${API_BASE_URL}/tree`);
    return response.data;
  },

  // Pro vizuální Nexus graf (Hluboký strom)
  getSubTree: async (publicId: string): Promise<InstitutionTreeResponse> => {
    const response = await axios.get<InstitutionTreeResponse>(`${API_BASE_URL}/${publicId}/sub-tree`);
    return response.data;
  }
};