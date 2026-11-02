import { useEffect, useState } from 'react';
import { Search, RefreshCw, ShieldCheck, FilterX } from 'lucide-react';
import toast from 'react-hot-toast';
import auditService from '../../services/auditService';

const ACCIONES = [
  'LOGIN',
  'LOGIN_FAILED',
  'LOGOUT',
  'PASSWORD_CHANGE',
  'PASSWORD_RESET_REQUEST',
  'PASSWORD_RESET',
  'CREATE',
  'UPDATE',
  'DELETE',
  'ASSIGN',
  'STATUS_CHANGE',
  'APPROVE',
  'REJECT',
  'EXPORT',
  'COMMENT',
  'ATTACH',
];

const accionLabels = {
  LOGIN: 'Inicio de sesión',
  LOGIN_FAILED: 'Login fallido',
  LOGOUT: 'Cierre de sesión',
  PASSWORD_CHANGE: 'Cambio de contraseña',
  PASSWORD_RESET_REQUEST: 'Solicitud de recuperación',
  PASSWORD_RESET: 'Contraseña restablecida',
  CREATE: 'Creación',
  UPDATE: 'Actualización',
  DELETE: 'Eliminación',
  ASSIGN: 'Asignación',
  STATUS_CHANGE: 'Cambio de estado',
  APPROVE: 'Aprobación',
  REJECT: 'Rechazo',
  EXPORT: 'Exportación',
  COMMENT: 'Comentario',
  ATTACH: 'Adjunto',
};

const accionStyles = {
  LOGIN: 'bg-green-100 text-green-800',
  LOGIN_FAILED: 'bg-red-100 text-red-800',
  LOGOUT: 'bg-slate-100 text-slate-700',
  PASSWORD_CHANGE: 'bg-purple-100 text-purple-800',
  PASSWORD_RESET_REQUEST: 'bg-indigo-100 text-indigo-800',
  PASSWORD_RESET: 'bg-fuchsia-100 text-fuchsia-800',
  CREATE: 'bg-blue-100 text-blue-800',
  UPDATE: 'bg-amber-100 text-amber-800',
  DELETE: 'bg-red-100 text-red-800',
  ASSIGN: 'bg-indigo-100 text-indigo-800',
  STATUS_CHANGE: 'bg-slate-100 text-slate-700',
  APPROVE: 'bg-emerald-100 text-emerald-800',
  REJECT: 'bg-rose-100 text-rose-800',
  EXPORT: 'bg-gray-100 text-gray-700',
  COMMENT: 'bg-cyan-100 text-cyan-800',
  ATTACH: 'bg-teal-100 text-teal-800',
};

const entidadLabels = {
  AUTH: 'Autenticación',
  CATEGORIA: 'Categoría',
  UBICACION: 'Ubicación',
  ACTIVO: 'Activo',
  MANTENIMIENTO_ORDEN: 'Orden de mantenimiento',
  MANTENIMIENTO_REPORTE: 'Reporte de falla',
  USUARIO: 'Usuario',
  ROL: 'Rol',
  REPORTE: 'Exportación',
};

const formatDateTime = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  return date.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'medium' });
};

const emptyFilters = { entidad: '', accion: '', email: '', desde: '', hasta: '' };
const PAGE_SIZE = 20;

