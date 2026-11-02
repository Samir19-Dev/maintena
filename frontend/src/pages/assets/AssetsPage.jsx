import { useState, useEffect } from 'react';
import assetService from '../../services/assetService';
import categoryService from '../../services/categoryService';
import locationService from '../../services/locationService';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';
import { Plus, Pencil, Trash2 } from 'lucide-react';

const emptyForm = {
  codigo: '',
  nombre: '',
  descripcion: '',
  marca: '',
  modelo: '',
  serial: '',
  fechaAdquisicion: '',
  categoriaId: '',
  ubicacionId: '',
  activo: true,
};

const errorMessage = (error, fallback) => {
  const data = error.response?.data;
  const firstValidationError = data?.errors ? Object.values(data.errors)[0] : null;
  return firstValidationError || data?.message || fallback;
};

const AssetsPage = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.rol === 'ADMIN';

  const [assets, setAssets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [assetsRes, catsRes, locsRes] = await Promise.all([
        assetService.getAll(),
        categoryService.getAll(),
        locationService.getAll(),
      ]);
      setAssets(assetsRes.data);
      setCategories(catsRes.data);
      setLocations(locsRes.data);
    } catch (error) {
      toast.error('Error al cargar datos');
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
      const payload = {
        ...formData,
        categoriaId: formData.categoriaId ? Number(formData.categoriaId) : null,
        ubicacionId: formData.ubicacionId ? Number(formData.ubicacionId) : null,
      };
      if (editingId) {
        await assetService.update(editingId, payload);
        toast.success('Activo actualizado');
      } else {
        await assetService.create(payload);
        toast.success('Activo creado');
      }
      setShowModal(false);
      setFormData(emptyForm);
      setEditingId(null);
      fetchData();
    } catch (error) {
      toast.error(errorMessage(error, 'Error al guardar'));
    }
  };

  const handleEdit = (asset) => {
    setFormData({
      codigo: asset.codigo,
      nombre: asset.nombre,
      descripcion: asset.descripcion || '',
      marca: asset.marca || '',
      modelo: asset.modelo || '',
      serial: asset.serial || '',
      fechaAdquisicion: asset.fechaAdquisicion || '',
      activo: asset.activo,
      categoriaId: asset.categoriaId || '',
      ubicacionId: asset.ubicacionId || '',
    });
    setEditingId(asset.id);
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('¿Está seguro de eliminar este activo?')) {
      try {
        await assetService.delete(id);
        toast.success('Activo eliminado');
        fetchData();
      } catch (error) {
        toast.error('Error al eliminar');
      }
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Activos</h1>
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
            Nuevo Activo
          </button>
        )}
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Código</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nombre</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Categoría</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ubicación</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estado</th>
              {isAdmin && (
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Acciones</th>
              )}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {loading ? (
              <tr>
                <td colSpan={isAdmin ? 6 : 5} className="px-6 py-4 text-center text-gray-500">Cargando...</td>
              </tr>
            ) : assets.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 6 : 5} className="px-6 py-4 text-center text-gray-500">No hay activos</td>
              </tr>
            ) : (
              assets.map((asset) => (
                <tr key={asset.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{asset.codigo}</td>
                  <td className="px-6 py-4 text-sm text-gray-900">{asset.nombre}</td>
                  <td className="px-6 py-4 text-sm text-gray-500">{asset.categoriaNombre || '-'}</td>
                  <td className="px-6 py-4 text-sm text-gray-500">{asset.ubicacionNombre || '-'}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${asset.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {asset.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button onClick={() => handleEdit(asset)} className="text-blue-600 hover:text-blue-900 mr-4">
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button onClick={() => handleDelete(asset.id)} className="text-red-600 hover:text-red-900">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg max-h-screen overflow-y-auto">
            <h2 className="text-lg font-bold mb-4">{editingId ? 'Editar Activo' : 'Nuevo Activo'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Código</label>
                  <input
                    type="text"
                    value={formData.codigo}
                    onChange={(e) => setFormData({ ...formData, codigo: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Nombre</label>
                  <input
                    type="text"
                    value={formData.nombre}
                    onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                    required
                  />
                </div>
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
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Marca</label>
                  <input
                    type="text"
                    value={formData.marca}
                    onChange={(e) => setFormData({ ...formData, marca: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Modelo</label>
                  <input
                    type="text"
                    value={formData.modelo}
                    onChange={(e) => setFormData({ ...formData, modelo: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Serial</label>
                  <input
                    type="text"
                    value={formData.serial}
                    onChange={(e) => setFormData({ ...formData, serial: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Categoría</label>
                  <select
                    value={formData.categoriaId}
                    onChange={(e) => setFormData({ ...formData, categoriaId: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                    required
                  >
                    <option value="">Seleccione...</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.nombre}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Ubicación</label>
                  <select
                    value={formData.ubicacionId}
                    onChange={(e) => setFormData({ ...formData, ubicacionId: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                    required
                  >
                    <option value="">Seleccione...</option>
                    {locations.map((l) => (
                      <option key={l.id} value={l.id}>{l.nombre}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Fecha de adquisición</label>
                  <input
                    type="date"
                    value={formData.fechaAdquisicion}
                    onChange={(e) => setFormData({ ...formData, fechaAdquisicion: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                </div>
                <div className="flex items-center mt-6">
                  <input
                    type="checkbox"
                    checked={formData.activo}
                    onChange={(e) => setFormData({ ...formData, activo: e.target.checked })}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label className="ml-2 block text-sm text-gray-900">Activo</label>
                </div>
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">
                  Guardar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AssetsPage;
