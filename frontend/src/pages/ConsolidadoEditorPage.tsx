import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import type { Cliente, Proyecto, Consolidado } from '@/types'
import { clienteService } from '@/services/clienteService'
import { proyectoService } from '@/services/proyectoService'
import { consolidadoService } from '@/services/consolidadoService'
import { formatCurrency, formatDate, formatUsd, hoyIso } from '@/lib/format'
import { nombreArchivo } from '@/lib/exportar'
import { ExportarDialog } from '@/components/export/ExportarDialog'
import { ConsolidadoPlantilla } from '@/components/export/ConsolidadoPlantilla'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useAvisoSalir, SALIR_SIN_GUARDAR } from '@/hooks/useAvisoSalir'
import {
  ArrowLeft,
  Save,
  Search,
  AlertCircle,
  FileDown,
} from 'lucide-react'

export function ConsolidadoEditorPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [clientes, setClientes] = useState<Cliente[]>([])
  const [proyectos, setProyectos] = useState<Proyecto[]>([])

  // Formulario
  const [clienteId, setClienteId] = useState('')
  const [proyectoId, setProyectoId] = useState('')
  const [desde, setDesde] = useState(() => {
    const ahora = new Date()
    const mes = String(ahora.getMonth() + 1).padStart(2, '0')
    return `${ahora.getFullYear()}-${mes}-01`
  })
  const [hasta, setHasta] = useState(hoyIso())
  const [nombreConsolidado, setNombreConsolidado] = useState('')
  const [mostrarTotalUsd, setMostrarTotalUsd] = useState(false)

  // Nombres editados recordados por fechaEntrega
  const [nombresGrupos, setNombresGrupos] = useState<Record<string, string>>({})

  // Estado consolidado
  const [consolidado, setConsolidado] = useState<Consolidado | null>(null)
  const [cargando, setCargando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [exportando, setExportando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cambiosSinGuardar, setCambiosSinGuardar] = useState(false)

  useAvisoSalir(cambiosSinGuardar)

  const isGuardado = !!consolidado?.id

  // Carga inicial de clientes
  useEffect(() => {
    clienteService.listar().then(setClientes).catch(console.error)
  }, [])

  // Cargar proyectos cuando cambia cliente
  useEffect(() => {
    if (!clienteId) {
      setProyectos([])
      return
    }
    proyectoService.listar(clienteId).then(setProyectos).catch(console.error)
  }, [clienteId])

  // Cargar consolidado si viene id en la URL
  const cargarConsolidado = useCallback(async (consId: string) => {
    try {
      setCargando(true)
      setError(null)
      const data = await consolidadoService.obtener(consId)
      setConsolidado(data)
      setClienteId(data.clienteId)
      setProyectoId(data.proyectoId ?? '')
      setDesde(data.desde)
      setHasta(data.hasta)
      setNombreConsolidado(data.nombre)
      setMostrarTotalUsd(Boolean(data.mostrarTotalUsd))

      const inicialesNombres: Record<string, string> = {}
      data.grupos.forEach(g => {
        if (g.editado) {
          inicialesNombres[g.fechaEntrega] = g.nombre
        }
      })
      setNombresGrupos(inicialesNombres)
      setCambiosSinGuardar(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar el consolidado')
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    if (id) {
      cargarConsolidado(id)
    } else {
      setConsolidado(null)
      setClienteId('')
      setProyectoId('')
      setNombreConsolidado('')
      setMostrarTotalUsd(false)
      setNombresGrupos({})
      setCambiosSinGuardar(false)
    }
  }, [id, cargarConsolidado])

  // Botón "Ver consolidado" (Vista previa)
  const handleVerConsolidado = async () => {
    if (!clienteId) {
      setError('Selecciona un cliente')
      return
    }
    if (desde > hasta) {
      setError('La fecha inicial no puede ser posterior a la final')
      return
    }

    try {
      setCargando(true)
      setError(null)
      const vista = await consolidadoService.vistaPrevia({
        clienteId,
        proyectoId: proyectoId || null,
        desde,
        hasta,
      })

      setConsolidado(prev => ({
        ...vista,
        id: prev?.id ?? null,
        folio: prev?.folio ?? null,
      }))

      if (!nombreConsolidado.trim()) {
        const c = clientes.find(cl => cl.id === clienteId)
        setNombreConsolidado(c ? `Consolidado ${c.nombre}` : 'Consolidado')
      }
      setCambiosSinGuardar(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al generar la vista previa')
    } finally {
      setCargando(false)
    }
  }

  // Editar nombre de subtotal
  const handleNombreGrupoChange = (fechaEntrega: string, nuevoNombre: string) => {
    setNombresGrupos(prev => ({
      ...prev,
      [fechaEntrega]: nuevoNombre,
    }))
    setCambiosSinGuardar(true)
  }

  // Guardar consolidado
  const handleGuardar = async () => {
    if (!clienteId) {
      setError('Selecciona un cliente')
      return
    }
    if (!nombreConsolidado.trim()) {
      setError('Escribe un nombre para el consolidado')
      return
    }
    if (desde > hasta) {
      setError('La fecha inicial no puede ser posterior a la final')
      return
    }

    setGuardando(true)
    setError(null)
    try {
      const gruposPayload = Object.entries(nombresGrupos)
        .filter(([, nombre]) => nombre.trim() !== '')
        .map(([fechaEntrega, nombre]) => ({
          fechaEntrega,
          nombre: nombre.trim(),
        }))

      const payload = {
        clienteId,
        proyectoId: proyectoId || null,
        nombre: nombreConsolidado.trim(),
        desde,
        hasta,
        grupos: gruposPayload,
        mostrarTotalUsd,
      }

      if (isGuardado && consolidado?.id) {
        const actualizado = await consolidadoService.actualizar(consolidado.id, payload)
        setConsolidado(actualizado)
        const inicialesNombres: Record<string, string> = {}
        actualizado.grupos.forEach(g => {
          if (g.editado) {
            inicialesNombres[g.fechaEntrega] = g.nombre
          }
        })
        setNombresGrupos(inicialesNombres)
        setCambiosSinGuardar(false)
      } else {
        const nuevo = await consolidadoService.crear(payload)
        setCambiosSinGuardar(false)
        navigate(`/consolidados/${nuevo.id}`)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar consolidado')
    } finally {
      setGuardando(false)
    }
  }

  const volver = () => {
    if (!cambiosSinGuardar || window.confirm(SALIR_SIN_GUARDAR)) {
      navigate('/consolidados')
    }
  }

  return (
    <div className="p-4 md:p-8 flex flex-col gap-6 max-w-5xl mx-auto">
      {/* ── Breadcrumb ── */}
      <div className="flex items-center justify-between">
        <button
          onClick={volver}
          className="inline-flex items-center min-h-11 gap-1.5 text-sm text-[#705D56] hover:text-[#022F40] transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" /> Volver a Consolidados
        </button>

        {isGuardado && (
          <div className="flex items-center gap-2">
            <span title={cambiosSinGuardar ? 'Guarda primero los cambios' : undefined}>
              <Button
                type="button"
                variant="outline"
                disabled={cambiosSinGuardar}
                onClick={() => setExportando(true)}
                className="rounded border-[#E5E7EB] gap-1.5"
              >
                <FileDown className="w-4 h-4 text-[#022F40]" />
                Exportar
              </Button>
            </span>
          </div>
        )}
      </div>

      {/* ── Error Banner ── */}
      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* ── 1. Formulario de Parámetros ── */}
      <div className="bg-white border border-[#E5E7EB] rounded p-5 shadow-sm flex flex-col gap-4">
        <h2 className="text-sm font-semibold text-[#022F40]">
          {isGuardado ? `Editar consolidado ${consolidado?.folio ?? ''}` : 'Nuevo consolidado'}
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          {/* Cliente */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Cliente *</label>
            <select
              value={clienteId}
              onChange={e => {
                setClienteId(e.target.value)
                setProyectoId('')
                setConsolidado(null)
                setNombreConsolidado('')
                setNombresGrupos({})
                setCambiosSinGuardar(true)
              }}
              disabled={isGuardado}
              className="w-full h-11 rounded border border-[#E5E7EB] bg-white px-3 text-base text-[#022F40] outline-none focus:border-[#022F40] disabled:bg-[#F9FAFB] disabled:text-[#80727B]"
            >
              <option value="">Selecciona cliente…</option>
              {clientes.map(c => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Proyecto */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Proyecto</label>
            <select
              value={proyectoId}
              onChange={e => {
                setProyectoId(e.target.value)
                setConsolidado(null)
                setNombreConsolidado('')
                setNombresGrupos({})
                setCambiosSinGuardar(true)
              }}
              disabled={isGuardado}
              className="w-full h-11 rounded border border-[#E5E7EB] bg-white px-3 text-base text-[#022F40] outline-none focus:border-[#022F40] disabled:bg-[#F9FAFB] disabled:text-[#80727B]"
            >
              <option value="">Todos los proyectos</option>
              {proyectos.map(p => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Desde */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Desde *</label>
            <Input
              type="date"
              value={desde}
              onChange={e => {
                setDesde(e.target.value)
                if (!isGuardado) {
                  setConsolidado(null)
                }
                setCambiosSinGuardar(true)
              }}
            />
          </div>

          {/* Hasta */}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#705D56]">Hasta *</label>
            <Input
              type="date"
              value={hasta}
              onChange={e => {
                setHasta(e.target.value)
                if (!isGuardado) {
                  setConsolidado(null)
                }
                setCambiosSinGuardar(true)
              }}
            />
          </div>
        </div>

        <div className="flex justify-end pt-2">
          <Button
            type="button"
            onClick={handleVerConsolidado}
            disabled={cargando || !clienteId}
            className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white gap-1.5"
          >
            <Search className="w-4 h-4" />
            {cargando ? 'Cargando…' : 'Ver consolidado'}
          </Button>
        </div>
      </div>

      {/* ── 2. Vista de Grupos y Líneas ── */}
      {consolidado && (
        <div className="flex flex-col gap-5">
          {consolidado.grupos.length === 0 ? (
            <div className="bg-white border border-[#E5E7EB] rounded p-8 text-center text-sm text-[#705D56] shadow-sm">
              No hay facturas activas para el cliente en el rango de fechas seleccionado.
            </div>
          ) : (
            consolidado.grupos.map((g, idx) => {
              const nombreMostrado = nombresGrupos[g.fechaEntrega] ?? g.nombre

              return (
                <div key={g.fechaEntrega} className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden">
                  {/* Barra de Subtotal editable */}
                  <div className="px-5 py-3 bg-[#F9FAFB] border-b border-[#E5E7EB] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-2 flex-1 max-w-md">
                      <span className="text-sm text-[#80727B] font-mono whitespace-nowrap">Grupo {idx + 1}:</span>
                      <Input
                        value={nombreMostrado}
                        onChange={e => handleNombreGrupoChange(g.fechaEntrega, e.target.value)}
                        placeholder="Nombre del subtotal…"
                        className="font-semibold text-[#022F40] bg-white"
                      />
                    </div>
                    <div className="flex items-center gap-4 text-sm font-semibold">
                      <span className="text-[#705D56]">
                        Kilos: <strong className="text-[#022F40]">{(g.subtotalKg ?? 0).toFixed(2)} kg</strong>
                      </span>
                      <span className="text-[#705D56]">
                        Subtotal: <strong className="text-[#022F40]">{formatCurrency(g.subtotal)}</strong>
                      </span>
                    </div>
                  </div>

                  {/* Tabla de líneas */}
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-[#E5E7EB] bg-white text-[#705D56]">
                          <th className="text-left px-4 py-2 font-medium">Folio</th>
                          <th className="text-right px-4 py-2 font-medium">Cant</th>
                          <th className="text-left px-4 py-2 font-medium">Descripción</th>
                          <th className="text-right px-4 py-2 font-medium">P. Unit (KG)</th>
                          <th className="text-right px-4 py-2 font-medium">P. Total (KG)</th>
                          <th className="text-right px-4 py-2 font-medium">P. Unit C$</th>
                          <th className="text-right px-4 py-2 font-medium">Total C$</th>
                          <th className="text-left px-4 py-2 font-medium">Lugar entrega</th>
                        </tr>
                      </thead>
                      <tbody>
                        {g.lineas.map((l, lIdx) => (
                          <tr
                            key={`${l.documentoId}-${lIdx}`}
                            className="border-b border-[#E5E7EB] last:border-b-0 hover:bg-[#F9FAFB] transition-colors"
                          >
                            <td className="px-4 py-2 font-mono font-semibold text-[#022F40]">
                              <Link to={`/documentos/${l.documentoId}`} className="underline underline-offset-2">
                                {l.folio}
                              </Link>
                            </td>
                            <td className="px-4 py-2 text-right">{l.cantidad}</td>
                            <td className="px-4 py-2 text-[#022F40]">{l.descripcion}</td>
                            <td className="px-4 py-2 text-right text-[#705D56]">
                              {l.pesoUnitKg != null ? l.pesoUnitKg.toFixed(2) : '—'}
                            </td>
                            <td className="px-4 py-2 text-right text-[#705D56]">
                              {l.pesoTotalKg != null ? l.pesoTotalKg.toFixed(2) : '—'}
                            </td>
                            <td className="px-4 py-2 text-right">{formatCurrency(l.precioUnit)}</td>
                            <td className="px-4 py-2 text-right font-semibold text-[#022F40]">
                              {formatCurrency(l.total)}
                            </td>
                            <td className="px-4 py-2 text-[#705D56]">{l.lugarEntrega || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )
            })
          )}

          {/* ── 3. Totales & Abonos del Período ── */}
          <div className="bg-white border border-[#E5E7EB] rounded p-6 shadow-sm flex flex-col md:flex-row justify-between gap-6">
            {/* Abonos del período */}
            <div className="flex-1 flex flex-col gap-2">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-[#80727B]">
                Abonos en el período ({consolidado.abonos.length})
              </h3>
              {consolidado.abonos.length === 0 ? (
                <p className="text-sm text-[#80727B]">No hubo abonos registrados en este rango.</p>
              ) : (
                <div className="border border-[#E5E7EB] rounded overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="bg-[#F9FAFB] border-b border-[#E5E7EB] text-[#705D56]">
                        <th className="text-left px-3 py-1.5 font-medium">Fecha</th>
                        <th className="text-right px-3 py-1.5 font-medium">Monto</th>
                        <th className="text-left px-3 py-1.5 font-medium">Nota</th>
                      </tr>
                    </thead>
                    <tbody>
                      {consolidado.abonos.map((a, aIdx) => (
                        <tr key={aIdx} className="border-b border-[#E5E7EB] last:border-b-0">
                          <td className="px-3 py-1.5 text-[#705D56]">{formatDate(a.fecha)}</td>
                          <td className="px-3 py-1.5 text-right font-semibold text-[#558564]">
                            {formatCurrency(a.monto)}
                          </td>
                          <td className="px-3 py-1.5 text-[#80727B]">{a.nota || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Cuadro de Totales */}
            <div className="w-full md:w-80 flex flex-col gap-2 border-t md:border-t-0 md:border-l border-[#E5E7EB] md:pl-6 pt-4 md:pt-0">
              <div className="flex justify-between text-sm py-1">
                <span className="text-[#705D56]">Total C$:</span>
                <span className="font-bold text-[#022F40]">{formatCurrency(consolidado.total)}</span>
              </div>
              {mostrarTotalUsd && (
                <div className="flex justify-between text-sm py-1">
                  <span className="text-[#705D56]">Total U$:</span>
                  <span className="font-bold text-[#022F40]">
                    {formatUsd(consolidado.totalUsd ?? (consolidado.tipoCambio ? Math.round(((consolidado.total / consolidado.tipoCambio) + Number.EPSILON) * 100) / 100 : 0))}
                  </span>
                </div>
              )}
              <div className="flex justify-between text-sm py-1">
                <span className="text-[#705D56]">Total KG:</span>
                <span className="font-bold text-[#022F40]">{(consolidado.totalKg ?? 0).toFixed(2)} kg</span>
              </div>

              {consolidado.saldoAnterior !== 0 && (
                <div className="flex justify-between text-sm py-1 border-t border-[#E5E7EB]">
                  <span className="text-[#705D56]">Saldo anterior:</span>
                  <span className="font-bold text-[#022F40]">
                    {formatCurrency(consolidado.saldoAnterior)}
                  </span>
                </div>
              )}

              {consolidado.totalAbonos !== 0 && (
                <div className="flex justify-between text-sm py-1">
                  <span className="text-[#705D56]">Menos abonos:</span>
                  <span className="font-bold text-[#558564]">
                    −{formatCurrency(consolidado.totalAbonos)}
                  </span>
                </div>
              )}

              {(consolidado.saldoAnterior !== 0 || consolidado.totalAbonos !== 0) && (
                <>
                  <div className="flex justify-between text-sm py-2 border-t border-[#022F40]/10 bg-[#F9FAFB] px-2 rounded">
                    <span className="font-semibold text-[#022F40]">Total actualizado C$:</span>
                    <span className="font-extrabold text-[#022F40]">
                      {formatCurrency(consolidado.totalActualizado)}
                    </span>
                  </div>
                  {mostrarTotalUsd && (
                    <div className="flex justify-between text-sm py-2 bg-[#F9FAFB] px-2 rounded">
                      <span className="font-semibold text-[#022F40]">Total actualizado U$:</span>
                      <span className="font-extrabold text-[#022F40]">
                        {formatUsd(consolidado.totalActualizadoUsd ?? (consolidado.tipoCambio ? Math.round(((consolidado.totalActualizado / consolidado.tipoCambio) + Number.EPSILON) * 100) / 100 : 0))}
                      </span>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

          {/* ── 4. Nombre del Consolidado & Guardar ── */}
          <div className="bg-white border border-[#E5E7EB] rounded p-5 shadow-sm flex flex-col gap-4">
            <label className="flex items-center gap-2 cursor-pointer text-sm text-[#705D56]">
              <input
                type="checkbox"
                checked={mostrarTotalUsd}
                onChange={e => {
                  setMostrarTotalUsd(e.target.checked)
                  setCambiosSinGuardar(true)
                }}
              />
              Mostrar total en dólares
            </label>

            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex flex-col gap-1.5 w-full sm:max-w-md">
                <label className="text-sm font-medium text-[#705D56]">Nombre del consolidado *</label>
                <Input
                  value={nombreConsolidado}
                  onChange={e => {
                    setNombreConsolidado(e.target.value)
                    setCambiosSinGuardar(true)
                  }}
                  placeholder="Ej. Consolidado Constructora Norte SA"
                />
              </div>

              <Button
                onClick={handleGuardar}
                disabled={guardando || !nombreConsolidado.trim()}
                className="rounded bg-[#022F40] hover:bg-[#022F40]/90 text-white gap-1.5 w-full sm:w-auto"
              >
                <Save className="w-4 h-4" />
                {guardando ? 'Guardando…' : isGuardado ? 'Guardar cambios' : 'Guardar consolidado'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {consolidado && isGuardado && (
        <ExportarDialog
          open={exportando}
          onOpenChange={setExportando}
          encabezadoInicial={
            consolidado.proyectoNombre
              ? `${consolidado.clienteNombre} / ${consolidado.proyectoNombre}`
              : consolidado.clienteNombre
          }
          nombreBase={(ext) =>
            nombreArchivo(
              consolidado.folio ?? 'CONS',
              consolidado.clienteNombre,
              consolidado.hasta,
              ext
            )
          }
          horizontal={consolidado.grupos.some((g) =>
            g.lineas.some((l) => l.lugarEntrega && l.lugarEntrega.trim() !== '')
          )}
          plantilla={(encabezado) => (
            <ConsolidadoPlantilla consolidado={consolidado} encabezado={encabezado} />
          )}
          descargarExcel={(enc) => consolidadoService.descargarExcel(consolidado.id!, enc)}
        />
      )}
    </div>
  )
}
