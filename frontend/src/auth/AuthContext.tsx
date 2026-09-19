import React, { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { AuthUser } from '../types';

interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const raw = localStorage.getItem('keystone_auth');
    if (raw) setUser(JSON.parse(raw));
    setLoading(false);
  }, []);

  async function login(email: string, password: string) {
    const res = await api.post('/api/auth/login', { email, password });
    const authUser: AuthUser = res.data;
    localStorage.setItem('keystone_auth', JSON.stringify(authUser));
    setUser(authUser);
  }

  function logout() {
    localStorage.removeItem('keystone_auth');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
