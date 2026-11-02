import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, KeyRound, Mail, Phone, Save, UserCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuthStore } from '../../store/authStore';
import authService from '../../services/authService';

const ProfilePage = () => {
  const navigate = useNavigate();
  const { updateProfile, changePassword, logout } = useAuthStore();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [changing, setChanging] = useState(false);
  const [form, setForm] = useState({ nombre: '', apellido: '', telefono: '' });
  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const response = await authService.getProfile();
        setProfile(response.data);
        setForm({
          nombre: response.data.nombre || '',
          apellido: response.data.apellido || '',
          telefono: response.data.telefono || '',
        });
      } catch (error) {
        toast.error(error.response?.data?.message || 'No se pudo cargar el perfil');
      } finally {
        setLoading(false);
      }
    };
    loadProfile();
  }, []);

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    if (!form.nombre.trim() || !form.apellido.trim()) {
      toast.error('Nombre y apellido son obligatorios');
      return;
    }
    setSaving(true);
    try {
      const updated = await updateProfile({
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        telefono: form.telefono.trim(),
      });
      setProfile(updated);
      toast.success('Perfil actualizado');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo actualizar el perfil');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    if (!passwords.currentPassword || !passwords.newPassword) {
      toast.error('Completa los campos de contraseña');
      return;
    }
    if (passwords.newPassword.length < 8) {
      toast.error('La nueva contraseña debe tener al menos 8 caracteres');
      return;
    }
    if (!/(?=.*[A-Za-z])(?=.*\d)/.test(passwords.newPassword)) {
      toast.error('La nueva contraseña debe incluir letras y números');
      return;
    }
    if (passwords.newPassword !== passwords.confirmPassword) {
      toast.error('Las contraseñas no coinciden');
      return;
    }

    setChanging(true);
    try {
      await changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      toast.success('Contraseña actualizada. Inicia sesión de nuevo.');
      await logout();
      navigate('/login');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo cambiar la contraseña');
    } finally {
      setChanging(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-gray-500">Cargando perfil...</div>;
  }

  const initials = `${profile?.nombre?.[0] || ''}${profile?.apellido?.[0] || ''}`.toUpperCase();
  const memberSince = profile?.fechaCreacion
    ? new Date(profile.fechaCreacion).toLocaleDateString('es-CO', { dateStyle: 'long' })
    : '—';

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="bg-white shadow rounded-lg p-6 flex flex-col sm:flex-row sm:items-center gap-5">
        <div className="w-16 h-16 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-xl font-bold shrink-0">
          {initials || <UserCircle className="w-8 h-8" />}
        </div>
        <div className="min-w-0">
          <h1 className="text-2xl font-bold text-gray-900">
            {profile?.nombre} {profile?.apellido}
          </h1>
          <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500">
            <span className="inline-flex items-center">
              <Mail className="w-4 h-4 mr-1.5" />
              {profile?.email}
            </span>
            <span className="inline-flex items-center">
              <Phone className="w-4 h-4 mr-1.5" />
              {profile?.telefono || 'Sin teléfono'}
            </span>
            <span className="inline-flex items-center">
              <Calendar className="w-4 h-4 mr-1.5" />
              Desde {memberSince}
            </span>
          </div>
        </div>
        <span className="sm:ml-auto px-3 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">
          {profile?.rol}
        </span>
      </div>

      <form onSubmit={handleProfileSubmit} className="bg-white shadow rounded-lg p-6 space-y-4">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">Datos personales</h2>
          <p className="text-sm text-gray-500 mt-1">
            El email y el rol solo pueden modificarlos un administrador.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nombre</label>
            <input
              type="text"
              value={form.nombre}
              onChange={(event) => setForm({ ...form, nombre: event.target.value })}
              maxLength={100}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Apellido</label>
            <input
              type="text"
              value={form.apellido}
              onChange={(event) => setForm({ ...form, apellido: event.target.value })}
              maxLength={100}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
            <input
              type="text"
              value={form.telefono}
              onChange={(event) => setForm({ ...form, telefono: event.target.value })}
              maxLength={20}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={profile?.email || ''}
              disabled
              className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-gray-50 text-gray-500"
            />
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="flex items-center px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-60"
          >
            <Save className="w-4 h-4 mr-2" />
            {saving ? 'Guardando...' : 'Guardar cambios'}
          </button>
        </div>
      </form>

      <form onSubmit={handlePasswordSubmit} className="bg-white shadow rounded-lg p-6 space-y-4">
        <div className="flex items-start gap-3">
          <KeyRound className="w-5 h-5 text-gray-400 mt-0.5" />
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Cambiar contraseña</h2>
            <p className="text-sm text-gray-500 mt-1">
              Al cambiarla se cerrarán todas tus sesiones activas.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña actual</label>
            <input
              type="password"
              value={passwords.currentPassword}
              onChange={(event) =>
                setPasswords({ ...passwords, currentPassword: event.target.value })
              }
              autoComplete="current-password"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nueva contraseña</label>
            <input
              type="password"
              value={passwords.newPassword}
              onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })}
              autoComplete="new-password"
              minLength={8}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Confirmar nueva contraseña
            </label>
            <input
              type="password"
              value={passwords.confirmPassword}
              onChange={(event) =>
                setPasswords({ ...passwords, confirmPassword: event.target.value })
              }
              autoComplete="new-password"
              minLength={8}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>
        <p className="text-xs text-gray-500">
          Mínimo 8 caracteres, con al menos una letra y un número.
        </p>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={changing}
            className="flex items-center px-4 py-2 bg-gray-900 text-white text-sm rounded-md hover:bg-gray-800 disabled:opacity-60"
          >
            <KeyRound className="w-4 h-4 mr-2" />
            {changing ? 'Actualizando...' : 'Cambiar contraseña'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ProfilePage;
