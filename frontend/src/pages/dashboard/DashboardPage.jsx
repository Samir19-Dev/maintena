import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Wrench,
  Clock,
  CheckCircle2,
  Timer,
  Box,
  AlertTriangle,
  Download,
  FileText,
  TrendingUp,
} from 'lucide-react';
import toast from 'react-hot-toast';
import dashboardService from '../../services/dashboardService';
import reportService from '../../services/reportService';
import { useAuthStore } from '../../store/authStore';

const estadoStyles = {
  PENDIENTE: { badge: 'bg-yellow-100 text-yellow-800', bar: 'bg-yellow-500' },
  ASIGNADA: { badge: 'bg-blue-100 text-blue-800', bar: 'bg-blue-500' },
  EN_PROCESO: { badge: 'bg-indigo-100 text-indigo-800', bar: 'bg-indigo-500' },
  COMPLETADA: { badge: 'bg-green-100 text-green-800', bar: 'bg-green-500' },
  CANCELADA: { badge: 'bg-gray-200 text-gray-700', bar: 'bg-gray-400' },
};

const prioridadStyles = {
  BAJA: 'bg-gray-100 text-gray-700',
  MEDIA: 'bg-sky-100 text-sky-800',
  ALTA: 'bg-orange-100 text-orange-800',
  URGENTE: 'bg-red-100 text-red-800',
};

const KpiCard = ({ icon: Icon, label, value, hint, color }) => (
  <div className="bg-white rounded-lg shadow p-5 flex items-start justify-between">
    <div>
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
      {hint && <p className="text-xs text-gray-400 mt-1">{hint}</p>}
    </div>
    <div className={`p-3 rounded-lg ${color}`}>
      <Icon className="w-5 h-5" />
    </div>
  </div>
);

