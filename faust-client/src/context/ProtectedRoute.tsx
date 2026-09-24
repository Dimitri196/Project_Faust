import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Fingerprint } from 'lucide-react';

interface ProtectedRouteProps {
  children: React.ReactNode;
  adminOnly?: boolean;
}

const ProtectedRoute = ({ children, adminOnly = false }: ProtectedRouteProps) => {
  const { isAuthenticated, user, isLoading } = useAuth();
  const location = useLocation();

  // Zobrazíme loading, dokud nevíme, jestli je session v localStorage platná
  if (isLoading) {
    return (
      <div className="h-screen bg-[#0a0c10] flex flex-col items-center justify-center font-mono text-blue-500/50 uppercase tracking-[0.4em]">
        <Fingerprint size={48} className="animate-pulse mb-4 text-blue-600" />
        Authenticating_Uplink...
      </div>
    );
  }

  if (!isAuthenticated) {
    // Pokud není přihlášen, pošleme ho na login a zapamatujeme si, kam chtěl jít
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (adminOnly && !user?.admin) {
    // Pokud chce na admin stránku a není admin, hodíme ho na dashboard (nebo 403)
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;