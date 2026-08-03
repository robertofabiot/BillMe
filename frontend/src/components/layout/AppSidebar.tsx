import { NavLink } from 'react-router-dom'
import {
  ReceiptText,
  Users,
  Package,
  FolderKanban,
  BarChart3,
  Settings,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/',          label: 'Facturas',   icon: ReceiptText },
  { to: '/clientes',  label: 'Clientes',   icon: Users },
  { to: '/productos', label: 'Productos',  icon: Package },
  { to: '/proyectos', label: 'Proyectos',  icon: FolderKanban },
  { to: '/reportes',  label: 'Reportes',   icon: BarChart3 },
]

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

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 flex flex-col gap-0.5">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
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
      <div className="px-3 py-4 border-t border-[#E5E7EB]">
        <NavLink
          to="/configuracion"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 px-3 py-2 rounded text-sm font-medium transition-colors',
              isActive
                ? 'bg-[#022F40] text-white'
                : 'text-[#705D56] hover:bg-[#F9FAFB] hover:text-[#022F40]'
            )
          }
        >
          <Settings className="w-4 h-4 shrink-0" />
          Configuración
        </NavLink>
      </div>
    </aside>
  )
}
