import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import AuthLayout from '../components/layout/AuthLayout';
import MainLayout from '../components/layout/MainLayout';
import ProtectedRoute from '../components/common/ProtectedRoute';
import RoleRoute from '../components/common/RoleRoute';
import LoginPage from '../pages/auth/LoginPage';
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '../pages/auth/ResetPasswordPage';
import DashboardPage from '../pages/dashboard/DashboardPage';
import CategoriesPage from '../pages/categories/CategoriesPage';
import LocationsPage from '../pages/locations/LocationsPage';
import AssetsPage from '../pages/assets/AssetsPage';
import OrdersPage from '../pages/maintenance/OrdersPage';
import ReportsPage from '../pages/maintenance/ReportsPage';
import UsersPage from '../pages/users/UsersPage';
import RolesPage from '../pages/roles/RolesPage';
import AuditPage from '../pages/audit/AuditPage';
import ProfilePage from '../pages/profile/ProfilePage';
import NotFoundPage from '../pages/notfound/NotFoundPage';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <Routes>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/categories" element={<CategoriesPage />} />
            <Route path="/locations" element={<LocationsPage />} />
            <Route path="/assets" element={<AssetsPage />} />
            <Route path="/maintenance/orders" element={<OrdersPage />} />
            <Route path="/maintenance/reports" element={<ReportsPage />} />
            <Route element={<RoleRoute roles={['ADMIN']} />}>
              <Route path="/users" element={<UsersPage />} />
              <Route path="/roles" element={<RolesPage />} />
              <Route path="/audit" element={<AuditPage />} />
            </Route>
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRoutes;
