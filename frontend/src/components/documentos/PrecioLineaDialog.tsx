import { useEffect, useState } from 'react'
import type { MetodoPrecio, PrecioFormula, PrecioSugerido } from '@/types'
import { precioService } from '@/services/precioService'
import { calculoService } from '@/services/calculoService'
import { formatCurrency, formatDate } from '@/lib/format'
import type { LineaForm } from './lineaForm'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  clienteId: string
  linea: LineaForm | null
  onAplicar: (precioUnit: string, metodoPrecio: MetodoPrecio, parametros: string | null) => void
  onAbrirCalculadoraPeso: () => void
}

export function PrecioLineaDialog({
  open,
  onOpenChange,
  clienteId,
  linea,
  onAplicar,
  onAbrirCalculadoraPeso,
}: Props) {
  const [sugerido, setSugerido] = useState<PrecioSugerido | null>(null)
  const [cargando, setCargando] = useState(false)
  const [metodo, setMetodo] = useState<MetodoPrecio>('ULTIMO_PRECIO')

  // Estado para Fórmula de lista
  const [descuentoPct, setDescuentoPct] = useState('')
  const [formulaRes, setFormulaRes] = useState<PrecioFormula | null>(null)

  // Estado para Por metro
  const [precioRef, setPrecioRef] = useState('')
  const [largoRef, setLargoRef] = useState('')
  const [largoNuevo, setLargoNuevo] = useState('')
  const [puMetro, setPuMetro] = useState<number | null>(null)

  useEffect(() => {
    if (!open || !linea || !clienteId) return
    setCargando(true)
    setFormulaRes(null)
    setPuMetro(null)
    setLargoRef('')
    setLargoNuevo('')

    const desc = linea.productoGenerico ? linea.descripcion.trim() : undefined
    precioService.sugerido(clienteId, linea.productoId, desc)
      .then(res => {
        setSugerido(res)
        setDescuentoPct(res.descuentoPct != null ? String(res.descuentoPct) : '0')
        if (res.precioUnit != null) {
          setMetodo('ULTIMO_PRECIO')
          setPrecioRef(String(res.precioUnit))
        } else if (res.precioFormula != null) {
          setMetodo('FORMULA_LISTA')
          setPrecioRef('')
        } else {
          setMetodo('POR_METRO')
          setPrecioRef('')
        }
      })
      .catch(() => {
        setSugerido(null)
        setMetodo('POR_METRO')
      })
      .finally(() => setCargando(false))
  }, [open, linea, clienteId])

  // Recalcular fórmula cuando cambia descuento
  useEffect(() => {
    if (!open || !linea || sugerido?.precioLista == null) return
    const desc = parseFloat(descuentoPct)
    const temporizador = setTimeout(() => {
      precioService.formula(linea.productoId, isNaN(desc) ? 0 : desc)
        .then(setFormulaRes)
        .catch(() => setFormulaRes(null))
    }, 250)
    return () => clearTimeout(temporizador)
  }, [open, linea, sugerido?.precioLista, descuentoPct])

  // Recalcular por metro cuando cambian los campos
  useEffect(() => {
    if (!open) return
    const pRef = parseFloat(precioRef)
    const lRef = parseFloat(largoRef)
    const lNew = parseFloat(largoNuevo)
    if (pRef > 0 && lRef > 0 && lNew > 0) {
      calculoService.precioPorMetro({
        precioReferencia: pRef,
        largoReferencia: lRef,
        largoNuevo: lNew,
      })
        .then(res => setPuMetro(res.precioUnit))
        .catch(() => setPuMetro(null))
    } else {
      setPuMetro(null)
    }
  }, [open, precioRef, largoRef, largoNuevo])

  if (!linea) return null

  const admitePorPeso = linea.productoForma === 'PLACA' || linea.productoForma === 'PLATINA'

  const handleAplicar = () => {
    if (metodo === 'ULTIMO_PRECIO' && sugerido?.precioUnit != null) {
      const parametros = JSON.stringify({
        folio: sugerido.folio,
        fecha: sugerido.fecha,
        clienteNombre: sugerido.clienteNombre,
        otroCliente: sugerido.origen === 'OTRO_CLIENTE',
        dias: sugerido.diasAntiguedad,
        viejo: sugerido.viejo,
      })
      onAplicar(String(sugerido.precioUnit), 'ULTIMO_PRECIO', parametros)
      onOpenChange(false)
    } else if (metodo === 'FORMULA_LISTA' && sugerido?.precioLista != null) {
      const pu = formulaRes?.precioUnit ?? sugerido.precioFormula
      if (pu != null) {
        const parametros = JSON.stringify({
          lista: sugerido.precioLista,
          descuentoPct: parseFloat(descuentoPct) || 0,
        })
        onAplicar(String(pu), 'FORMULA_LISTA', parametros)
        onOpenChange(false)
      }
    } else if (metodo === 'POR_METRO' && puMetro != null) {
      const parametros = JSON.stringify({
        precioReferencia: parseFloat(precioRef),
        largoReferencia: parseFloat(largoRef),
        largoNuevo: parseFloat(largoNuevo),
      })
      onAplicar(String(puMetro), 'POR_METRO', parametros)
      onOpenChange(false)
    }
  }

  const puedeAplicar =
    (metodo === 'ULTIMO_PRECIO' && sugerido?.precioUnit != null) ||
    (metodo === 'FORMULA_LISTA' && (formulaRes?.precioUnit != null || sugerido?.precioFormula != null)) ||
    (metodo === 'POR_METRO' && puMetro != null)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Elegir precio</DialogTitle>
        </DialogHeader>

        {cargando && <p className="text-sm text-[#705D56] py-2">Consultando precios…</p>}

        {!cargando && (
          <div className="flex flex-col gap-4 py-2">

            {/* 1. Último precio */}
            <label className={`flex items-start gap-3 p-3 rounded border cursor-pointer transition-colors ${
              sugerido?.precioUnit != null ? 'border-[#E5E7EB] hover:bg-[#F9FAFB]' : 'opacity-60 bg-gray-50 cursor-not-allowed'
            }`}>
              <input
                type="radio"
                name="metodoPrecio"
                value="ULTIMO_PRECIO"
                checked={metodo === 'ULTIMO_PRECIO'}
                disabled={sugerido?.precioUnit == null}
                onChange={() => setMetodo('ULTIMO_PRECIO')}
                className="mt-1 text-[#022F40]"
              />
              <div className="flex-1 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[#022F40]">Último precio</span>
                  {sugerido?.precioUnit != null ? (
                    <span className="font-semibold text-[#022F40]">{formatCurrency(sugerido.precioUnit)}</span>
                  ) : (
                    <span className="text-xs text-[#80727B]">Todavía no se ha vendido</span>
                  )}
                </div>
                {sugerido?.precioUnit != null && (
                  <div className="text-xs text-[#705D56] mt-1 flex flex-col gap-0.5">
                    <span>
                      {sugerido.origen === 'OTRO_CLIENTE' && sugerido.clienteNombre
                        ? `Precio de ${sugerido.clienteNombre}`
                        : 'Último precio de este cliente'}
                      {sugerido.folio ? ` · ${sugerido.folio}` : ''}
                      {sugerido.fecha ? ` · ${formatDate(sugerido.fecha)}` : ''}
                    </span>
                    {sugerido.viejo && (
                      <span className="text-amber-600 font-medium">
                        Tiene {sugerido.diasAntiguedad} días: revísalo
                      </span>
                    )}
                  </div>
                )}
              </div>
            </label>

            {/* 2. Fórmula de lista */}
            <label className={`flex items-start gap-3 p-3 rounded border cursor-pointer transition-colors ${
              sugerido?.precioLista != null ? 'border-[#E5E7EB] hover:bg-[#F9FAFB]' : 'opacity-60 bg-gray-50 cursor-not-allowed'
            }`}>
              <input
                type="radio"
                name="metodoPrecio"
                value="FORMULA_LISTA"
                checked={metodo === 'FORMULA_LISTA'}
                disabled={sugerido?.precioLista == null}
                onChange={() => setMetodo('FORMULA_LISTA')}
                className="mt-1 text-[#022F40]"
              />
              <div className="flex-1 text-sm flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[#022F40]">Fórmula de lista</span>
                  {sugerido?.precioLista != null ? (
                    <span className="font-semibold text-[#022F40]">
                      {formatCurrency(formulaRes?.precioUnit ?? sugerido.precioFormula ?? 0)}
                    </span>
                  ) : (
                    <span className="text-xs text-[#80727B]">No tiene precio de lista</span>
                  )}
                </div>
                {sugerido?.precioLista != null && (
                  <div className="flex items-center gap-3 text-xs text-[#705D56]">
                    <span>Lista: {formatCurrency(sugerido.precioLista)}</span>
                    <div className="flex items-center gap-1.5">
                      <label htmlFor="descuento-input" className="font-medium">Descuento:</label>
                      <Input
                        id="descuento-input"
                        type="number"
                        min="0"
                        max="99.99"
                        step="0.01"
                        value={descuentoPct}
                        onChange={e => setDescuentoPct(e.target.value)}
                        className="w-16 h-7 text-xs px-1.5"
                        onClick={e => e.stopPropagation()}
                      />
                      <span>%</span>
                    </div>
                  </div>
                )}
              </div>
            </label>

            {/* 3. Por peso (solo Placa o Platina) */}
            {admitePorPeso && (
              <div className="flex items-start gap-3 p-3 rounded border border-[#E5E7EB] hover:bg-[#F9FAFB]">
                <div className="flex-1 text-sm flex items-center justify-between">
                  <div>
                    <span className="font-semibold text-[#022F40]">Por peso</span>
                    <p className="text-xs text-[#705D56] mt-0.5">Calcula medidas y precio según tarifa por kg</p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      onOpenChange(false)
                      onAbrirCalculadoraPeso()
                    }}
                    className="h-8 text-xs font-medium text-[#022F40]"
                  >
                    Abrir calculadora
                  </Button>
                </div>
              </div>
            )}

            {/* 4. Por metro */}
            <label className="flex items-start gap-3 p-3 rounded border border-[#E5E7EB] hover:bg-[#F9FAFB] cursor-pointer">
              <input
                type="radio"
                name="metodoPrecio"
                value="POR_METRO"
                checked={metodo === 'POR_METRO'}
                onChange={() => setMetodo('POR_METRO')}
                className="mt-1 text-[#022F40]"
              />
              <div className="flex-1 text-sm flex flex-col gap-2.5">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[#022F40]">Por metro</span>
                  {puMetro != null && (
                    <span className="font-semibold text-[#022F40]">{formatCurrency(puMetro)}</span>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2" onClick={e => e.stopPropagation()}>
                  <div className="flex flex-col gap-1">
                    <span className="text-[11px] font-medium text-[#705D56]">Precio vendida (C$)</span>
                    <Input
                      type="number"
                      min="0"
                      step="any"
                      placeholder="Ej. 2700"
                      value={precioRef}
                      onChange={e => setPrecioRef(e.target.value)}
                      className="h-7 text-xs px-2"
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <span className="text-[11px] font-medium text-[#705D56]">Largo vendida (m)</span>
                    <Input
                      type="number"
                      min="0"
                      step="any"
                      placeholder="Ej. 6.75"
                      value={largoRef}
                      onChange={e => setLargoRef(e.target.value)}
                      className="h-7 text-xs px-2"
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <span className="text-[11px] font-medium text-[#705D56]">Largo nuevo (m)</span>
                    <Input
                      type="number"
                      min="0"
                      step="any"
                      placeholder="Ej. 5.40"
                      value={largoNuevo}
                      onChange={e => setLargoNuevo(e.target.value)}
                      className="h-7 text-xs px-2"
                    />
                  </div>
                </div>
              </div>
            </label>

          </div>
        )}

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
            onClick={handleAplicar}
            disabled={!puedeAplicar}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-xs h-9"
          >
            Usar este precio
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
