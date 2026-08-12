import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import type { AuthResponse, AuthRequest } from '@/types'
import { authService } from '@/services'

interface AuthContextType {
  user: AuthResponse | null
  loading: boolean
  login: (credentials: AuthRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Restaurar sesión desde localStorage al cargar la app
    const token = localStorage.getItem('billme_token')
    const savedUser = localStorage.getItem('billme_user')
    
    if (token && savedUser) {
      try {
        setUser(JSON.parse(savedUser))
      } catch (e) {
        localStorage.removeItem('billme_token')
        localStorage.removeItem('billme_user')
      }
    }
    setLoading(false)
  }, [])

  const login = async (credentials: AuthRequest) => {
    const data = await authService.login(credentials)
    localStorage.setItem('billme_token', data.token)
    // Guardamos la metadata del usuario (sin el token por seguridad/limpieza)
    const userInfo = { ...data }
    localStorage.setItem('billme_user', JSON.stringify(userInfo))
    setUser(userInfo)
  }

  const logout = () => {
    localStorage.removeItem('billme_token')
    localStorage.removeItem('billme_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider')
  }
  return context
}
