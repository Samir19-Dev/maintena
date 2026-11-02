import { useEffect, useRef, useState } from 'react';
import {
  Download,
  ExternalLink,
  FileText,
  Loader2,
  MessageSquare,
  Paperclip,
  Send,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import toast from 'react-hot-toast';
import maintenanceService from '../../services/maintenanceService';
import { useAuthStore } from '../../store/authStore';

const MAX_BYTES = 5 * 1024 * 1024;
const ACCEPTED = 'image/png,image/jpeg,image/webp,image/gif,application/pdf';

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

const formatDate = (value) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
};

const formatSize = (bytes) => {
  if (!bytes && bytes !== 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const AttachmentThumb = ({ orderId, attachment }) => {
  const [url, setUrl] = useState(null);
  const esImagen = attachment.tipoContenido?.startsWith('image/');

  useEffect(() => {
    if (!esImagen) return undefined;
    let activo = true;
    let objectUrl;
    maintenanceService.orders.attachments
      .download(orderId, attachment.id)
      .then((response) => {
        if (!activo) return;
        objectUrl = URL.createObjectURL(response.data);
        setUrl(objectUrl);
      })
      .catch(() => {});
    return () => {
      activo = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [orderId, attachment.id, esImagen]);

  if (!esImagen) {
    return (
      <div className="w-12 h-12 rounded bg-red-50 text-red-600 flex items-center justify-center shrink-0">
        <FileText className="w-6 h-6" />
      </div>
    );
  }
  if (!url) {
    return (
      <div className="w-12 h-12 rounded bg-gray-100 flex items-center justify-center shrink-0">
        <Loader2 className="w-5 h-5 animate-spin text-gray-400" />
      </div>
    );
  }
  return <img src={url} alt={attachment.nombreOriginal} className="w-12 h-12 rounded object-cover shrink-0" />;
};

const OrderDetailModal = ({ order, onClose }) => {
  const { user } = useAuthStore();
  const isAdmin = user?.rol === 'ADMIN';

  const [tab, setTab] = useState('detalle');
  const [comments, setComments] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(true);
  const [loadingAttachments, setLoadingAttachments] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [sending, setSending] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    let activo = true;
    maintenanceService.orders.comments
      .list(order.id)
      .then((response) => {
        if (activo) setComments(response.data);
      })
      .catch(() => toast.error('No se pudieron cargar los comentarios'))
      .finally(() => {
        if (activo) setLoadingComments(false);
      });

    maintenanceService.orders.attachments
      .list(order.id)
      .then((response) => {
        if (activo) setAttachments(response.data);
      })
      .catch(() => toast.error('No se pudieron cargar los adjuntos'))
      .finally(() => {
        if (activo) setLoadingAttachments(false);
      });

    return () => {
      activo = false;
    };
  }, [order.id]);

  const handleAddComment = async (event) => {
    event.preventDefault();
    const contenido = commentText.trim();
    if (!contenido) {
      toast.error('Escribe un comentario');
      return;
    }
    setSending(true);
    try {
      const response = await maintenanceService.orders.comments.create(order.id, { contenido });
      setComments((prev) => [...prev, response.data]);
      setCommentText('');
      toast.success('Comentario agregado');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo agregar el comentario');
    } finally {
      setSending(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('¿Eliminar este comentario?')) return;
    try {
      await maintenanceService.orders.comments.remove(order.id, commentId);
      setComments((prev) => prev.filter((comment) => comment.id !== commentId));
      toast.success('Comentario eliminado');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo eliminar el comentario');
    }
  };

  const handleUpload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const tiposPermitidos = ACCEPTED.split(',');
    if (!tiposPermitidos.includes(file.type)) {
      toast.error('Solo se permiten imágenes (PNG, JPG, WEBP, GIF) o PDF');
      return;
    }
    if (file.size > MAX_BYTES) {
      toast.error('El archivo supera el máximo de 5 MB');
      return;
    }

    setUploading(true);
    try {
      const response = await maintenanceService.orders.attachments.upload(order.id, file);
      setAttachments((prev) => [...prev, response.data]);
      toast.success('Adjunto subido');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo subir el adjunto');
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (attachment) => {
    try {
      const response = await maintenanceService.orders.attachments.download(order.id, attachment.id);
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachment.nombreOriginal;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 30000);
    } catch {
      toast.error('No se pudo descargar el adjunto');
    }
  };

  const handleOpen = async (attachment) => {
    // Abrimos la pestaña antes del await para que el navegador no la bloquee como popup.
    const ventana = window.open('', '_blank');
    try {
      const response = await maintenanceService.orders.attachments.download(order.id, attachment.id);
      const url = URL.createObjectURL(response.data);
      if (ventana) {
        ventana.location.href = url;
      } else {
        const link = document.createElement('a');
        link.href = url;
        link.download = attachment.nombreOriginal;
        link.click();
      }
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch {
      if (ventana) ventana.close();
      toast.error('No se pudo abrir el adjunto');
    }
  };

  const handleDeleteAttachment = async (attachment) => {
    if (!window.confirm(`¿Eliminar "${attachment.nombreOriginal}"?`)) return;
    try {
      await maintenanceService.orders.attachments.remove(order.id, attachment.id);
      setAttachments((prev) => prev.filter((item) => item.id !== attachment.id));
      toast.success('Adjunto eliminado');
    } catch (error) {
      toast.error(error.response?.data?.message || 'No se pudo eliminar el adjunto');
    }
  };

  const tabs = [
    { id: 'detalle', label: 'Detalle', icon: FileText },
    { id: 'comentarios', label: `Comentarios (${comments.length})`, icon: MessageSquare },
    { id: 'adjuntos', label: `Adjuntos (${attachments.length})`, icon: Paperclip },
  ];

  return (
    <div className="fixed inset-0 z-50 bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Detalle de la orden ${order.codigo}`}
        className="bg-white rounded-lg w-full max-w-3xl max-h-[90vh] flex flex-col"
      >
        <div className="flex items-start justify-between p-5 border-b">
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{order.codigo}</p>
            <h2 className="text-lg font-bold text-gray-900">{order.titulo}</h2>
            <div className="flex flex-wrap items-center gap-2 mt-2">
              <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${estadoStyles[order.estado] || 'bg-gray-100 text-gray-700'}`}>
                {order.estado}
              </span>
              <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${prioridadStyles[order.prioridad] || 'bg-gray-100 text-gray-700'}`}>
                {order.prioridad}
              </span>
              <span className="text-xs text-gray-500">{order.tipo}</span>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" aria-label="Cerrar">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex border-b px-5">
          {tabs.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex items-center gap-1.5 px-3 py-3 text-sm font-medium border-b-2 -mb-px ${
                tab === id
                  ? 'border-blue-600 text-blue-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </div>

        <div className="p-5 overflow-y-auto flex-1">
          {tab === 'detalle' && (
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-gray-500">Activo</dt>
                <dd className="font-medium text-gray-900">{order.activoCodigo} · {order.activoNombre}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Técnico asignado</dt>
                <dd className="font-medium text-gray-900">{order.tecnicoNombre || 'Sin asignar'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Solicitante</dt>
                <dd className="font-medium text-gray-900">{order.solicitanteNombre || '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Fecha programada</dt>
                <dd className="font-medium text-gray-900">{order.fechaProgramada || '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Inicio</dt>
                <dd className="font-medium text-gray-900">{formatDate(order.fechaInicio)}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Fin</dt>
                <dd className="font-medium text-gray-900">{formatDate(order.fechaFin)}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Costo</dt>
                <dd className="font-medium text-gray-900">
                  {order.costo != null ? `$ ${Number(order.costo).toLocaleString('es-CO')}` : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Creada</dt>
                <dd className="font-medium text-gray-900">{formatDate(order.fechaCreacion)}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-gray-500">Descripción</dt>
                <dd className="text-gray-900 whitespace-pre-line">{order.descripcion || '—'}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-gray-500">Observaciones</dt>
                <dd className="text-gray-900 whitespace-pre-line">{order.observaciones || '—'}</dd>
              </div>
            </dl>
          )}

          {tab === 'comentarios' && (
            <div className="space-y-4">
              {loadingComments ? (
                <p className="text-sm text-gray-500">Cargando comentarios...</p>
              ) : comments.length === 0 ? (
                <p className="text-sm text-gray-500">Todavía no hay comentarios en esta orden.</p>
              ) : (
                <ul className="space-y-3">
                  {comments.map((comment) => (
                    <li key={comment.id} className="rounded-lg border border-gray-200 p-3">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm font-semibold text-gray-900">{comment.autorNombre}</p>
                          <p className="text-xs text-gray-500">{formatDate(comment.fechaCreacion)}</p>
                        </div>
                        {(isAdmin || comment.autorId === user?.id) && (
                          <button
                            onClick={() => handleDeleteComment(comment.id)}
                            className="text-gray-400 hover:text-red-600"
                            title="Eliminar comentario"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <p className="mt-2 text-sm text-gray-700 whitespace-pre-line">{comment.contenido}</p>
                    </li>
                  ))}
                </ul>
              )}

              <form onSubmit={handleAddComment} className="space-y-2 border-t pt-4">
                <textarea
                  value={commentText}
                  onChange={(event) => setCommentText(event.target.value)}
                  rows="3"
                  maxLength={1000}
                  placeholder="Escribe una nota de seguimiento..."
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                />
                <div className="flex items-center justify-between">
                  <span className="text-xs text-gray-400">{commentText.length}/1000</span>
                  <button
                    type="submit"
                    disabled={sending}
                    className="inline-flex items-center px-3 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    {sending ? <Loader2 className="w-4 h-4 mr-1.5 animate-spin" /> : <Send className="w-4 h-4 mr-1.5" />}
                    Comentar
                  </button>
                </div>
              </form>
            </div>
          )}

          {tab === 'adjuntos' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm text-gray-500">Imágenes o PDF de hasta 5 MB.</p>
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                  className="inline-flex items-center px-3 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  {uploading ? <Loader2 className="w-4 h-4 mr-1.5 animate-spin" /> : <Upload className="w-4 h-4 mr-1.5" />}
                  Subir adjunto
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPTED}
                  onChange={handleUpload}
                  className="hidden"
                />
              </div>

              {loadingAttachments ? (
                <p className="text-sm text-gray-500">Cargando adjuntos...</p>
              ) : attachments.length === 0 ? (
                <p className="text-sm text-gray-500">Todavía no hay adjuntos en esta orden.</p>
              ) : (
                <ul className="divide-y divide-gray-200 border rounded-lg">
                  {attachments.map((attachment) => (
                    <li key={attachment.id} className="flex items-center gap-3 p-3">
                      <AttachmentThumb orderId={order.id} attachment={attachment} />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-900 truncate">{attachment.nombreOriginal}</p>
                        <p className="text-xs text-gray-500">
                          {formatSize(attachment.tamano)} · {attachment.subidoPorNombre} · {formatDate(attachment.fechaCreacion)}
                        </p>
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleOpen(attachment)}
                          className="p-1.5 text-gray-500 hover:text-blue-600"
                          title="Abrir"
                        >
                          <ExternalLink className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDownload(attachment)}
                          className="p-1.5 text-gray-500 hover:text-blue-600"
                          title="Descargar"
                        >
                          <Download className="w-4 h-4" />
                        </button>
                        {(isAdmin || attachment.subidoPorId === user?.id) && (
                          <button
                            onClick={() => handleDeleteAttachment(attachment)}
                            className="p-1.5 text-gray-400 hover:text-red-600"
                            title="Eliminar"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OrderDetailModal;
