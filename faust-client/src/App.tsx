import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { useParams } from 'react-router-dom';

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
import LocationPage from './pages/LocationPage'; 
import LocationDetailPage from './pages/LocationDetailPage'; 

import GlobalSearchTerminal from './pages/GlobalSearchTerminal';
import SystemProtocols from './pages/SystemProtocols';

import IntelligenceTerminal from './components/ai/IntelligenceTerminal';

// --- HELPER WRAPPER ---
// Vytáhne 'id' z URL (/intelligence/123) a předá ho jako subjectId do HUD komponenty
const IntelligencePageWrapper = () => {
  const { id } = useParams();
  // ensure subjectId is always a string to satisfy prop types
  return <IntelligenceTerminal subjectId={id ?? ''} />;
};

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

        {/* NAVIGATION */}
        {isAuthenticated && <Sidebar />}

        <main className="relative flex-1 overflow-hidden">
          {/* UI CORNERS */}
          {isAuthenticated && (
            <>
              <div className="absolute top-0 left-0 w-4 h-4 border-t border-l border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute top-0 right-0 w-4 h-4 border-t border-r border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute bottom-0 left-0 w-4 h-4 border-b border-l border-brand-accent/30 z-20 pointer-events-none" />
              <div className="absolute bottom-0 right-0 w-4 h-4 border-b border-r border-brand-accent/30 z-20 pointer-events-none" />
            </>
          )}

          <Routes>
            {/* AUTH */}
            <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} />
            <Route path="/" element={isAuthenticated ? <HomePage /> : <Navigate to="/login" />} />

            {/* SYSTEM TERMINALS */}
            <Route path="/terminal" element={isAuthenticated ? <GlobalSearchTerminal /> : <Navigate to="/login" />} />
            <Route path="/protocols" element={isAuthenticated ? <SystemProtocols /> : <Navigate to="/login" />} />

            {/* PERSONNEL & INTELLIGENCE HUD */}
            <Route path="/personnel" element={isAuthenticated ? <SubjectRegistry /> : <Navigate to="/login" />} />
            <Route path="/personnel/:id" element={isAuthenticated ? <PersonDossier /> : <Navigate to="/login" />} />
            <Route path="/intelligence/:id" element={isAuthenticated ? <IntelligencePageWrapper /> : <Navigate to="/login" />} />

            {/* INSTITUTIONS */}
            <Route path="/institutions" element={isAuthenticated ? <InstitutionsPage /> : <Navigate to="/login" />} />
            <Route path="/institutions/:id" element={isAuthenticated ? <InstitutionDetail /> : <Navigate to="/login" />} />

            {/* LOCATIONS */}
            <Route path="/locations" element={isAuthenticated ? <LocationPage /> : <Navigate to="/login" />} />
            <Route path="/locations/:id" element={isAuthenticated ? <LocationDetailPage /> : <Navigate to="/login" />} />

            {/* OCCUPATIONS & HIERARCHY */}
            <Route path="/occupations" element={isAuthenticated ? <OccupationsPage /> : <Navigate to="/login" />} />
            <Route path="/occupations/:id" element={isAuthenticated ? <OccupationDetail /> : <Navigate to="/login" />} />
            <Route path="/hierarchy" element={isAuthenticated ? <HierarchyPage /> : <Navigate to="/login" />} />
            
            {/* AGENTS & PROFILE */}
            <Route path="/archive" element={isAuthenticated ? <ArchivePage /> : <Navigate to="/login" />} />
            <Route path="/agents/:id" element={isAuthenticated ? <AgentProfilePage /> : <Navigate to="/login" />} />
            <Route path="/profile" element={isAuthenticated ? <UserProfilePage /> : <Navigate to="/login" />} />
            <Route path="/onboard" element={isAuthenticated && user?.admin ? <OnboardingPage /> : <Navigate to="/" />} />

            {/* 404 - SECTOR NOT FOUND */}
            <Route path="*" element={
              <div className="flex h-full items-center justify-center p-10 bg-black/40 backdrop-blur-sm">
                <div className="border border-red-500/20 bg-red-500/5 p-12 text-center animate-in zoom-in duration-300 shadow-[0_0_50px_rgba(239,68,68,0.1)]">
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