const AuditPage = () => {
  const [logs, setLogs] = useState([]);
  const [entidades, setEntidades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [forbidden, setForbidden] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState(emptyFilters);
  const [applied, setApplied] = useState(emptyFilters);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const params = { page, size: PAGE_SIZE };
      if (applied.entidad) params.entidad = applied.entidad;
      if (applied.accion) params.accion = applied.accion;
      if (applied.email) params.email = applied.email;
      if (applied.desde) params.desde = `${applied.desde}T00:00:00`;
      if (applied.hasta) params.hasta = `${applied.hasta}T23:59:59`;

      const response = await auditService.getAll(params);
      setLogs(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
      setForbidden(false);
    } catch (error) {
      if (error.response?.status === 403) {
        setForbidden(true);
      } else {
        toast.error('Error al cargar la bitácora');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, applied]);

  useEffect(() => {
    const loadEntidades = async () => {
      try {
        const response = await auditService.getEntidades();
        setEntidades(response.data);
      } catch {
        setEntidades([]);
      }
    };
    loadEntidades();
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    setApplied({ ...filters });
  };

  const handleClear = () => {
    setFilters(emptyFilters);
    setApplied(emptyFilters);
    setPage(0);
  };

  if (forbidden) {
    return (
      <div className="bg-white rounded-lg shadow p-8 text-center">
        <ShieldCheck className="w-10 h-10 text-gray-400 mx-auto mb-3" />
        <h1 className="text-lg font-semibold text-gray-900 mb-1">Acceso restringido</h1>
        <p className="text-sm text-gray-500">La bitácora de auditoría solo está disponible para administradores.</p>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Auditoría</h1>
          <p className="text-sm text-gray-500 mt-1">
            Registro de las acciones realizadas sobre el sistema.
          </p>
        </div>
        <button
          onClick={fetchLogs}
          disabled={loading}
          className="flex items-center px-3 py-2 text-sm bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-60"
        >
          <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
          Actualizar
        </button>
      </div>

      <form onSubmit={handleSearch} className="bg-white shadow rounded-lg p-4 flex flex-wrap gap-3 items-end">
        <div className="flex flex-col">
          <label className="text-xs text-gray-500 mb-1">Entidad</label>
          <select
            value={filters.entidad}
            onChange={(e) => setFilters({ ...filters, entidad: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm min-w-[180px]"
          >
            <option value="">Todas</option>
            {entidades.map((entidad) => (
              <option key={entidad} value={entidad}>
                {entidadLabels[entidad] || entidad}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col">
          <label className="text-xs text-gray-500 mb-1">Acción</label>
          <select
            value={filters.accion}
            onChange={(e) => setFilters({ ...filters, accion: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm min-w-[160px]"
          >
            <option value="">Todas</option>
            {ACCIONES.map((accion) => (
              <option key={accion} value={accion}>
                {accionLabels[accion]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col flex-1 min-w-[200px]">
          <label className="text-xs text-gray-500 mb-1">Usuario (email)</label>
          <input
            type="text"
            value={filters.email}
            onChange={(e) => setFilters({ ...filters, email: e.target.value })}
            placeholder="admin@maintena.com"
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          />
        </div>

        <div className="flex flex-col">
          <label className="text-xs text-gray-500 mb-1">Desde</label>
          <input
            type="date"
            value={filters.desde}
            onChange={(e) => setFilters({ ...filters, desde: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          />
        </div>

        <div className="flex flex-col">
          <label className="text-xs text-gray-500 mb-1">Hasta</label>
          <input
            type="date"
            value={filters.hasta}
            onChange={(e) => setFilters({ ...filters, hasta: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          />
        </div>

        <button
          type="submit"
          className="flex items-center px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
        >
          <Search className="w-4 h-4 mr-2" />
          Buscar
        </button>
        <button
          type="button"
          onClick={handleClear}
          className="flex items-center px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
        >
          <FilterX className="w-4 h-4 mr-2" />
          Limpiar
        </button>
      </form>

      <div className="bg-white shadow rounded-lg overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Usuario</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Acción</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entidad</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Detalle</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">IP</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {loading && (
              <tr>
                <td colSpan="6" className="px-4 py-6 text-center text-sm text-gray-500">
                  Cargando bitácora...
                </td>
              </tr>
            )}
            {!loading && logs.length === 0 && (
              <tr>
                <td colSpan="6" className="px-4 py-6 text-center text-sm text-gray-500">
                  No hay registros con los filtros seleccionados
                </td>
              </tr>
            )}
            {!loading &&
              logs.map((log) => (
                <tr key={log.id}>
                  <td className="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">
                    {formatDateTime(log.fechaCreacion)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-700">{log.usuarioEmail || '—'}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        accionStyles[log.accion] || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {accionLabels[log.accion] || log.accion}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">
                    {entidadLabels[log.entidad] || log.entidad}
                    {log.entidadId != null && <span className="text-gray-400"> #{log.entidadId}</span>}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500 max-w-md">{log.detalle || '—'}</td>
                  <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">{log.ip || '—'}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-gray-600">
        <span>
          {totalElements} registro{totalElements === 1 ? '' : 's'}
          {totalPages > 0 && ` · página ${page + 1} de ${totalPages}`}
        </span>
        <div className="flex gap-2">
          <button
            onClick={() => setPage((current) => Math.max(0, current - 1))}
            disabled={page === 0 || loading}
            className="px-3 py-1.5 border border-gray-300 rounded-md bg-white hover:bg-gray-50 disabled:opacity-50"
          >
            Anterior
          </button>
          <button
            onClick={() => setPage((current) => current + 1)}
            disabled={page + 1 >= totalPages || loading}
            className="px-3 py-1.5 border border-gray-300 rounded-md bg-white hover:bg-gray-50 disabled:opacity-50"
          >
            Siguiente
          </button>
        </div>
      </div>
    </div>
  );
};

export default AuditPage;