const DashboardPage = () => {
  const { user } = useAuthStore();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState('');

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        const response = await dashboardService.getSummary();
        setSummary(response.data);
      } catch (error) {
        toast.error('Error al cargar los indicadores');
      } finally {
        setLoading(false);
      }
    };
    fetchSummary();
  }, []);

  const handleExport = async (kind) => {
    setExporting(kind);
    try {
      if (kind === 'csv') {
        await reportService.ordersCsv();
      } else {
        await reportService.ordersPdf();
      }
      toast.success(kind === 'csv' ? 'CSV descargado' : 'PDF descargado');
    } catch (error) {
      toast.error('Error al generar el archivo');
    } finally {
      setExporting('');
    }
  };

  if (loading) {
    return <div className="text-sm text-gray-500">Cargando dashboard...</div>;
  }

  if (!summary) {
    return <div className="text-sm text-gray-500">No se pudieron cargar los indicadores.</div>;
  }

  const openOrders = summary.pendingOrders + summary.assignedOrders + summary.inProgressOrders;
  const maxStatus = Math.max(1, ...summary.ordersByStatus.map((item) => item.total));
  const maxTrend = Math.max(
    1,
    ...summary.monthlyTrend.flatMap((item) => [item.creadas, item.completadas])
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">
            {user ? `Hola ${user.nombre}, este es el resumen de mantenimiento.` : 'Resumen general de mantenimiento.'}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => handleExport('csv')}
            disabled={exporting !== ''}
            className="flex items-center px-3 py-2 text-sm bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-60"
          >
            <Download className="w-4 h-4 mr-2" />
            {exporting === 'csv' ? 'Generando...' : 'Exportar CSV'}
          </button>
          <button
            onClick={() => handleExport('pdf')}
            disabled={exporting !== ''}
            className="flex items-center px-3 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-60"
          >
            <FileText className="w-4 h-4 mr-2" />
            {exporting === 'pdf' ? 'Generando...' : 'Exportar PDF'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <KpiCard
          icon={Wrench}
          label="Órdenes totales"
          value={summary.totalOrders}
          hint={`${summary.createdThisMonth} creadas este mes`}
          color="bg-blue-50 text-blue-600"
        />
        <KpiCard
          icon={Clock}
          label="Órdenes abiertas"
          value={openOrders}
          hint={`${summary.pendingOrders} pendientes · ${summary.inProgressOrders} en proceso`}
          color="bg-amber-50 text-amber-600"
        />
        <KpiCard
          icon={CheckCircle2}
          label="Completadas"
          value={summary.completedOrders}
          hint={`${summary.completedThisMonth} este mes`}
          color="bg-green-50 text-green-600"
        />
        <KpiCard
          icon={Timer}
          label="Tiempo medio de resolución"
          value={summary.averageResolutionHours > 0 ? `${summary.averageResolutionHours} h` : '—'}
          hint="Órdenes completadas"
          color="bg-indigo-50 text-indigo-600"
        />
        <KpiCard
          icon={Box}
          label="Activos"
          value={summary.totalAssets}
          hint={`${summary.activeAssets} activos`}
          color="bg-slate-100 text-slate-600"
        />
        <KpiCard
          icon={AlertTriangle}
          label="Reportes pendientes"
          value={summary.pendingReports}
          hint={`${summary.totalReports} reportes en total`}
          color="bg-red-50 text-red-600"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-lg shadow p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-900">Órdenes por estado</h2>
            <Link to="/maintenance/orders" className="text-xs text-blue-600 hover:underline">
              Ver órdenes
            </Link>
          </div>
          <div className="space-y-3">
            {summary.ordersByStatus.map((item) => (
              <div key={item.label}>
                <div className="flex justify-between text-xs text-gray-600 mb-1">
                  <span>{item.label}</span>
                  <span className="font-semibold text-gray-900">{item.total}</span>
                </div>
                <div className="h-2 bg-gray-100 rounded">
                  <div
                    className={`h-2 rounded ${estadoStyles[item.label]?.bar || 'bg-gray-400'}`}
                    style={{ width: `${(item.total / maxStatus) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
          <div className="flex flex-wrap gap-2 mt-5 pt-4 border-t">
            {summary.ordersByPriority.map((item) => (
              <span
                key={item.label}
                className={`px-2 py-1 text-xs font-semibold rounded-full ${prioridadStyles[item.label] || 'bg-gray-100 text-gray-700'}`}
              >
                {item.label}: {item.total}
              </span>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-900">Tendencia (últimos 6 meses)</h2>
            <div className="flex items-center gap-3 text-xs text-gray-500">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-blue-500 inline-block" /> Creadas
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-green-500 inline-block" /> Completadas
              </span>
            </div>
          </div>
          <div className="flex items-end justify-between gap-3 h-40">
            {summary.monthlyTrend.map((item) => (
              <div key={item.mes} className="flex-1 flex flex-col items-center gap-2">
                <div className="flex items-end gap-1 h-32">
                  <div
                    className="w-3 bg-blue-500 rounded-t"
                    style={{ height: `${(item.creadas / maxTrend) * 100}%` }}
                    title={`Creadas: ${item.creadas}`}
                  />
                  <div
                    className="w-3 bg-green-500 rounded-t"
                    style={{ height: `${(item.completadas / maxTrend) * 100}%` }}
                    title={`Completadas: ${item.completadas}`}
                  />
                </div>
                <span className="text-[10px] text-gray-500 text-center leading-tight">{item.mes}</span>
              </div>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-3 mt-5 pt-4 border-t text-xs text-gray-600">
            {summary.ordersByType.map((item) => (
              <div key={item.label} className="flex justify-between">
                <span>{item.label}</span>
                <span className="font-semibold text-gray-900">{item.total}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg shadow lg:col-span-2 overflow-x-auto">
          <div className="flex items-center justify-between p-5 pb-3">
            <h2 className="text-sm font-semibold text-gray-900">Órdenes recientes</h2>
            <Link to="/maintenance/orders" className="text-xs text-blue-600 hover:underline">
              Ver todas
            </Link>
          </div>
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Código</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Título</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Técnico</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {summary.recentOrders.length === 0 && (
                <tr>
                  <td colSpan="4" className="px-4 py-6 text-center text-sm text-gray-500">
                    Aún no hay órdenes registradas
                  </td>
                </tr>
              )}
              {summary.recentOrders.map((order) => (
                <tr key={order.id}>
                  <td className="px-4 py-3 text-sm font-medium text-gray-900 whitespace-nowrap">{order.codigo}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{order.titulo}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        estadoStyles[order.estado]?.badge || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {order.estado}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{order.tecnico || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="bg-white rounded-lg shadow p-5">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="w-4 h-4 text-gray-500" />
            <h2 className="text-sm font-semibold text-gray-900">Técnicos con más órdenes completadas</h2>
          </div>
          {summary.topTechnicians.length === 0 && (
            <p className="text-sm text-gray-500">Sin datos todavía.</p>
          )}
          <ul className="space-y-3">
            {summary.topTechnicians.map((item, index) => (
              <li key={item.tecnico} className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  <span className="w-6 h-6 flex items-center justify-center rounded-full bg-gray-100 text-xs font-semibold text-gray-600">
                    {index + 1}
                  </span>
                  <span className="text-sm text-gray-700 truncate">{item.tecnico}</span>
                </div>
                <span className="text-sm font-semibold text-gray-900">{item.completadas}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
