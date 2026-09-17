import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loginUser, registerUser, refreshToken as refreshTokenApi, logoutUser } from '../api/authService';

const AuthContext = createContext(null);

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};

export const AuthProvider = ({ children }) => {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'));
  const [refreshTokenValue, setRefreshTokenValue] = useState(() => localStorage.getItem('refreshToken'));
  const [loading, setLoading] = useState(true);

  const isAuthenticated = !!accessToken;

  const saveTokens = (tokens) => {
    setAccessToken(tokens.accessToken);
    setRefreshTokenValue(tokens.refreshToken);
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
  };

  const clearTokens = () => {
    setAccessToken(null);
    setRefreshTokenValue(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  };

  const login = async (usernameOrEmail, password) => {
    const tokens = await loginUser(usernameOrEmail, password);
    saveTokens(tokens);
  };

  const register = async (email, username, password) => {
    const tokens = await registerUser(email, username, password);
    saveTokens(tokens);
  };

  const logout = async () => {
    try {
      if (accessToken) await logoutUser(accessToken);
    } catch {
      // logout best-effort
    }
    clearTokens();
  };

  const tryRefresh = useCallback(async () => {
    if (!refreshTokenValue) return false;
    try {
      const tokens = await refreshTokenApi(refreshTokenValue);
      saveTokens(tokens);
      return true;
    } catch {
      clearTokens();
      return false;
    }
  }, [refreshTokenValue]);

  useEffect(() => {
    if (!accessToken && refreshTokenValue) {
      tryRefresh().finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, accessToken, login, register, logout, tryRefresh, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
