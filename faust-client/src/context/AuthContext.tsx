import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { UserProfile, AuthResponse } from '../types';

interface AuthContextType {
    user: UserProfile | null;
    // CHANGED: login() now accepts the full AuthResponse from POST /auth/login,
    // not just a UserProfile. It extracts the token for storage and derives
    // the UserProfile shape from the remaining fields.
    login: (authResponse: AuthResponse) => void;
    updateUser: (updates: Partial<UserProfile>) => void;
    logout: () => void;
    isAuthenticated: boolean;
    isLoading: boolean;
    // NEW: expose the token so it can be read by the axios interceptor
    // without importing AuthContext into axios.ts (avoids circular imports).
    token: string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// NEW: separate storage keys for token vs profile.
// The token is sensitive and short-lived (24h per faust.jwt.expiration-ms);
// keeping it separate makes it easier to clear independently on 401.
const SESSION_KEY = 'faust_session';
const TOKEN_KEY = 'faust_token';

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<UserProfile | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // 1. Recover session on mount.
    // CHANGED: now also recovers the token. If either is missing/corrupt,
    // the whole session is cleared — a profile without a token is useless
    // since every API call (including /profile/me) now requires the JWT.
    useEffect(() => {
        const savedUser = localStorage.getItem(SESSION_KEY);
        const savedToken = localStorage.getItem(TOKEN_KEY);

        if (savedUser && savedToken) {
            try {
                setUser(JSON.parse(savedUser));
                setToken(savedToken);
            } catch (e) {
                console.error('Session_Corruption_Detected');
                localStorage.removeItem(SESSION_KEY);
                localStorage.removeItem(TOKEN_KEY);
            }
        } else if (savedUser || savedToken) {
            // CHANGED: partial session (one without the other) is invalid — clear both.
            localStorage.removeItem(SESSION_KEY);
            localStorage.removeItem(TOKEN_KEY);
        }

        setIsLoading(false);
    }, []);

    // 2. Login with persistence.
    // CHANGED: accepts AuthResponse (token + profile fields combined) instead
    // of a pre-built UserProfile. Splits the response into:
    //   - token        -> stored separately for the axios interceptor
    //   - UserProfile  -> derived from the remaining AuthResponse fields
    //
    // NOTE: AuthResponse does not include techStack, status, or
    // createdAt/updatedAt (ProfileResponse has these, AuthResponse doesn't).
    // These are set to safe defaults here and should be refreshed via a
    // GET /profile/me call shortly after login if the UI needs them.
    const login = (authResponse: AuthResponse) => {
        const profile: UserProfile = {
            id: authResponse.userId,
            fullName: authResponse.fullName,
            email: authResponse.email,
            role: authResponse.role,
            clearance: authResponse.clearance,
            // Not present in AuthResponse — placeholder until /profile/me refresh.
            status: 'OPERATIONAL',
            techStack: [],
            admin: authResponse.role === 'ADMIN' || authResponse.role === 'SUPER_ADMIN',
            createdAt: '',
            updatedAt: '',
        };

        setUser(profile);
        setToken(authResponse.token);
        localStorage.setItem(SESSION_KEY, JSON.stringify(profile));
        localStorage.setItem(TOKEN_KEY, authResponse.token);
    };

    // 3. Update without logout (use after a successful PATCH to /profile/dossier/{id}).
    // UNCHANGED logic, but also re-syncs token storage key naming for consistency.
    const updateUser = (updates: Partial<UserProfile>) => {
        setUser(prev => {
            if (!prev) return null;
            const updated = { ...prev, ...updates };
            localStorage.setItem(SESSION_KEY, JSON.stringify(updated));
            return updated;
        });
    };

    // 4. Wipe session.
    // CHANGED: now also clears the token.
    const logout = () => {
        setUser(null);
        setToken(null);
        localStorage.removeItem(SESSION_KEY);
        localStorage.removeItem(TOKEN_KEY);
    };

    return (
        <AuthContext.Provider value={{
            user,
            login,
            updateUser,
            logout,
            isAuthenticated: !!user && !!token,
            isLoading,
            token,
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
