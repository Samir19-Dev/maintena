import { Outlet, Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import {
  LogOut,
  LayoutDashboard,
  FolderTree,
  MapPin,
  Box,
  Users,
  Shield,
  Wrench,
  ClipboardList,
  ScrollText,
  UserCircle,
} from 'lucide-react';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/categories', label: 'Categorías', icon: FolderTree },
  { to: '/locations', label: 'Ubicaciones', icon: MapPin },
  { to: '/assets', label: 'Activos', icon: Box },
  { to: '/maintenance/orders', label: 'Órdenes', icon: Wrench },
  { to: '/maintenance/reports', label: 'Reportes', icon: ClipboardList },
  { to: '/users', label: 'Usuarios', icon: Users, roles: ['ADMIN'] },
  { to: '/roles', label: 'Roles', icon: Shield, roles: ['ADMIN'] },
  { to: '/audit', label: 'Auditoría', icon: ScrollText, roles: ['ADMIN'] },
];

const linkClass = ({ isActive }) =>
  `flex items-center whitespace-nowrap px-3 py-2 text-sm font-medium rounded-md ${
    isActive
      ? 'bg-blue-50 text-blue-700'
      : 'text-gray-700 hover:text-gray-900 hover:bg-gray-100'
  }`;

const MainLayout = () => {
  const { logout, user } = useAuthStore();
  const navigate = useNavigate();

  const navItems = NAV_ITEMS.filter(({ roles }) => !roles || roles.includes(user?.rol));

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-5 flex-1 min-w-0">
              <h1 className="text-xl font-bold text-gray-900 shrink-0">Maintena</h1>
              <nav className="hidden xl:flex items-center gap-1 min-w-0 overflow-x-auto">
                {navItems.map(({ to, label, icon: Icon }) => (
                  <NavLink key={to} to={to} className={linkClass}>
                    <Icon className="w-4 h-4 mr-2" />
                    {label}
                  </NavLink>
                ))}
              </nav>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              {user && (
                <Link
                  to="/profile"
                  title="Mi perfil"
                  aria-label="Mi perfil"
                  className="flex items-center gap-2 px-2 py-2 text-sm text-gray-600 rounded-md hover:text-blue-700 hover:bg-gray-100"
                >
                  <UserCircle className="w-5 h-5" />
                  <span className="hidden lg:inline">
                    {user.nombre} · {user.rol}
                  </span>
                </Link>
              )}
              <button
                onClick={handleLogout}
                className="flex items-center px-3 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-md"
              >
                <LogOut className="w-4 h-4 mr-2" />
                Salir
              </button>
            </div>
          </div>
          <nav className="xl:hidden flex items-center gap-1 overflow-x-auto pb-2">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} className={linkClass}>
                <Icon className="w-4 h-4 mr-2" />
                {label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
};

export default MainLayout;
