import { useEffect, useState, type FormEvent } from 'react'
import type { ConfiguracionSistema } from '@/types'
import { configuracionService } from '@/services'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Settings, Sliders, Save } from 'lucide-react'
import { UsuariosSeccion } from '@/components/configuracion/UsuariosSeccion'

type Campos = Record<keyof ConfiguracionSistema, string>

const CAMPOS: { clave: keyof ConfiguracionSistema; etiqueta: string; ayuda: string; paso: string }[] = [
  { clave: 'ivaPct', etiqueta: 'IVA (%)', ayuda: 'Se suma a todos los precios que se muestran a los clientes.', paso: '0.01' },
  { clave: 'tipoCambio', etiqueta: 'Tipo de cambio (C$ por dólar)', ayuda: 'Para convertir las tarifas en dólares a córdobas.', paso: '0.0001' },
  { clave: 'tarifaKgUsd', etiqueta: 'Tarifa por kg de placas y platinas (USD, sin IVA)', ayuda: 'Se usa cuando el cliente no tiene una tarifa propia.', paso: '0.01' },
  { clave: 'densidadKgIn3', etiqueta: 'Densidad del acero (kg por pulgada³)', ayuda: 'Para calcular el peso de placas, platinas y angulares.', paso: '0.0001' },
  { clave: 'diasPrecioViejo', etiqueta: 'Días para avisar que un precio es viejo', ayuda: 'Un precio más antiguo que esto se marca con una alerta.', paso: '1' },
]

const aCampos = (c: ConfiguracionSistema): Campos => ({
  ivaPct: String(c.ivaPct),
  tipoCambio: String(c.tipoCambio),
  tarifaKgUsd: String(c.tarifaKgUsd),
  densidadKgIn3: String(c.densidadKgIn3),
  diasPrecioViejo: String(c.diasPrecioViejo),
})

export function ConfiguracionPage() {
  const [campos, setCampos] = useState<Campos | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

  useEffect(() => {
    configuracionService.obtener()
      .then(c => setCampos(aCampos(c)))
      .catch(() => setAviso({ tipo: 'error', texto: 'No se pudo cargar la configuración.' }))
  }, [])

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    if (!campos) return
    setGuardando(true)
    setAviso(null)
    try {
      const guardada = await configuracionService.actualizar({
        ivaPct: Number(campos.ivaPct),
        tipoCambio: Number(campos.tipoCambio),
        tarifaKgUsd: Number(campos.tarifaKgUsd),
        densidadKgIn3: Number(campos.densidadKgIn3),
        diasPrecioViejo: Number(campos.diasPrecioViejo),
      })
      setCampos(aCampos(guardada))
      setAviso({ tipo: 'ok', texto: 'Configuración guardada.' })
    } catch (err) {
      setAviso({ tipo: 'error', texto: err instanceof Error ? err.message : 'No se pudo guardar.' })
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="p-8 flex flex-col gap-6 max-w-3xl mx-auto w-full">

      {/* ── Header ── */}
      <div>
        <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
          <Settings className="w-5 h-5" /> Configuración
        </h1>
        <p className="text-sm text-[#705D56] mt-0.5">Valores del negocio y usuarios del sistema.</p>
      </div>

      {/* ── Valores del negocio ── */}
      <form onSubmit={guardar} className="bg-white border border-[#E5E7EB] rounded shadow-sm flex flex-col overflow-hidden">
        <div className="px-6 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center gap-2">
          <Sliders className="w-4 h-4 text-[#022F40]" />
          <h2 className="text-sm font-semibold text-[#022F40]">Valores del negocio</h2>
        </div>

        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-5">
          {CAMPOS.map(c => (
            <div key={c.clave} className="flex flex-col gap-1.5">
              <label htmlFor={c.clave} className="text-xs font-medium text-[#705D56]">{c.etiqueta}</label>
              <Input
                id={c.clave}
                type="number"
                step={c.paso}
                min="0"
                disabled={!campos}
                value={campos?.[c.clave] ?? ''}
                onChange={e => campos && setCampos({ ...campos, [c.clave]: e.target.value })}
              />
              <p className="text-[11px] text-[#80727B]">{c.ayuda}</p>
            </div>
          ))}
        </div>

        <div className="px-6 py-4 border-t border-[#E5E7EB] bg-[#F9FAFB] flex items-center justify-between gap-4">
          <p className={`text-sm ${aviso?.tipo === 'ok' ? 'text-[#558564]' : 'text-red-600'}`}>{aviso?.texto}</p>
          <Button type="submit" disabled={!campos || guardando} className="bg-[#022F40] hover:bg-[#022F40]/90 text-white gap-2">
            <Save className="w-4 h-4" /> {guardando ? 'Guardando…' : 'Guardar'}
          </Button>
        </div>
      </form>

      <UsuariosSeccion />
    </div>
  )
}
