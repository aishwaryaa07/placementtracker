import { createContext, useContext, useState, type ReactNode } from "react";
import { api } from "../api/client";
import type { AuthResponse } from "../api/types";

interface AuthUser {
  email: string;
  name: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function storeAuth(response: AuthResponse) {
  localStorage.setItem("token", response.token);
  localStorage.setItem("email", response.email);
  localStorage.setItem("name", response.name);
}

function readStoredUser(): AuthUser | null {
  const token = localStorage.getItem("token");
  const email = localStorage.getItem("email");
  const name = localStorage.getItem("name");
  if (token && email && name) {
    return { email, name };
  }
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());

  const login = async (email: string, password: string) => {
    const response = await api.post<AuthResponse>("/api/auth/login", { email, password });
    storeAuth(response.data);
    setUser({ email: response.data.email, name: response.data.name });
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await api.post<AuthResponse>("/api/auth/register", { name, email, password });
    storeAuth(response.data);
    setUser({ email: response.data.email, name: response.data.name });
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("name");
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
