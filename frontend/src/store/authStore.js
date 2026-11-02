import { create } from 'zustand';
import authService from '../services/authService';
import { clearSession } from '../api/axios';

const storedUser = localStorage.getItem('user');

export const useAuthStore = create((set) => ({
  user: storedUser ? JSON.parse(storedUser) : null,
  accessToken: localStorage.getItem('accessToken'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
  isLoading: false,

  login: async (credentials) => {
    set({ isLoading: true });
    try {
      const response = await authService.login(credentials);
      const { accessToken, refreshToken, userId, nombre, apellido, email, rol } = response.data;
      localStorage.setItem('accessToken', accessToken);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }
      const user = { id: userId, nombre, apellido, email, rol };
      localStorage.setItem('user', JSON.stringify(user));
      set({ accessToken, user, isAuthenticated: true, isLoading: false });
      return response.data;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  logout: async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    try {
      if (refreshToken) {
        await authService.logout(refreshToken);
      }
    } catch {
      // Si el backend no está disponible, cerramos la sesión localmente igualmente.
    }
    clearSession();
    set({ user: null, accessToken: null, isAuthenticated: false });
  },

  updateProfile: async (data) => {
    const response = await authService.updateProfile(data);
    const profile = response.data;
    set((state) => {
      const user = {
        ...state.user,
        id: profile.id,
        nombre: profile.nombre,
        apellido: profile.apellido,
        email: profile.email,
        rol: profile.rol,
      };
      localStorage.setItem('user', JSON.stringify(user));
      return { user };
    });
    return profile;
  },

  changePassword: (data) => authService.changePassword(data),
}));
