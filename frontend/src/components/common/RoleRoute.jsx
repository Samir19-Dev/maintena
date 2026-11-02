import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const RoleRoute = ({ roles = [] }) => {
  const { user } = useAuthStore();

  if (roles.length > 0 && !roles.includes(user?.rol)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
};

export default RoleRoute;
