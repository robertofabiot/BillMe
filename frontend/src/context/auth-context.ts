import { createContext } from 'react'
import type { AuthResponse, AuthRequest } from '@/types'

/** Datos del usuario en sesión (el token se guarda aparte). */
export type UsuarioSesion = Omit<AuthResponse, 'token'>

export interface AuthContextType {
  user: UsuarioSesion | null
  loading: boolean
  login: (credentials: AuthRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined)
