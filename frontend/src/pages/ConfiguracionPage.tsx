import { useState } from 'react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Settings, Sliders, Save } from 'lucide-react'

export function ConfiguracionPage() {
  // Estado Mock para Preferencias del Sistema
  const [sistema, setSistema] = useState({
    iva: 15,
    moneda: 'NIO',
    folioSiguiente: 'FAC-0005'
  })

  const handleSaveSistema = () => {
    alert('Preferencias del sistema guardadas (simulado).')
  }

  return (
    <div className="p-8 flex flex-col gap-6 max-w-3xl mx-auto w-full">

      {/* ── Header ── */}
      <div>
        <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
          <Settings className="w-5 h-5" /> Configuración
        </h1>
        <p className="text-sm text-[#705D56] mt-0.5">Ajustes generales del sistema y preferencias de usuario.</p>
      </div>

      {/* ── Contenido ── */}
      <div className="bg-white border border-[#E5E7EB] rounded shadow-sm flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-2 duration-300">
        <div className="px-6 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center gap-2">
          <Sliders className="w-4 h-4 text-[#022F40]" />
          <h2 className="text-sm font-semibold text-[#022F40]">Ajustes del Sistema</h2>
        </div>
        
        <div className="p-6 flex flex-col gap-8">
          {/* Facturación */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-[#80727B] mb-4">Facturación y Finanzas</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-medium text-[#705D56]">IVA por defecto (%)</label>
                <Input type="number" value={sistema.iva} onChange={e => setSistema({...sistema, iva: Number(e.target.value)})} />
                <p className="text-[10px] text-[#80727B]">Se utilizará al calcular costos netos y facturas.</p>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-medium text-[#705D56]">Moneda Principal</label>
                <select 
                  value={sistema.moneda}
                  onChange={e => setSistema({...sistema, moneda: e.target.value})}
                  className="h-9 w-full rounded border border-[#E5E7EB] bg-white px-3 py-1 text-sm text-[#022F40] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 transition-all"
                >
                  <option value="NIO">Córdobas (C$)</option>
                  <option value="USD">Dólares (USD)</option>
                </select>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-medium text-[#705D56]">Siguiente Folio de Factura</label>
                <Input value={sistema.folioSiguiente} onChange={e => setSistema({...sistema, folioSiguiente: e.target.value})} />
              </div>
            </div>
          </div>
        </div>

        <div className="px-6 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] flex justify-end">
          <Button onClick={handleSaveSistema} className="bg-[#022F40] hover:bg-[#022F40]/90 text-white gap-2">
            <Save className="w-4 h-4" /> Guardar Preferencias
          </Button>
        </div>
      </div>
    </div>
  )
}
