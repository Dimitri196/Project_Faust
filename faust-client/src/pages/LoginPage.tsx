import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import axiosInstance from '../api/axios';
import type { AuthResponse, LoginRequest } from '../types';

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    // NEW: surface backend error messages (e.g. "Invalid email or password.")
    // instead of a generic alert().
    const [error, setError] = useState<string | null>(null);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleIdentification = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsProcessing(true);
        setError(null);

        try {
            // CHANGED: was GET /profile/me?email=... (no password check at all).
            // Now calls POST /auth/login with { email, password } and receives
            // an AuthResponse containing the JWT token + profile fields.
            const payload: LoginRequest = { email, password };
            const response = await axiosInstance.post<AuthResponse>('/auth/login', payload);

            // CHANGED: login() now takes the full AuthResponse (token + profile),
            // not a bare UserProfile. AuthContext splits it internally.
            login(response.data);

            navigate('/');
        } catch (err: any) {
            // CHANGED: read the backend's ProblemDetail response.
            // GlobalExceptionHandler returns { detail, title, status } for
            // BadCredentialsException -> "Invalid email or password." (401).
            const detail = err?.response?.data?.detail;
            setError(detail ?? 'ACCESS DENIED: IDENTITY NOT FOUND');
            setIsProcessing(false);
        }
    };

    return (
        <div className="flex min-h-screen w-full items-center justify-center bg-brand-dark px-4">
            {/* SCANNER FRAME */}
            <div className="relative w-full max-w-md border border-brand-accent/20 bg-brand-dark/80 p-8 backdrop-blur-xl">
                {/* Decorative Corners */}
                <div className="absolute -top-1 -left-1 h-4 w-4 border-t-2 border-l-2 border-brand-accent" />
                <div className="absolute -bottom-1 -right-1 h-4 w-4 border-b-2 border-r-2 border-brand-accent" />

                <div className="mb-8 text-center">
                    <h2 className="font-mono text-xl font-bold tracking-[0.2em] text-brand-accent">
                        PROJECT FAUST
                    </h2>
                    <p className="mt-2 font-mono text-xs text-slate-500 uppercase tracking-widest">
                        System // Unauthorized access is logged
                    </p>
                </div>

                <form onSubmit={handleIdentification} className="space-y-6">
                    <div className="space-y-2">
                        <label className="block font-mono text-[10px] uppercase tracking-tighter text-slate-400">
                            Agent Identity (Email)
                        </label>
                        <input
                            type="email"
                            className="w-full border-b border-brand-accent/30 bg-transparent px-2 py-2 font-mono text-brand-accent outline-none transition-colors focus:border-brand-accent"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="agent@faust.int"
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="block font-mono text-[10px] uppercase tracking-tighter text-slate-400">
                            Authorization Key
                        </label>
                        <input
                            type="password"
                            className="w-full border-b border-brand-accent/30 bg-transparent px-2 py-2 font-mono text-brand-accent outline-none transition-colors focus:border-brand-accent"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    {/* NEW: inline error display instead of alert() */}
                    {error && (
                        <p className="font-mono text-[10px] uppercase tracking-widest text-red-500">
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={isProcessing}
                        className="group relative w-full overflow-hidden border border-brand-accent/50 bg-brand-accent/10 py-3 font-mono text-xs font-bold uppercase tracking-[0.2em] text-brand-accent transition-all hover:bg-brand-accent hover:text-black disabled:opacity-50"
                    >
                        {isProcessing ? (
                            <span className="flex items-center justify-center gap-2">
                                <span className="h-2 w-2 animate-ping rounded-full bg-brand-accent" />
                                Scanning...
                            </span>
                        ) : (
                            'Initiate Login'
                        )}
                    </button>
                </form>

                {/* Status bar at the bottom of the box */}
                <div className="mt-8 flex justify-between font-mono text-[8px] text-slate-600 uppercase">
                    <span>Enc: RSA_4096</span>
                    <span>Node: PRG_SEC_01</span>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;