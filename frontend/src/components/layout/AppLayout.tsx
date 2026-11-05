import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { AppSidebar, MenuNavegacion } from './AppSidebar'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'
import { Menu, ReceiptText } from 'lucide-react'

export function AppLayout() {
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <div className="flex flex-col md:flex-row min-h-screen bg-[#F9FAFB]">
      {/* Barra superior en celular (< 768px) */}
      <header className="md:hidden flex items-center justify-between px-4 py-3 bg-white border-b border-[#E5E7EB] shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
            <ReceiptText className="w-4 h-4 text-[#91E5F6]" />
          </div>
          <span className="font-semibold text-[#022F40] tracking-tight text-base">
            Bill<span className="text-[#91E5F6]">me</span>
          </span>
        </div>

        <Sheet open={menuAbierto} onOpenChange={setMenuAbierto}>
          <SheetTrigger
            aria-label="Abrir menú"
            className="w-11 h-11 flex items-center justify-center rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] text-[#022F40]"
          >
            <Menu className="w-5 h-5" />
          </SheetTrigger>
          <SheetContent side="left" className="w-64 p-0 bg-white flex flex-col h-full">
            <div className="flex items-center gap-2 px-6 py-5 border-b border-[#E5E7EB] shrink-0">
              <div className="w-7 h-7 rounded bg-[#022F40] flex items-center justify-center">
                <ReceiptText className="w-4 h-4 text-[#91E5F6]" />
              </div>
              <span className="font-semibold text-[#022F40] tracking-tight text-base">
                Bill<span className="text-[#91E5F6]">me</span>
              </span>
            </div>
            <div className="flex-1 overflow-y-auto">
              <MenuNavegacion onNavigate={() => setMenuAbierto(false)} />
            </div>
          </SheetContent>
        </Sheet>
      </header>

      {/* Menú lateral en pantallas grandes (>= 768px) */}
      <div className="hidden md:flex shrink-0">
        <AppSidebar />
      </div>

      <main className="flex-1 flex flex-col overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
