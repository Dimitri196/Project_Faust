import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import HomePage from './pages/HomePage';
import PeoplePage from './pages/PeoplePage';
import InstitutionsPage from './pages/InstitutionsPage';
import InstitutionDetail from './pages/InstitutionDetail'; // NOVÝ IMPORT
import HierarchyPage from './pages/HierarchyPage';
import LoginPage from './pages/LoginPage';
import { useAuth } from './context/AuthContext';

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <BrowserRouter>
      <div className="relative flex h-screen w-full bg-brand-dark overflow-hidden text-slate-200">
        
        {/* VISUAL OVERLAYS */}
        <div className="pointer-events-none absolute inset-0 z-50 opacity-[0.03] mix-blend-overlay bg-[url('https://www.transparenttextures.com/patterns/stardust.png')]" />
        <div className="pointer-events-none absolute inset-0 z-50 opacity-10 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.1)_50%),linear-gradient(90deg,rgba(255,0,0,0.03),rgba(0,255,0,0.01),rgba(0,0,255,0.03))] bg-[length:100%_3px,3px_100%]" />

        {/* NAVIGATION: Sidebar */}
        {isAuthenticated && <Sidebar />}
        
        {/* OPERATIONAL SPACE */}
        <main className="relative flex-1 overflow-hidden">
          {/* Decorative Corner Crosshairs */}
          {isAuthenticated && (
            <>
              <div className="absolute top-0 left-0 w-4 h-4 border-t border-l border-brand-accent/30 z-20" />
              <div className="absolute top-0 right-0 w-4 h-4 border-t border-r border-brand-accent/30 z-20" />
              <div className="absolute bottom-0 left-0 w-4 h-4 border-b border-l border-brand-accent/30 z-20" />
              <div className="absolute bottom-0 right-0 w-4 h-4 border-b border-r border-brand-accent/30 z-20" />
            </>
          )}

          <Routes>
            {/* AUTHENTICATION MODULE */}
            <Route 
              path="/login" 
              element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} 
            />

            {/* PROTECTED MODULES */}
            <Route 
              path="/" 
              element={isAuthenticated ? <HomePage /> : <Navigate to="/login" />} 
            />
            
            <Route 
              path="/people" 
              element={isAuthenticated ? <PeoplePage /> : <Navigate to="/login" />} 
            />

            {/* INSTITUTION MODULES */}
            <Route 
              path="/institutions" 
              element={isAuthenticated ? <InstitutionsPage /> : <Navigate to="/login" />} 
            />
            
            {/* DYNAMICKÁ ROUTA PRO DETAIL INSTITUCE */}
            <Route 
              path="/institutions/:id" 
              element={isAuthenticated ? <InstitutionDetail /> : <Navigate to="/login" />} 
            />

            <Route 
              path="/hierarchy" 
              element={isAuthenticated ? <HierarchyPage /> : <Navigate to="/login" />} 
            />
            
            {/* SYSTEM FALLBACK (404) */}
            <Route path="*" element={
              <div className="flex h-full items-center justify-center p-10">
                <div className="border border-red-500/20 bg-red-500/5 p-12 text-center backdrop-blur-md">
                  <h1 className="mb-2 font-mono text-4xl font-black text-red-600 uppercase tracking-tighter">Error_404</h1>
                  <p className="font-mono text-xs text-red-400/60 uppercase tracking-[0.3em]">Sector_Not_Found // Structure_Corrupted</p>
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