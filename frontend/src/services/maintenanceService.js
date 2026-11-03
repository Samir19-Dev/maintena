import api from '../api/axios';

const maintenanceService = {
  orders: {
    getAll: () => api.get('/maintenance/orders'),
    getById: (id) => api.get(`/maintenance/orders/${id}`),
    create: (data) => api.post('/maintenance/orders', data),
    update: (id, data) => api.put(`/maintenance/orders/${id}`, data),
    assign: (id, tecnicoId) => api.patch(`/maintenance/orders/${id}/assign`, { tecnicoId }),
    changeStatus: (id, estado, observaciones) =>
      api.patch(`/maintenance/orders/${id}/status`, { estado, observaciones }),
    reopen: (id, motivo) => api.patch(`/maintenance/orders/${id}/reopen`, { motivo }),
    delete: (id) => api.delete(`/maintenance/orders/${id}`),
    comments: {
      list: (orderId) => api.get(`/maintenance/orders/${orderId}/comments`),
      create: (orderId, data) => api.post(`/maintenance/orders/${orderId}/comments`, data),
      remove: (orderId, commentId) =>
        api.delete(`/maintenance/orders/${orderId}/comments/${commentId}`),
    },
    attachments: {
      list: (orderId) => api.get(`/maintenance/orders/${orderId}/attachments`),
      upload: (orderId, file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post(`/maintenance/orders/${orderId}/attachments`, formData);
      },
      download: (orderId, attachmentId) =>
        api.get(`/maintenance/orders/${orderId}/attachments/${attachmentId}`, {
          responseType: 'blob',
        }),
      remove: (orderId, attachmentId) =>
        api.delete(`/maintenance/orders/${orderId}/attachments/${attachmentId}`),
    },
  },
  reports: {
    getAll: () => api.get('/maintenance/reports'),
    getMine: () => api.get('/maintenance/reports/mine'),
    getById: (id) => api.get(`/maintenance/reports/${id}`),
    create: (data) => api.post('/maintenance/reports', data),
    approve: (id) => api.patch(`/maintenance/reports/${id}/approve`),
    reject: (id) => api.patch(`/maintenance/reports/${id}/reject`),
    delete: (id) => api.delete(`/maintenance/reports/${id}`),
  },
};

export default maintenanceService;
