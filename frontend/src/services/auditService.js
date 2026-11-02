import api from '../api/axios';

const auditService = {
  getAll: (params) => api.get('/audit', { params }),
  getEntidades: () => api.get('/audit/entidades'),
};

export default auditService;
