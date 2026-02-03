import api from './axios';
import type { PersonResponse } from '../types/index';

export const PeopleAPI = {
    getAll: async () => {
        const { data } = await api.get<PersonResponse[]>('/persons');
        return data;
    },
    getById: async (id: string) => {
        const { data } = await api.get<PersonResponse>(`/persons/${id}`);
        return data;
    }
};