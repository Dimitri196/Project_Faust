import React, { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import type { UserProfile } from '../types';

interface AuthContextType {
    user: UserProfile | null;
    login: (profile: UserProfile) => void;
    logout: () => void;
    isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<UserProfile | null>(null);

    const login = (profile: UserProfile) => setUser(profile);
    const logout = () => setUser(null);

    return (
        <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within AuthProvider');
    return context;
};
