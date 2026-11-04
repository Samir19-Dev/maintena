import { useState, useEffect } from 'react';
import maintenanceService from '../../services/maintenanceService';
import assetService from '../../services/assetService';
import { useAuthStore } from '../../store/authStore';
import { Plus, CheckCircle2, XCircle, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

const PRIORIDADES = ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'];

const estadoStyles = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  APROBADA: 'bg-blue-100 text-blue-800',
  RECHAZADA: 'bg-red-100 text-red-800',
  CONVERTIDA: 'bg-green-100 text-green-800',
};

const prioridadStyles = {
  BAJA: 'bg-gray-100 text-gray-700',
  MEDIA: 'bg-sky-100 text-sky-800',
  ALTA: 'bg-orange-100 text-orange-800',
  URGENTE: 'bg-red-100 text-red-800',
};

const ReportsPage = () => {
  const { user } = useAuthStore();
  const isManager = ['ADMIN', 'TECNICO', 'SUPERVISOR'].includes(user?.rol);

  const [reports, setReports] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ descripcion: '', activoId: '', prioridad: 'MEDIA' });

  const fetchData = async () => {
    try {
      const [reportsRes, assetsRes] = await Promise.all([
        maintenanceService.reports.getAll(),
        assetService.getAll(),
      ]);
      setReports(reportsRes.data);
      setAssets(assetsRes.data);
    } catch (error) {
      toast.error('Error al cargar reportes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await maintenanceService.reports.create({
        ...formData,
        activoId: Number(formData.activoId),
      });
      toast.success('Falla reportada correctamente');
      setShowModal(false);
      setFormData({ descripcion: '', activoId: '', prioridad: 'MEDIA' });
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al reportar la falla');
    }
  };

  const handleApprove = async (id) => {
    try {
      const res = await maintenanceService.reports.approve(id);
      toast.success(`Orden generada: ${res.data.ordenCodigo}`);
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al aprobar');
    }
  };

  const handleReject = async (id) => {
    try {
      await maintenanceService.reports.reject(id);
      toast.success('Reporte rechazado');
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al rechazar');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('¿Eliminar este reporte?')) {
      try {
        await maintenanceService.reports.delete(id);
        toast.success('Reporte eliminado');
        fetchData();
      } catch (error) {
        toast.error(error.response?.data?.message || 'Error al eliminar');
      }
    }
  };

  if (loading) return <div>Cargando...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reportes de Falla</h1>
          <p className="text-sm text-gray-500 mt-1">
            {isManager
              ? 'Aprueba un reporte para generar automáticamente una orden correctiva.'
              : 'Reporta fallas en los activos y sigue su estado.'}
          </p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          <Plus className="w-4 h-4 mr-2" />
          Reportar Falla
        </button>
      </div>

      <div className="bg-white shadow rounded-lg overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Activo</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Descripción</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Prioridad</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Solicitante</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Orden</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Acciones</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {reports.length === 0 && (
              <tr>
                <td colSpan="7" className="px-4 py-6 text-center text-sm text-gray-500">
                  No hay reportes de falla
                </td>
              </tr>
            )}
            {reports.map((report) => (
              <tr key={report.id}>
                <td className="px-4 py-3 text-sm text-gray-900">
                  {report.activoCodigo} — {report.activoNombre}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 max-w-xs">{report.descripcion}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${prioridadStyles[report.prioridad] || 'bg-gray-100 text-gray-700'}`}>
                    {report.prioridad}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${estadoStyles[report.estado] || 'bg-gray-100 text-gray-700'}`}>
                    {report.estado}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{report.solicitanteNombre}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{report.ordenCodigo || '—'}</td>
                <td className="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    {isManager && report.estado === 'PENDIENTE' && (
                      <>
                        <button
                          onClick={() => handleApprove(report.id)}
                          className="text-green-600 hover:text-green-900"
                          title="Aprobar y generar orden"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleReject(report.id)}
                          className="text-red-600 hover:text-red-900"
                          title="Rechazar"
                        >
                          <XCircle className="w-4 h-4" />
                        </button>
                      </>
                    )}
                    {user?.rol === 'ADMIN' && (
                      <button
                        onClick={() => handleDelete(report.id)}
                        className="text-red-600 hover:text-red-900"
                        title="Eliminar"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg">
            <h2 className="text-lg font-bold mb-4">Reportar Falla</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Activo</label>
                <select
                  value={formData.activoId}
                  onChange={(e) => setFormData({ ...formData, activoId: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  required
                >
                  <option value="">Seleccione...</option>
                  {assets.map((a) => (
                    <option key={a.id} value={a.id}>{a.codigo} - {a.nombre}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Descripción de la falla</label>
                <textarea
                  value={formData.descripcion}
                  onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  rows="3"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Prioridad</label>
                <select
                  value={formData.prioridad}
                  onChange={(e) => setFormData({ ...formData, prioridad: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                >
                  {PRIORIDADES.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <div className="flex justify-end space-x-3">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 border rounded-md">
                  Cancelar
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md">
                  Enviar Reporte
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportsPage;
