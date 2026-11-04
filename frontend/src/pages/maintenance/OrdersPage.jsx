import { useState, useEffect, useMemo } from 'react';
import maintenanceService from '../../services/maintenanceService';
import assetService from '../../services/assetService';
import userService from '../../services/userService';
import { useAuthStore } from '../../store/authStore';
import { Plus, Pencil, Trash2, UserCog, Eye, RotateCcw } from 'lucide-react';
import toast from 'react-hot-toast';
import OrderDetailModal from '../../components/maintenance/OrderDetailModal';

const ESTADOS = ['PENDIENTE', 'ASIGNADA', 'EN_PROCESO', 'COMPLETADA', 'CANCELADA'];
const TIPOS = ['PREVENTIVO', 'CORRECTIVO'];
const PRIORIDADES = ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'];

const TRANSICIONES = {
  PENDIENTE: ['ASIGNADA', 'EN_PROCESO', 'CANCELADA'],
  ASIGNADA: ['PENDIENTE', 'EN_PROCESO', 'CANCELADA'],
  EN_PROCESO: ['COMPLETADA', 'CANCELADA'],
  COMPLETADA: [],
  CANCELADA: [],
};

// Cancelar es una decisión administrativa: el técnico solo opera su orden.
const estadosDisponibles = (estado, isAdmin) =>
  (TRANSICIONES[estado] || []).filter((s) => isAdmin || s !== 'CANCELADA');

const esEstadoFinal = (estado) => estado === 'COMPLETADA' || estado === 'CANCELADA';

const estadoStyles = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  ASIGNADA: 'bg-blue-100 text-blue-800',
  EN_PROCESO: 'bg-indigo-100 text-indigo-800',
  COMPLETADA: 'bg-green-100 text-green-800',
  CANCELADA: 'bg-gray-200 text-gray-700',
};

const prioridadStyles = {
  BAJA: 'bg-gray-100 text-gray-700',
  MEDIA: 'bg-sky-100 text-sky-800',
  ALTA: 'bg-orange-100 text-orange-800',
  URGENTE: 'bg-red-100 text-red-800',
};

const emptyForm = {
  titulo: '',
  descripcion: '',
  tipo: 'PREVENTIVO',
  prioridad: 'MEDIA',
  activoId: '',
  tecnicoId: '',
  fechaProgramada: '',
  costo: '',
  observaciones: '',
};

