import { useState, useMemo, useEffect } from 'react'
import type { Consolidado, ConsolidadoGroup, Factura, Cliente } from '@/types'
import { facturaService } from '@/services/facturaService'
import { consolidadoService } from '@/services/consolidadoService'
import { formatCurrency } from '@/lib/format'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { X, Layers, ArrowUp, ArrowDown, Plus, Trash2, CheckSquare, Square } from 'lucide-react'

interface Props {
  clientes: Cliente[]
  open: boolean
  onOpenChange: (v: boolean) => void
  onSave: (c: Omit<Consolidado, 'id' | 'createdAt' | 'folioInterno'>) => void
}

export function NuevoConsolidadoSheet({ clientes, open, onOpenChange, onSave }: Props) {
  const [step, setStep] = useState<1 | 2>(1)
  
  // Step 1 State
  const [clienteId, setClienteId] = useState('')
  const [nombre, setNombre] = useState('')
  const [selectedFacturas, setSelectedFacturas] = useState<string[]>([])

  // Step 2 State
  const [grupos, setGrupos] = useState<ConsolidadoGroup[]>([])

  // Derived
  const [facturasCliente, setFacturasCliente] = useState<Factura[]>([])

  useEffect(() => {
    if (!clienteId) {
      setFacturasCliente([])
      return
    }
    facturaService.listar({ clienteId }).then(res => {
      setFacturasCliente(res.filter(f => f.estado !== 'COTIZACION'))
    })
  }, [clienteId])

  const handleNext = () => {
    if (step === 1) {
      if (!clienteId || !nombre.trim() || selectedFacturas.length === 0) return
      
      // Generate initial groups based on selected invoices
      const initialGrupos: ConsolidadoGroup[] = selectedFacturas.map(fid => {
        const fac = facturasCliente.find(f => f.id === fid)!
        return {
          id: `g-${fid}`,
          nombre: `Factura ${fac.folioInterno}`,
          items: fac.detalles.map(d => ({
            id: `item-${Date.now()}-${Math.random()}`,
            facturaId: fac.id,
            productoNombre: d.productoNombre,
            cantidad: d.cantidad,
            precioUnitario: d.precioUnitarioVenta
          }))
        }
      })
      setGrupos(initialGrupos)
      setStep(2)
    } else {
      const payload = {
        clienteId,
        nombre: nombre.trim(),
        grupos
      }
      onSave(payload)
      reset()
    }
  }

  const reset = () => {
    setStep(1)
    setClienteId('')
    setNombre('')
    setSelectedFacturas([])
    setGrupos([])
    onOpenChange(false)
  }

  const toggleFactura = (id: string) => {
    setSelectedFacturas(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])
  }

  // Group movement
  const moveGroup = (idx: number, dir: -1 | 1) => {
    const newGrupos = [...grupos]
    const temp = newGrupos[idx]
    newGrupos[idx] = newGrupos[idx + dir]
    newGrupos[idx + dir] = temp
    setGrupos(newGrupos)
  }

  const renameGroup = (idx: number, name: string) => {
    const newGrupos = [...grupos]
    newGrupos[idx].nombre = name
    setGrupos(newGrupos)
  }
  
  const addEmptyGroup = () => {
    setGrupos([...grupos, { id: `g-new-${Date.now()}`, nombre: 'Nuevo Subtotal', items: [] }])
  }

  const deleteGroup = (idx: number) => {
    setGrupos(grupos.filter((_, i) => i !== idx))
  }

  // Item movement
  const moveItem = (gIdx: number, iIdx: number, dir: -1 | 1) => {
    const newGrupos = [...grupos]
    const items = [...newGrupos[gIdx].items]
    const temp = items[iIdx]
    items[iIdx] = items[iIdx + dir]
    items[iIdx + dir] = temp
    newGrupos[gIdx].items = items
    setGrupos(newGrupos)
  }

  const deleteItem = (gIdx: number, iIdx: number) => {
    const newGrupos = [...grupos]
    newGrupos[gIdx].items = newGrupos[gIdx].items.filter((_, i) => i !== iIdx)
    setGrupos(newGrupos)
  }

  // Calcs
  const totalGeneral = grupos.reduce((accG, g) => 
    accG + g.items.reduce((accI, item) => accI + (item.cantidad * item.precioUnitario), 0)
  , 0)

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-black/20" onClick={reset} />

      <div className="relative z-10 w-full max-w-[600px] h-full bg-white border-l border-[#E5E7EB] flex flex-col shadow-sm overflow-hidden">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-[#E5E7EB] shrink-0 bg-[#F9FAFB]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center shrink-0">
              <Layers className="w-4.5 h-4.5 text-[#91E5F6]" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-[#022F40] leading-tight">Nuevo Consolidado</h2>
              <p className="text-xs text-[#705D56] mt-0.5">Paso {step} de 2</p>
            </div>
          </div>
          <button onClick={reset} className="w-8 h-8 flex items-center justify-center rounded hover:bg-white text-[#80727B] transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body Step 1 */}
        {step === 1 && (
          <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#705D56]">Nombre del Consolidado *</label>
              <Input value={nombre} onChange={e => setNombre(e.target.value)} placeholder="Ej. Estado de Cuenta Julio" />
            </div>
            
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#705D56]">Cliente *</label>
              <select 
                value={clienteId} 
                onChange={e => { setClienteId(e.target.value); setSelectedFacturas([]) }}
                className="h-10 w-full rounded border border-[#E5E7EB] bg-white px-3 text-sm text-[#022F40] outline-none focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 transition-all"
              >
                <option value="" disabled>Selecciona un cliente</option>
                {clientes.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </div>

            {clienteId && (
              <div className="flex flex-col gap-3">
                <label className="text-xs font-medium text-[#705D56]">Selecciona las facturas a incluir</label>
                {facturasCliente.length === 0 ? (
                  <p className="text-sm text-[#80727B] italic">No hay facturas válidas para este cliente.</p>
                ) : (
                  <div className="flex flex-col gap-2">
                    {facturasCliente.map(f => {
                      const selected = selectedFacturas.includes(f.id)
                      return (
                        <div 
                          key={f.id} 
                          onClick={() => toggleFactura(f.id)}
                          className={`flex items-center justify-between p-3 border rounded cursor-pointer transition-colors ${selected ? 'border-[#022F40] bg-[#022F40]/5' : 'border-[#E5E7EB] hover:bg-[#F9FAFB]'}`}
                        >
                          <div className="flex items-center gap-3">
                            {selected ? <CheckSquare className="w-4 h-4 text-[#022F40]" /> : <Square className="w-4 h-4 text-[#80727B]" />}
                            <div>
                              <p className="text-sm font-semibold text-[#022F40]">{f.folioInterno}</p>
                              <p className="text-xs text-[#705D56]">{f.createdAt}</p>
                            </div>
                          </div>
                          <p className="text-sm font-bold text-[#022F40]">{formatCurrency(f.totalVenta)}</p>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Body Step 2 */}
        {step === 2 && (
          <div className="flex-1 overflow-y-auto bg-[#F9FAFB]">
            <div className="p-6 flex flex-col gap-6">
              
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-[#022F40]">Estructura del Consolidado</h3>
                <Button size="sm" variant="outline" onClick={addEmptyGroup} className="h-7 text-xs bg-white">
                  <Plus className="w-3.5 h-3.5 mr-1" /> Añadir Grupo
                </Button>
              </div>

              <div className="flex flex-col gap-4">
                {grupos.map((g, gIdx) => {
                  const subtotal = g.items.reduce((sum, item) => sum + (item.cantidad * item.precioUnitario), 0)
                  return (
                    <div key={g.id} className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden flex flex-col">
                      {/* Group Header */}
                      <div className="px-3 py-2 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center justify-between gap-3">
                        <div className="flex items-center gap-1 shrink-0">
                          <button onClick={() => moveGroup(gIdx, -1)} disabled={gIdx === 0} className="w-6 h-6 flex items-center justify-center rounded hover:bg-[#E5E7EB] disabled:opacity-30"><ArrowUp className="w-3.5 h-3.5 text-[#022F40]" /></button>
                          <button onClick={() => moveGroup(gIdx, 1)} disabled={gIdx === grupos.length - 1} className="w-6 h-6 flex items-center justify-center rounded hover:bg-[#E5E7EB] disabled:opacity-30"><ArrowDown className="w-3.5 h-3.5 text-[#022F40]" /></button>
                        </div>
                        <Input 
                          value={g.nombre} 
                          onChange={e => renameGroup(gIdx, e.target.value)} 
                          className="h-7 text-sm font-semibold bg-transparent border-transparent focus:bg-white focus:border-[#E5E7EB] px-2 flex-1"
                        />
                        <button onClick={() => deleteGroup(gIdx)} className="w-7 h-7 flex items-center justify-center rounded hover:bg-red-50 text-red-500 transition-colors shrink-0">
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* Items */}
                      <div className="flex flex-col divide-y divide-[#F3F4F6]">
                        {g.items.length === 0 ? (
                          <p className="text-xs text-[#80727B] p-3 text-center italic">Sin productos en este grupo</p>
                        ) : (
                          g.items.map((item, iIdx) => (
                            <div key={item.id} className="p-3 flex items-center gap-3 hover:bg-[#F9FAFB]">
                              <div className="flex flex-col gap-0.5 shrink-0">
                                <button onClick={() => moveItem(gIdx, iIdx, -1)} disabled={iIdx === 0} className="w-4 h-4 flex items-center justify-center rounded hover:bg-[#E5E7EB] disabled:opacity-30"><ArrowUp className="w-3 h-3 text-[#705D56]" /></button>
                                <button onClick={() => moveItem(gIdx, iIdx, 1)} disabled={iIdx === g.items.length - 1} className="w-4 h-4 flex items-center justify-center rounded hover:bg-[#E5E7EB] disabled:opacity-30"><ArrowDown className="w-3 h-3 text-[#705D56]" /></button>
                              </div>
                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-[#022F40] truncate">{item.productoNombre}</p>
                                <p className="text-[10px] text-[#705D56]">{item.cantidad} x {formatCurrency(item.precioUnitario)}</p>
                              </div>
                              <div className="text-right shrink-0">
                                <p className="text-sm font-semibold text-[#022F40]">{formatCurrency(item.cantidad * item.precioUnitario)}</p>
                              </div>
                              <button onClick={() => deleteItem(gIdx, iIdx)} className="w-6 h-6 flex items-center justify-center rounded hover:bg-red-50 text-red-500 transition-colors shrink-0">
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          ))
                        )}
                      </div>

                      {/* Subtotal */}
                      <div className="px-3 py-2 border-t border-[#E5E7EB] bg-[#022F40]/5 flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-[#022F40]">Subtotal {g.nombre}</span>
                        <span className="text-sm font-bold text-[#022F40]">{formatCurrency(subtotal)}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="px-6 py-4 border-t border-[#E5E7EB] bg-white shrink-0 flex items-center justify-between">
          <div>
            {step === 2 && (
              <>
                <p className="text-[10px] uppercase font-bold tracking-wider text-[#80727B]">Total Consolidado</p>
                <p className="text-lg font-bold text-[#022F40] leading-none mt-0.5">{formatCurrency(totalGeneral)}</p>
              </>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={step === 1 ? reset : () => setStep(1)} className="rounded border-[#E5E7EB] text-sm h-9">
              {step === 1 ? 'Cancelar' : 'Atrás'}
            </Button>
            <Button 
              onClick={handleNext} 
              disabled={step === 1 && (!clienteId || !nombre.trim() || selectedFacturas.length === 0)}
              className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white text-sm h-9 gap-1"
            >
              {step === 1 ? 'Siguiente' : 'Guardar Consolidado'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
