import axios from 'axios';

const api = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

// NEW: Request interceptor — attaches the JWT token to every outgoing request.
//
// Reads directly from localStorage rather than importing AuthContext to avoid
// a circular dependency (AuthContext doesn't import axios, axios shouldn't
// import AuthContext). The storage key 'faust_token' must match TOKEN_KEY
// in AuthContext.tsx exactly.
//
// Without this, every protected endpoint (anyRequest().authenticated() in
// SecurityConfig) rejects the request as anonymous -> 403.
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('faust_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// NEW: Response interceptor — handles token expiry / invalidation globally.
//
// If the backend returns 401 (BadCredentialsException, expired JWT, or
// JwtAuthFilter rejecting an invalid token), clear the stored session and
// redirect to /login. This prevents the SPA from being stuck in a state
// where every request silently fails with a stale token.
//
// 403 is NOT handled here — 403 means "authenticated but insufficient role"
// (a real authorization failure), which the UI should surface as an error,
// not treat as a logout trigger.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('faust_session');
            localStorage.removeItem('faust_token');

            // Avoid redirect loop if already on the login page
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default api;