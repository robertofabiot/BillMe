import { useEffect, useState } from 'react'
import type { MetodoPrecio, UnidadLongitud } from '@/types'
import { calculoService } from '@/services/calculoService'
import { precioService } from '@/services/precioService'
import { formatCurrency } from '@/lib/format'
import type { LineaForm } from './lineaForm'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  clienteId: string
  linea: LineaForm | null
  soloPeso?: boolean
  onAplicar: (datos: {
    pesoUnitKg: string
    descripcion?: string
    precioUnit?: string
    metodoPrecio?: MetodoPrecio
    parametros?: string | null
  }) => void
}

type FormaTipo = 'PLACA' | 'PLATINA' | 'ANGULAR'

const UNIDADES: { valor: UnidadLongitud; etiqueta: string }[] = [
  { valor: 'PULGADA', etiqueta: 'pulg' },
  { valor: 'PIE', etiqueta: 'pie' },
  { valor: 'METRO', etiqueta: 'm' },
  { valor: 'CM', etiqueta: 'cm' },
  { valor: 'MM', etiqueta: 'mm' },
]

export function CalculadoraPesoDialog({
  open,
  onOpenChange,
  clienteId,
  linea,
  soloPeso = false,
  onAplicar,
}: Props) {
  const [forma, setForma] = useState<FormaTipo>('PLACA')

  // Medidas
  const [anchoTexto, setAnchoTexto] = useState('')
  const [anchoUnidad, setAnchoUnidad] = useState<UnidadLongitud>('PULGADA')

  const [largoTexto, setLargoTexto] = useState('')
  const [largoUnidad, setLargoUnidad] = useState<UnidadLongitud>('PULGADA')

  const [espesorTexto, setEspesorTexto] = useState('')
  const [espesorUnidad, setEspesorUnidad] = useState<UnidadLongitud>('PULGADA')

  const [ala2Texto, setAla2Texto] = useState('')
  const [ala2Unidad, setAla2Unidad] = useState<UnidadLongitud>('PULGADA')

  // Tarifa y resultados
  const [tarifaUsd, setTarifaUsd] = useState('')
  const [pesoCalculado, setPesoCalculado] = useState<number | null>(null)
  const [descCalculada, setDescCalculada] = useState('')
  const [precioCalculado, setPrecioCalculado] = useState<number | null>(null)
  const [errorCalculo, setErrorCalculo] = useState<string | null>(null)

  useEffect(() => {
    if (!open || !linea) return

    // Determinar forma inicial
    let initialForma: FormaTipo = 'PLACA'
    if (linea.productoForma === 'PLATINA') initialForma = 'PLATINA'
    else if (linea.productoForma === 'ANGULAR') initialForma = 'ANGULAR'

    // Defaults de unidad según forma
    const defaultLargoUnidad: UnidadLongitud = (initialForma === 'PLATINA' || initialForma === 'ANGULAR') ? 'METRO' : 'PULGADA'

    let preAncho = ''
    let preAnchoU: UnidadLongitud = 'PULGADA'
    let preLargo = ''
    let preLargoU: UnidadLongitud = defaultLargoUnidad
    let preEspesor = ''
    let preEspesorU: UnidadLongitud = 'PULGADA'
    let preAla2 = ''
    let preAla2U: UnidadLongitud = 'PULGADA'
    let preTarifa = ''

    // Si viene de una línea POR_PESO con parámetros guardados, rellenar campos
    if (linea.metodoPrecio === 'POR_PESO' && linea.parametros) {
      try {
        const p = typeof linea.parametros === 'string' ? JSON.parse(linea.parametros) : linea.parametros
        if (p) {
          if (p.forma) initialForma = p.forma
          if (p.ancho) { preAncho = p.ancho.texto ?? ''; preAnchoU = p.ancho.unidad ?? 'PULGADA' }
          if (p.largo) { preLargo = p.largo.texto ?? ''; preLargoU = p.largo.unidad ?? defaultLargoUnidad }
          if (p.espesor) { preEspesor = p.espesor.texto ?? ''; preEspesorU = p.espesor.unidad ?? 'PULGADA' }
          if (p.ala2) { preAla2 = p.ala2.texto ?? ''; preAla2U = p.ala2.unidad ?? 'PULGADA' }
          if (p.tarifaUsd) preTarifa = String(p.tarifaUsd)
        }
      } catch {
        // Ignorar error al parsear parámetros viejos
      }
    }

    setForma(initialForma)
    setAnchoTexto(preAncho)
    setAnchoUnidad(preAnchoU)
    setLargoTexto(preLargo)
    setLargoUnidad(preLargoU)
    setEspesorTexto(preEspesor)
    setEspesorUnidad(preEspesorU)
    setAla2Texto(preAla2)
    setAla2Unidad(preAla2U)
    setTarifaUsd(preTarifa)
    setPesoCalculado(null)
    setDescCalculada('')
    setPrecioCalculado(null)
    setErrorCalculo(null)

    // Si no tiene tarifa y no es soloPeso, buscar la tarifa sugerida
    if (!soloPeso && !preTarifa && clienteId && (initialForma === 'PLACA' || initialForma === 'PLATINA')) {
      const desc = linea.productoGenerico ? linea.descripcion.trim() : undefined
      precioService.sugerido(clienteId, linea.productoId, desc)
        .then(res => {
          if (res.tarifaKgUsd != null) {
            setTarifaUsd(String(res.tarifaKgUsd))
          }
        })
        .catch(() => {})
    }
  }, [open, linea, clienteId, soloPeso])

  // Al cambiar forma manualmente, ajustar unidad de largo por defecto si estaba vacía
  const handleCambiarForma = (nueva: FormaTipo) => {
    setForma(nueva)
    if (!largoTexto) {
      setLargoUnidad(nueva === 'PLATINA' || nueva === 'ANGULAR' ? 'METRO' : 'PULGADA')
    }
  }

  // Cálculo de peso con debounce de 300 ms
  useEffect(() => {
    if (!open) return
    const a = anchoTexto.trim()
    const l = largoTexto.trim()
    const e = espesorTexto.trim()
    const a2 = ala2Texto.trim()

    if (!a || !l || !e || (forma === 'ANGULAR' && !a2)) {
      setPesoCalculado(null)
      setDescCalculada('')
      setErrorCalculo(null)
      return
    }

    const temporizador = setTimeout(() => {
      calculoService.peso({
        forma,
        ancho: { texto: a, unidad: anchoUnidad },
        largo: { texto: l, unidad: largoUnidad },
        espesor: { texto: e, unidad: espesorUnidad },
        ala2: forma === 'ANGULAR' ? { texto: a2, unidad: ala2Unidad } : null,
      })
        .then(res => {
          setPesoCalculado(res.pesoUnitKg)
          setDescCalculada(res.descripcion)
          setErrorCalculo(null)
        })
        .catch(err => {
          setPesoCalculado(null)
          setDescCalculada('')
          setErrorCalculo(err instanceof Error ? err.message : 'Error en las medidas')
        })
    }, 300)

    return () => clearTimeout(temporizador)
  }, [open, forma, anchoTexto, anchoUnidad, largoTexto, largoUnidad, espesorTexto, espesorUnidad, ala2Texto, ala2Unidad])

  // Cálculo de precio por peso en modo completo con Placa o Platina
  useEffect(() => {
    if (!open || soloPeso || (forma !== 'PLACA' && forma !== 'PLATINA')) {
      setPrecioCalculado(null)
      return
    }
    const tarifa = parseFloat(tarifaUsd)
    if (pesoCalculado != null && tarifa > 0) {
      calculoService.precioPorPeso({
        pesoUnitKg: pesoCalculado,
        tarifaUsd: tarifa,
      })
        .then(res => setPrecioCalculado(res.precioUnit))
        .catch(() => setPrecioCalculado(null))
    } else {
      setPrecioCalculado(null)
    }
  }, [open, soloPeso, forma, pesoCalculado, tarifaUsd])

  if (!linea) return null

  const esModoCompletoPlacaOPlatina = !soloPeso && (forma === 'PLACA' || forma === 'PLATINA')

  const handleUsar = () => {
    if (pesoCalculado == null) return

    if (soloPeso || forma === 'ANGULAR') {
      onAplicar({
        pesoUnitKg: String(pesoCalculado),
        descripcion: linea.productoGenerico && descCalculada ? descCalculada : undefined,
      })
    } else {
      const parametros = JSON.stringify({
        forma,
        ancho: { texto: anchoTexto.trim(), unidad: anchoUnidad },
        largo: { texto: largoTexto.trim(), unidad: largoUnidad },
        espesor: { texto: espesorTexto.trim(), unidad: espesorUnidad },
        tarifaUsd: parseFloat(tarifaUsd) || 0,
      })
      onAplicar({
        pesoUnitKg: String(pesoCalculado),
        descripcion: linea.productoGenerico && descCalculada ? descCalculada : undefined,
        precioUnit: precioCalculado != null ? String(precioCalculado) : undefined,
        metodoPrecio: 'POR_PESO',
        parametros,
      })
    }
    onOpenChange(false)
  }

  const puedeUsar = pesoCalculado != null && (!esModoCompletoPlacaOPlatina || precioCalculado != null)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{soloPeso ? 'Calcular peso' : 'Calculadora de peso y precio'}</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-4 py-2">
          {/* Selector de forma */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#705D56]">Forma</label>
            <div className="flex items-center gap-4 text-sm text-[#022F40]">
              {(['PLACA', 'PLATINA', 'ANGULAR'] as FormaTipo[]).map(f => (
                <label key={f} className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="radio"
                    name="formaCalculo"
                    value={f}
                    checked={forma === f}
                    onChange={() => handleCambiarForma(f)}
                    className="text-[#022F40]"
                  />
                  <span>{f === 'PLACA' ? 'Placa' : f === 'PLATINA' ? 'Platina' : 'Angular'}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Campos según forma */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* Ancho o Ala 1 */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-[#705D56]">
                {forma === 'ANGULAR' ? 'Ala 1' : 'Ancho'}
              </label>
              <div className="flex gap-1.5">
                <Input
                  value={anchoTexto}
                  onChange={e => setAnchoTexto(e.target.value)}
                  placeholder="Ej. 10 o 1 1/2"
                  className="h-8 text-xs flex-1"
                />
                <select
                  value={anchoUnidad}
                  onChange={e => setAnchoUnidad(e.target.value as UnidadLongitud)}
                  className="h-8 rounded border border-[#E5E7EB] bg-white px-2 text-xs text-[#022F40] outline-none"
                >
                  {UNIDADES.map(u => (
                    <option key={u.valor} value={u.valor}>{u.etiqueta}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Ala 2 (solo angular) */}
            {forma === 'ANGULAR' && (
              <div className="flex flex-col gap-1">
                <label className="text-xs font-medium text-[#705D56]">Ala 2</label>
                <div className="flex gap-1.5">
                  <Input
                    value={ala2Texto}
                    onChange={e => setAla2Texto(e.target.value)}
                    placeholder="Ej. 2 o 1/4"
                    className="h-8 text-xs flex-1"
                  />
                  <select
                    value={ala2Unidad}
                    onChange={e => setAla2Unidad(e.target.value as UnidadLongitud)}
                    className="h-8 rounded border border-[#E5E7EB] bg-white px-2 text-xs text-[#022F40] outline-none"
                  >
                    {UNIDADES.map(u => (
                      <option key={u.valor} value={u.valor}>{u.etiqueta}</option>
                    ))}
                  </select>
                </div>
              </div>
            )}

            {/* Espesor */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-[#705D56]">Espesor</label>
              <div className="flex gap-1.5">
                <Input
                  value={espesorTexto}
                  onChange={e => setEspesorTexto(e.target.value)}
                  placeholder="Ej. 1/4 o 3/16"
                  className="h-8 text-xs flex-1"
                />
                <select
                  value={espesorUnidad}
                  onChange={e => setEspesorUnidad(e.target.value as UnidadLongitud)}
                  className="h-8 rounded border border-[#E5E7EB] bg-white px-2 text-xs text-[#022F40] outline-none"
                >
                  {UNIDADES.map(u => (
                    <option key={u.valor} value={u.valor}>{u.etiqueta}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Largo */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-[#705D56]">Largo</label>
              <div className="flex gap-1.5">
                <Input
                  value={largoTexto}
                  onChange={e => setLargoTexto(e.target.value)}
                  placeholder={forma === 'PLACA' ? 'Ej. 8' : 'Ej. 3.05 o 6'}
                  className="h-8 text-xs flex-1"
                />
                <select
                  value={largoUnidad}
                  onChange={e => setLargoUnidad(e.target.value as UnidadLongitud)}
                  className="h-8 rounded border border-[#E5E7EB] bg-white px-2 text-xs text-[#022F40] outline-none"
                >
                  {UNIDADES.map(u => (
                    <option key={u.valor} value={u.valor}>{u.etiqueta}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Tarifa USD si es modo completo para Placa o Platina */}
          {esModoCompletoPlacaOPlatina && (
            <div className="flex flex-col gap-1 pt-1">
              <label htmlFor="tarifa-input" className="text-xs font-medium text-[#705D56]">
                Tarifa USD por kg (sin IVA)
              </label>
              <Input
                id="tarifa-input"
                type="number"
                min="0"
                step="any"
                value={tarifaUsd}
                onChange={e => setTarifaUsd(e.target.value)}
                placeholder="Ej. 1.80"
                className="h-8 text-xs max-w-xs"
              />
            </div>
          )}

          {/* Errores de cálculo */}
          {errorCalculo && (
            <p className="text-xs text-red-600 bg-red-50 p-2 rounded border border-red-200">
              {errorCalculo}
            </p>
          )}

          {/* Resumen de resultados */}
          <div className="p-3 bg-[#F9FAFB] rounded border border-[#E5E7EB] flex flex-col gap-1 text-xs">
            {descCalculada && (
              <div className="flex items-center justify-between">
                <span className="text-[#705D56]">Descripción sugerida:</span>
                <span className="font-medium text-[#022F40]">{descCalculada}</span>
              </div>
            )}
            <div className="flex items-center justify-between">
              <span className="text-[#705D56]">Peso unitario calculado:</span>
              <span className="font-semibold text-sm text-[#022F40]">
                {pesoCalculado !== null ? `${pesoCalculado.toFixed(2)} kg` : '—'}
              </span>
            </div>
            {esModoCompletoPlacaOPlatina && (
              <div className="flex items-center justify-between border-t border-[#E5E7EB] pt-1 mt-1">
                <span className="text-[#705D56]">Precio por unidad (con IVA):</span>
                <span className="font-semibold text-sm text-[#022F40]">
                  {precioCalculado !== null ? formatCurrency(precioCalculado) : '—'}
                </span>
              </div>
            )}
          </div>
        </div>

        <DialogFooter className="mt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="text-xs h-9"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            onClick={handleUsar}
            disabled={!puedeUsar}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-xs h-9"
          >
            Usar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
