import api from '../api/axios';

const triggerDownload = (data, filename) => {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

const filenameFrom = (headers, fallback) => {
  const disposition = headers?.['content-disposition'];
  if (!disposition) return fallback;
  const match = disposition.match(/filename="?([^";]+)"?/);
  return match ? match[1] : fallback;
};

const reportService = {
  ordersCsv: async (params = {}) => {
    const response = await api.get('/reports/orders/csv', { params, responseType: 'blob' });
    triggerDownload(response.data, filenameFrom(response.headers, 'ordenes-mantenimiento.csv'));
  },
  ordersPdf: async (params = {}) => {
    const response = await api.get('/reports/orders/pdf', { params, responseType: 'blob' });
    triggerDownload(response.data, filenameFrom(response.headers, 'ordenes-mantenimiento.pdf'));
  },
};

export default reportService;
