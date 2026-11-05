import { NavLink } from 'react-router-dom'
import {
  ReceiptText,
  FileText,
  Users,
  Package,
  FolderKanban,
  BarChart3,
  Settings,
  Layers,
  LogOut,
  UserCircle,
  Upload,
  Tags
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '@/context/AuthContext'

export const navItems = [
  { to: '/',             label: 'Facturas',     icon: ReceiptText },
  { to: '/cotizaciones', label: 'Cotizaciones', icon: FileText },
  { to: '/precios',      label: 'Precios',      icon: Tags },
  { to: '/consolidados', label: 'Consolidados', icon: Layers },
  { to: '/clientes',     label: 'Clientes',     icon: Users },
  { to: '/productos',    label: 'Productos',    icon: Package },
  { to: '/proyectos',    label: 'Proyectos',    icon: FolderKanban },
  { to: '/reportes',     label: 'Reportes',     icon: BarChart3 },
  { to: '/importar',     label: 'Importar',     icon: Upload },
]

export function MenuNavegacion({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth()

  return (
    <div className="flex flex-col flex-1 justify-between min-h-0">
      {/* Nav */}
      <nav className="px-3 py-4 flex flex-col gap-0.5">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            onClick={() => onNavigate?.()}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-2 rounded text-sm font-medium transition-colors',
                isActive
                  ? 'bg-[#022F40] text-white'
                  : 'text-[#705D56] hover:bg-[#F9FAFB] hover:text-[#022F40]'
              )
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Bottom */}
      <div className="px-3 py-4 border-t border-[#E5E7EB] flex flex-col gap-1">
        <NavLink
          to="/configuracion"
          onClick={() => onNavigate?.()}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 px-3 py-2 rounded text-sm font-medium transition-colors mb-2',
              isActive
                ? 'bg-[#022F40] text-white'
                : 'text-[#705D56] hover:bg-[#F9FAFB] hover:text-[#022F40]'
            )
          }
        >
          <Settings className="w-4 h-4 shrink-0" />
          Configuración
        </NavLink>
        
        {/* User Profile & Logout */}
        <div className="bg-[#F9FAFB] border border-[#E5E7EB] rounded p-3 flex flex-col gap-3 mt-1">
          <div className="flex items-center gap-2 overflow-hidden">
            <UserCircle className="w-6 h-6 text-[#80727B] shrink-0" />
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-bold text-[#022F40] truncate">{user?.nombre || 'Usuario'}</span>
              <span className="text-sm text-[#705D56] uppercase tracking-wider">{user?.username}</span>
            </div>
          </div>
          <button 
            onClick={() => {
              onNavigate?.()
              logout()
            }}
            className="flex items-center justify-center gap-2 w-full px-2 py-2 rounded bg-white border border-[#E5E7EB] text-sm font-medium text-red-600 hover:bg-red-50 hover:border-red-200 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  )
}

export function AppSidebar() {
  return (
    <aside className="flex flex-col w-56 min-h-screen border-r border-[#E5E7EB] bg-white shrink-0">
      {/* Logo */}
      <div className="flex items-center gap-2 px-6 py-5 border-b border-[#E5E7EB]">
        <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
          <ReceiptText className="w-4 h-4 text-[#91E5F6]" />
        </div>
        <span className="font-semibold text-[#022F40] tracking-tight text-base">
          Bill<span className="text-[#91E5F6]">me</span>
        </span>
      </div>

      <MenuNavegacion />
    </aside>
  )
}
