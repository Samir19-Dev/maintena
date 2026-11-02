import api from '../api/axios';

const dashboardService = {
  getSummary: () => api.get('/dashboard/summary'),
};

export default dashboardService;
