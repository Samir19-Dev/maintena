import { useState, useEffect } from 'react';
import locationService from '../../services/locationService';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

const LocationsPage = () => {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ nombre: '', edificio: '', piso: '', descripcion: '', activo: true });
  const [editingId, setEditingId] = useState(null);

  const fetchLocations = async () => {
    try {
      const response = await locationService.getAll();
      setLocations(response.data);
    } catch (error) {
      toast.error('Error al cargar ubicaciones');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLocations();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await locationService.update(editingId, formData);
        toast.success('Ubicación actualizada');
      } else {
        await locationService.create(formData);
        toast.success('Ubicación creada');
      }
      setShowModal(false);
      setFormData({ nombre: '', edificio: '', piso: '', descripcion: '', activo: true });
      setEditingId(null);
      fetchLocations();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Error al guardar');
    }
  };

  const handleEdit = (location) => {
    setFormData({
      nombre: location.nombre,
      edificio: location.edificio || '',
      piso: location.piso || '',
      descripcion: location.descripcion || '',
      activo: location.activo
    });
    setEditingId(location.id);
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('¿Eliminar esta ubicación?')) {
      try {
        await locationService.delete(id);
        toast.success('Ubicación eliminada');
        fetchLocations();
      } catch (error) {
        toast.error('Error al eliminar');
      }
    }
  };

  if (loading) return <div>Cargando...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Ubicaciones</h1>
        <button
          onClick={() => { setShowModal(true); setEditingId(null); setFormData({ nombre: '', edificio: '', piso: '', descripcion: '', activo: true }); }}
          className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          <Plus className="w-4 h-4 mr-2" />
          Nueva Ubicación
        </button>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nombre</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Edificio</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Piso</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Acciones</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {locations.map((loc) => (
              <tr key={loc.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{loc.nombre}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{loc.edificio}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{loc.piso}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${loc.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                    {loc.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button onClick={() => handleEdit(loc)} className="text-blue-600 hover:text-blue-900 mr-4">
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button onClick={() => handleDelete(loc.id)} className="text-red-600 hover:text-red-900">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-bold mb-4">{editingId ? 'Editar Ubicación' : 'Nueva Ubicación'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
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
              <div>
                <label className="block text-sm font-medium text-gray-700">Edificio</label>
                <input
                  type="text"
                  value={formData.edificio}
                  onChange={(e) => setFormData({ ...formData, edificio: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Piso</label>
                <input
                  type="text"
                  value={formData.piso}
                  onChange={(e) => setFormData({ ...formData, piso: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Descripción</label>
                <textarea
                  value={formData.descripcion}
                  onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
                />
              </div>
              <div className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.activo}
                  onChange={(e) => setFormData({ ...formData, activo: e.target.checked })}
                  className="h-4 w-4 text-blue-600"
                />
                <label className="ml-2 block text-sm text-gray-900">Activo</label>
              </div>
              <div className="flex justify-end space-x-3">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 border rounded-md">Cancelar</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default LocationsPage;
