import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

// --- COMPONENTS ---
import Sidebar from './components/Sidebar';

// --- PAGES ---
import HomePage from './pages/HomePage';
import SubjectRegistry from './pages/SubjectRegistry';
import PersonDossier from './pages/PersonDossier';
import AgentProfilePage from './pages/AgentProfilePage';
import InstitutionsPage from './pages/InstitutionsPage';
import InstitutionDetail from './pages/InstitutionDetail';
import OccupationsPage from './pages/OccupationPage';
import OccupationDetail from './pages/OccupationDetail';
import HierarchyPage from './pages/HierarchyPage';
import LoginPage from './pages/LoginPage';
import UserProfilePage from './pages/UserProfilePage';
import ArchivePage from './pages/ArchivePage';
import OnboardingPage from './pages/OnboardingPage';

// --- GEOSPATIAL PAGES ---
import LocationPage from './pages/LocationPage'; // Tento používá /api/v1/locations/search
import LocationDetailPage from './pages/LocationDetailPage'; // Tento používá /api/v1/locations/{id}

import GlobalSearchTerminal from './pages/GlobalSearchTerminal';
import SystemProtocols from './pages/SystemProtocols';

function App() {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="h-screen w-full bg-[#0a0c10] flex items-center justify-center font-mono text-[10px] text-blue-500 tracking-[0.5em] animate-pulse">
        ESTABLISHING_SECURE_UPLINK...
      </div>
    );
  }

  return (
    <BrowserRouter>
      <div className="relative flex h-screen w-full bg-brand-dark overflow-hidden text-slate-200 font-sans">

        {/* VISUAL OVERLAYS */}
        <div className="pointer-events-none absolute inset-0 z-50 opacity-[0.03] mix-blend-overlay bg-[url('https://www.transparenttextures.com/patterns/stardust.png')]" />
        <div className="pointer-events-none absolute inset-0 z-50 opacity-10 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.1)_50%),linear-gradient(90deg,rgba(255,0,0,0.03),rgba(0,255,0,0.01),rgba(0,0,255,0.03))] bg-[length:100%_3px,3px_100%]" />

        {isAuthenticated && <Sidebar />}

        <main className="relative flex-1 overflow-hidden">
          {isAuthenticated && (
            <>
              <div className="absolute top-0 left-0 w-4 h-4 border-t border-l border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute top-0 right-0 w-4 h-4 border-t border-r border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute bottom-0 left-0 w-4 h-4 border-b border-l border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute bottom-0 right-0 w-4 h-4 border-b border-r border-brand-accent/30 z-20 pointer-events-none" />
            </>
          )}

          <Routes>
            <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} />
            <Route path="/" element={isAuthenticated ? <HomePage /> : <Navigate to="/login" />} />

            {/* OMNI-SEARCH TERMINAL */}
            <Route path="/terminal" element={isAuthenticated ? <GlobalSearchTerminal /> : <Navigate to="/login" />} />
            <Route path="/protocols" element={isAuthenticated ? <SystemProtocols /> : <Navigate to="/login" />} />

            {/* PERSONÁL */}
            <Route path="/personnel" element={isAuthenticated ? <SubjectRegistry /> : <Navigate to="/login" />} />
            <Route path="/personnel/:id" element={isAuthenticated ? <PersonDossier /> : <Navigate to="/login" />} />

            {/* INSTITUCE */}
            <Route path="/institutions" element={isAuthenticated ? <InstitutionsPage /> : <Navigate to="/login" />} />
            <Route path="/institutions/:id" element={isAuthenticated ? <InstitutionDetail /> : <Navigate to="/login" />} />

            {/* LOKALITY (Zde byla chyba) */}
            {/* 1. Hlavní přehled (Registry), který volá tvůj /search endpoint */}
            <Route path="/locations" element={isAuthenticated ? <LocationPage /> : <Navigate to="/login" />} />

            {/* 2. Detail konkrétní lokace */}
            <Route path="/locations/:id" element={isAuthenticated ? <LocationDetailPage /> : <Navigate to="/login" />} />

            {/* OSTATNÍ */}
            <Route path="/occupations" element={isAuthenticated ? <OccupationsPage /> : <Navigate to="/login" />} />
            <Route path="/occupations/:id" element={isAuthenticated ? <OccupationDetail /> : <Navigate to="/login" />} />
            <Route path="/hierarchy" element={isAuthenticated ? <HierarchyPage /> : <Navigate to="/login" />} />
            <Route path="/archive" element={isAuthenticated ? <ArchivePage /> : <Navigate to="/login" />} />
            <Route path="/agents/:id" element={isAuthenticated ? <AgentProfilePage /> : <Navigate to="/login" />} />
            <Route path="/profile" element={isAuthenticated ? <UserProfilePage /> : <Navigate to="/login" />} />
            <Route path="/onboard" element={isAuthenticated && user?.isAdmin ? <OnboardingPage /> : <Navigate to="/" />} />

            {/* 404 */}
            <Route path="*" element={
              <div className="flex h-full items-center justify-center p-10 bg-black/40 backdrop-blur-sm">
                <div className="border border-red-500/20 bg-red-500/5 p-12 text-center animate-in zoom-in duration-300">
                  <div className="inline-block p-4 border border-red-500 mb-6 bg-red-500/10 text-red-600 font-black text-6xl font-mono uppercase tracking-tighter">
                    404
                  </div>
                  <h2 className="text-xl font-bold text-white uppercase mb-2">Sector_Not_Found</h2>
                  <p className="font-mono text-[10px] text-red-400/60 uppercase tracking-[0.3em]">
                    Structure_Corrupted // Unauthorized_Access_Detected
                  </p>
                  <button
                    onClick={() => window.location.href = '/'}
                    className="mt-8 px-6 py-2 border border-red-500/40 text-red-500 font-mono text-[10px] uppercase hover:bg-red-500 hover:text-white transition-all tracking-[0.2em]"
                  >
                    Return_to_Safe_Zone
                  </button>
                </div>
              </div>
            } />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;