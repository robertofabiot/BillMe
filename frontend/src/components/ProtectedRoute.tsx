import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Layers } from 'lucide-react'

export function ProtectedRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center gap-4">
        <div className="w-12 h-12 bg-[#022F40] rounded-full flex items-center justify-center animate-pulse">
          <Layers className="w-6 h-6 text-[#91E5F6]" />
        </div>
        <p className="text-[#705D56] text-sm font-medium animate-pulse">Autenticando...</p>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
