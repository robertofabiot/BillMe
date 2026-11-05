import { useState, useEffect, type ReactNode } from 'react'
import type { AuthRequest } from '@/types'
import { authService } from '@/services'
import { AuthContext, type UsuarioSesion } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UsuarioSesion | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Restaurar sesión desde localStorage al cargar la app
    const token = localStorage.getItem('billme_token')
    const savedUser = localStorage.getItem('billme_user')
    
    if (token && savedUser) {
      try {
        setUser(JSON.parse(savedUser))
      } catch {
        localStorage.removeItem('billme_token')
        localStorage.removeItem('billme_user')
      }
    }
    setLoading(false)
  }, [])

  const login = async (credentials: AuthRequest) => {
    const data = await authService.login(credentials)
    localStorage.setItem('billme_token', data.token)
    // Guardamos la metadata del usuario sin el token
    const { token: _, ...userInfo } = data
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
