import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { UserProfile } from '../types';

interface AuthContextType {
    user: UserProfile | null;
    login: (profile: UserProfile) => void;
    updateUser: (updates: Partial<UserProfile>) => void; // Pro synchronizaci po PATCHi
    logout: () => void;
    isAuthenticated: boolean;
    isLoading: boolean; // Důležité pro checkování session při startu
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<UserProfile | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // 1. Recover session při startu (Mount)
    useEffect(() => {
        const savedUser = localStorage.getItem('faust_session');
        if (savedUser) {
            try {
                setUser(JSON.parse(savedUser));
            } catch (e) {
                console.error("Session_Corruption_Detected");
                localStorage.removeItem('faust_session');
            }
        }
        setIsLoading(false);
    }, []);

    // 2. Login s persistencí
    const login = (profile: UserProfile) => {
        setUser(profile);
        localStorage.setItem('faust_session', JSON.stringify(profile));
    };

    // 3. Update bez logoutu (použij v Dossieru po úspěšném Patchi)
    const updateUser = (updates: Partial<UserProfile>) => {
        setUser(prev => {
            if (!prev) return null;
            const updated = { ...prev, ...updates };
            localStorage.setItem('faust_session', JSON.stringify(updated));
            return updated;
        });
    };

    // 4. Wipe session
    const logout = () => {
        setUser(null);
        localStorage.removeItem('faust_session');
        // Zde by mohl být i call na backend api.post('/auth/logout')
    };

    return (
        <AuthContext.Provider value={{ 
            user, 
            login, 
            updateUser, 
            logout, 
            isAuthenticated: !!user,
            isLoading 
        }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within AuthProvider');
    return context;
};
