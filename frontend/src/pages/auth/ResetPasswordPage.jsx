import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { KeyRound } from 'lucide-react';
import toast from 'react-hot-toast';
import authService from '../../services/authService';

const inputClass =
  'mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500';

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);

  if (!token) {
    return (
      <div className="text-center space-y-4">
        <h2 className="text-xl font-semibold text-gray-900">Enlace no válido</h2>
        <p className="text-sm text-gray-600">
          El enlace de recuperación está incompleto o expiró. Solicita uno nuevo.
        </p>
        <Link
          to="/forgot-password"
          className="inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          Solicitar otro enlace
        </Link>
      </div>
    );
  }

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (form.newPassword.length < 8) {
      toast.error('La contraseña debe tener al menos 8 caracteres');
      return;
    }
    if (!/(?=.*[A-Za-z])(?=.*\d)/.test(form.newPassword)) {
      toast.error('La contraseña debe incluir letras y números');
      return;
    }
    if (form.newPassword !== form.confirmPassword) {
      toast.error('Las contraseñas no coinciden');
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword({ token, newPassword: form.newPassword });
      toast.success('Contraseña actualizada. Inicia sesión de nuevo.');
      navigate('/login');
    } catch (error) {
      toast.error(
        error.response?.data?.message || 'No se pudo restablecer la contraseña. Solicita otro enlace.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex items-start gap-3">
        <KeyRound className="w-5 h-5 text-gray-400 mt-0.5" />
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Nueva contraseña</h2>
          <p className="text-sm text-gray-500 mt-1">
            Mínimo 8 caracteres, con al menos una letra y un número.
          </p>
        </div>
      </div>

      <div>
        <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
          Nueva contraseña
        </label>
        <input
          id="newPassword"
          type="password"
          autoComplete="new-password"
          value={form.newPassword}
          onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
          minLength={8}
          required
          className={inputClass}
        />
      </div>

      <div>
        <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
          Confirmar contraseña
        </label>
        <input
          id="confirmPassword"
          type="password"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
          minLength={8}
          required
          className={inputClass}
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full flex justify-center items-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
      >
        <KeyRound className="w-4 h-4 mr-2" />
        {loading ? 'Guardando...' : 'Restablecer contraseña'}
      </button>

      <p className="text-center">
        <Link to="/login" className="text-sm font-medium text-blue-600 hover:text-blue-700">
          Volver a iniciar sesión
        </Link>
      </p>
    </form>
  );
};

export default ResetPasswordPage;