const OrdersPage = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.rol === 'ADMIN';

  const canChangeStatus = (order) =>
    isAdmin || (['TECNICO', 'SUPERVISOR'].includes(user?.rol) && order.tecnicoId === user?.id);

  const [orders, setOrders] = useState([]);
  const [assets, setAssets] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [assignOrder, setAssignOrder] = useState(null);
  const [assignTecnicoId, setAssignTecnicoId] = useState('');
  const [detailOrder, setDetailOrder] = useState(null);
  const [reopenOrder, setReopenOrder] = useState(null);
  const [reopenMotivo, setReopenMotivo] = useState('');
  const [formData, setFormData] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [filters, setFilters] = useState({ estado: '', tipo: '', search: '' });

  const fetchData = async () => {
    try {
      const [ordersRes, assetsRes, usersRes] = await Promise.all([
        maintenanceService.orders.getAll(),
        assetService.getAll(),
        isAdmin ? userService.getAll() : Promise.resolve({ data: [] }),
      ]);
      setOrders(ordersRes.data);
      setAssets(assetsRes.data);
      setTechnicians(usersRes.data.filter((u) => u.rolNombre === 'TECNICO'));
    } catch (error) {
      toast.error('Error al cargar órdenes de mantenimiento');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const filtered = useMemo(() => {
    return orders.filter((o) => {
      if (filters.estado && o.estado !== filters.estado) return false;
      if (filters.tipo && o.tipo !== filters.tipo) return false;
      if (filters.search) {
        const text = `${o.codigo} ${o.titulo} ${o.activoNombre || ''} ${o.tecnicoNombre || ''}`.toLowerCase();
        if (!text.includes(filters.search.toLowerCase())) return false;
      }
      return true;
    });
  }, [orders, filters]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      ...formData,
      activoId: Number(formData.activoId),
      tecnicoId: formData.tecnicoId ? Number(formData.tecnicoId) : null,
      fechaProgramada: formData.fechaProgramada || null,
      costo: formData.costo === '' ? null : Number(formData.costo),
    };
    try {
      if (editingId) {
        await maintenanceService.orders.update(editingId, payload);
        toast.success('Orden actualizada');
      } else {
        await maintenanceService.orders.create(payload);
        toast.success('Orden creada');
      }
      setShowModal(false);
      setFormData(emptyForm);
      setEditingId(null);
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al guardar la orden');
    }
  };

  const handleEdit = (order) => {
    setFormData({
      titulo: order.titulo,
      descripcion: order.descripcion || '',
      tipo: order.tipo,
      prioridad: order.prioridad,
      activoId: order.activoId || '',
      tecnicoId: order.tecnicoId || '',
      fechaProgramada: order.fechaProgramada || '',
      costo: order.costo ?? '',
      observaciones: order.observaciones || '',
    });
    setEditingId(order.id);
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('¿Eliminar esta orden de mantenimiento?')) {
      try {
        await maintenanceService.orders.delete(id);
        toast.success('Orden eliminada');
        fetchData();
      } catch (error) {
        toast.error(error.response?.data?.message || 'Error al eliminar');
      }
    }
  };

  const handleStatusChange = async (order, estado) => {
    if (!estado) return;

    // Cancelar o completar no se puede deshacer desde la ficha: se confirma antes.
    const mensajes = {
      CANCELADA: `¿Cancelar la orden ${order.codigo}? Podrás reabrirla después desde administración.`,
      COMPLETADA: `¿Marcar la orden ${order.codigo} como COMPLETADA? Esta acción es definitiva.`,
    };
    if (mensajes[estado] && !window.confirm(mensajes[estado])) return;

    try {
      await maintenanceService.orders.changeStatus(order.id, estado);
      toast.success(`Estado actualizado a ${estado}`);
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al cambiar el estado');
    }
  };

  const openReopen = (order) => {
    setReopenOrder(order);
    setReopenMotivo('');
  };

  const handleReopen = async (e) => {
    e.preventDefault();
    if (reopenMotivo.trim().length < 5) {
      toast.error('Indique el motivo de la reapertura (mínimo 5 caracteres)');
      return;
    }
    try {
      await maintenanceService.orders.reopen(reopenOrder.id, reopenMotivo.trim());
      toast.success('Orden reabierta');
      setReopenOrder(null);
      setReopenMotivo('');
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al reabrir la orden');
    }
  };

  const openAssign = (order) => {
    setAssignOrder(order);
    setAssignTecnicoId(order.tecnicoId || '');
  };

  const handleAssign = async (e) => {
    e.preventDefault();
    if (!assignTecnicoId) {
      toast.error('Seleccione un técnico');
      return;
    }
    try {
      await maintenanceService.orders.assign(assignOrder.id, Number(assignTecnicoId));
      toast.success('Técnico asignado');
      setAssignOrder(null);
      setAssignTecnicoId('');
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al asignar');
    }
  };

  if (loading) return <div>Cargando...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Órdenes de Mantenimiento</h1>
        {isAdmin && (
          <button
            onClick={() => {
              setFormData(emptyForm);
              setEditingId(null);
              setShowModal(true);
            }}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            <Plus className="w-4 h-4 mr-2" />
            Nueva Orden
          </button>
        )}
      </div>

      <div className="bg-white shadow rounded-lg p-4 mb-4 flex flex-wrap gap-3">
        <input
          type="text"
          placeholder="Buscar por código, título, activo o técnico..."
          value={filters.search}
          onChange={(e) => setFilters({ ...filters, search: e.target.value })}
          className="px-3 py-2 border border-gray-300 rounded-md text-sm flex-1 min-w-[220px]"
        />
        <select
          value={filters.estado}
          onChange={(e) => setFilters({ ...filters, estado: e.target.value })}
          className="px-3 py-2 border border-gray-300 rounded-md text-sm"
        >
          <option value="">Todos los estados</option>
          {ESTADOS.map((e) => (
            <option key={e} value={e}>{e}</option>
          ))}
        </select>
        <select
          value={filters.tipo}
          onChange={(e) => setFilters({ ...filters, tipo: e.target.value })}
          className="px-3 py-2 border border-gray-300 rounded-md text-sm"
        >
          <option value="">Todos los tipos</option>
          {TIPOS.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </div>

      <div className="bg-white shadow rounded-lg overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Código</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Título</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Activo</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tipo</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Prioridad</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Técnico</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Acciones</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filtered.length === 0 && (
              <tr>
                <td colSpan="8" className="px-4 py-6 text-center text-sm text-gray-500">
                  No hay órdenes de mantenimiento
                </td>
              </tr>
            )}
            {filtered.map((order) => (
              <tr key={order.id}>
                <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">{order.codigo}</td>
                <td className="px-4 py-3 text-sm text-gray-900">{order.titulo}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{order.activoNombre}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{order.tipo}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${prioridadStyles[order.prioridad] || 'bg-gray-100 text-gray-700'}`}>
                    {order.prioridad}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${estadoStyles[order.estado] || 'bg-gray-100 text-gray-700'}`}>
                    {order.estado}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{order.tecnicoNombre || '—'}</td>
                <td className="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => setDetailOrder(order)}
                      className="text-gray-600 hover:text-gray-900"
                      title="Ver detalle, comentarios y adjuntos"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                    {canChangeStatus(order) && estadosDisponibles(order.estado, isAdmin).length > 0 && (
                      <select
                        value=""
                        onChange={(e) => handleStatusChange(order, e.target.value)}
                        className="text-xs border border-gray-300 rounded px-1 py-1"
                      >
                        <option value="">Cambiar estado...</option>
                        {estadosDisponibles(order.estado, isAdmin).map((s) => (
                          <option key={s} value={s}>{s}</option>
                        ))}
                      </select>
                    )}
                    {isAdmin && order.estado === 'CANCELADA' && (
                      <button
                        onClick={() => openReopen(order)}
                        className="text-emerald-600 hover:text-emerald-900"
                        title="Reabrir orden cancelada"
                      >
                        <RotateCcw className="w-4 h-4" />
                      </button>
                    )}
                    {isAdmin && !esEstadoFinal(order.estado) && (
                      <>
                        <button onClick={() => openAssign(order)} className="text-indigo-600 hover:text-indigo-900" title="Asignar técnico">
                          <UserCog className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleEdit(order)} className="text-blue-600 hover:text-blue-900" title="Editar">
                          <Pencil className="w-4 h-4" />
                        </button>
                      </>
                    )}
                    {isAdmin && (
                      <button onClick={() => handleDelete(order.id)} className="text-red-600 hover:text-red-900" title="Eliminar">
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
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-bold mb-4">{editingId ? 'Editar Orden' : 'Nueva Orden'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Título</label>
                <input
                  type="text"
                  value={formData.titulo}
                  onChange={(e) => setFormData({ ...formData, titulo: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Descripción</label>
                <textarea
                  value={formData.descripcion}
                  onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  rows="2"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Tipo</label>
                  <select
                    value={formData.tipo}
                    onChange={(e) => setFormData({ ...formData, tipo: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  >
                    {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
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
              </div>
              <div className="grid grid-cols-2 gap-4">
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
                  <label className="block text-sm font-medium text-gray-700">Técnico (opcional)</label>
                  <select
                    value={formData.tecnicoId}
                    onChange={(e) => setFormData({ ...formData, tecnicoId: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  >
                    <option value="">Sin asignar</option>
                    {technicians.map((t) => (
                      <option key={t.id} value={t.id}>{t.nombre} {t.apellido}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Fecha programada</label>
                  <input
                    type="date"
                    value={formData.fechaProgramada}
                    onChange={(e) => setFormData({ ...formData, fechaProgramada: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Costo</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.costo}
                    onChange={(e) => setFormData({ ...formData, costo: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Observaciones</label>
                <textarea
                  value={formData.observaciones}
                  onChange={(e) => setFormData({ ...formData, observaciones: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  rows="2"
                />
              </div>
              <div className="flex justify-end space-x-3">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 border rounded-md">
                  Cancelar
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md">
                  Guardar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {assignOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-bold mb-1">Asignar técnico</h2>
            <p className="text-sm text-gray-500 mb-4">{assignOrder.codigo} — {assignOrder.titulo}</p>
            <form onSubmit={handleAssign} className="space-y-4">
              <select
                value={assignTecnicoId}
                onChange={(e) => setAssignTecnicoId(e.target.value)}
                className="block w-full px-3 py-2 border border-gray-300 rounded-md"
                required
              >
                <option value="">Seleccione un técnico...</option>
                {technicians.map((t) => (
                  <option key={t.id} value={t.id}>{t.nombre} {t.apellido}</option>
                ))}
              </select>
              <div className="flex justify-end space-x-3">
                <button type="button" onClick={() => setAssignOrder(null)} className="px-4 py-2 border rounded-md">
                  Cancelar
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md">
                  Asignar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {reopenOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-bold mb-1">Reabrir orden cancelada</h2>
            <p className="text-sm text-gray-500 mb-4">{reopenOrder.codigo} — {reopenOrder.titulo}</p>
            <p className="text-sm text-gray-600 mb-4">
              La orden conservará su código e historial y volverá a{' '}
              {reopenOrder.tecnicoId ? 'ASIGNADA' : 'PENDIENTE'}.
            </p>
            <form onSubmit={handleReopen} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Motivo de la reapertura</label>
                <textarea
                  value={reopenMotivo}
                  onChange={(e) => setReopenMotivo(e.target.value)}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  rows="3"
                  minLength="5"
                  maxLength="500"
                  placeholder="Ej.: la orden se canceló por error durante las pruebas"
                  required
                />
              </div>
              <div className="flex justify-end space-x-3">
                <button type="button" onClick={() => setReopenOrder(null)} className="px-4 py-2 border rounded-md">
                  Cancelar
                </button>
                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700">
                  Reabrir
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {detailOrder && (
        <OrderDetailModal order={detailOrder} onClose={() => setDetailOrder(null)} />
      )}
    </div>
  );
};

export default OrdersPage;
