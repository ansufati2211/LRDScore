import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface Props {
  children: React.ReactNode;
  roles?: string[];
}

export default function PrivateRoute({ children, roles }: Props) {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated()) return <Navigate to="/login" replace />;

  if (roles && user && !roles.includes(user.rol)) {
    // Redirige al área correcta según su rol
    if (user.rol === 'ROLE_COCINA') return <Navigate to="/kds" replace />;
    return <Navigate to="/mozo" replace />;
  }

  return <>{children}</>;
}
