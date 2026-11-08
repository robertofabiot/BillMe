import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import type { Cliente, ConfiguracionSistema, Documento, MetodoPrecio, PrecioSugerido, Producto, Proyecto, TipoDocumento } from '@/types'
import { clienteService } from '@/services/clienteService'
import { configuracionService } from '@/services/configuracionService'
import { documentoService, type DocumentoPayload } from '@/services/documentoService'
import { precioService } from '@/services/precioService'
import { proyectoService } from '@/services/proyectoService'
import { formatCurrency, formatFechaCorta, formatUsd, hoyIso } from '@/lib/format'
import { ExportarDialog } from '@/components/export/ExportarDialog'
import { DocumentoPlantilla } from '@/components/export/DocumentoPlantilla'
import { nombreArchivo } from '@/lib/exportar'
import { EstadoDocumentoBadge } from '@/components/documentos/EstadoDocumentoBadge'
import { LineasTabla } from '@/components/documentos/LineasTabla'
import { calcularLinea, r2, recalcularPrecioPorPeso, type LineaForm } from '@/components/documentos/lineaForm'
import { ProductoBuscador } from '@/components/documentos/ProductoBuscador'
import { RevisionesDialog } from '@/components/documentos/RevisionesDialog'
import { PrecioLineaDialog } from '@/components/documentos/PrecioLineaDialog'
import { CalculadoraPesoDialog } from '@/components/documentos/CalculadoraPesoDialog'
import { RegistrarCompraDialog } from '@/components/documentos/RegistrarCompraDialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useAvisoSalir, SALIR_SIN_GUARDAR } from '@/hooks/useAvisoSalir'
import { ArrowLeft, FileDown, FileText, History, Plus, ReceiptText, Save, ShoppingCart } from 'lucide-react'

interface Formulario {
  clienteId: string
  proyectoId: string
  fecha: string
  notas: string
  mostrarEntrega: boolean
  mostrarTotalUsd: boolean
  lineas: LineaForm[]
}

const TITULOS: Record<TipoDocumento, { nuevo: string; lista: string; ruta: string }> = {
  COTIZACION: { nuevo: 'Nueva cotización', lista: 'Cotizaciones', ruta: '/cotizaciones' },
  FACTURA: { nuevo: 'Nueva factura', lista: 'Facturas', ruta: '/' },
  HISTORICO: { nuevo: 'Histórico', lista: 'Precios', ruta: '/precios' },
}

const SELECT = 'h-11 w-full rounded border border-[#E5E7EB] bg-white px-3 text-base text-[#022F40] outline-none focus:border-[#022F40] disabled:opacity-60'

function formularioVacio(): Formulario {
  return { clienteId: '', proyectoId: '', fecha: hoyIso(), notas: '', mostrarEntrega: false, mostrarTotalUsd: false, lineas: [] }
}

function formularioDe(d: Documento): Formulario {
  return {
    clienteId: d.clienteId,
    proyectoId: d.proyectoId ?? '',
    fecha: d.fecha,
    notas: d.notas ?? '',
    mostrarEntrega: d.mostrarEntrega,
    mostrarTotalUsd: Boolean(d.mostrarTotalUsd),
    lineas: d.lineas.map(l => ({
      clave: l.id,
      id: l.id,
      productoId: l.productoId,
      productoCodigo: l.productoCodigo,
      productoGenerico: l.productoGenerico,
      productoForma: l.productoForma ?? 'NINGUNA',
      descripcion: l.descripcion,
      cantidad: String(l.cantidad),
      pesoUnitKg: l.pesoUnitKg != null ? String(l.pesoUnitKg) : '',
      precioUnit: String(l.precioUnit),
      metodoPrecio: l.metodoPrecio,
      parametros: l.parametros,
      fechaEntrega: l.fechaEntrega ?? '',
      lugarEntrega: l.lugarEntrega ?? '',
    })),
  }
}

/** Revisa lo que el backend no puede explicar tan claro; devuelve el primer problema. */
function problemaDe(f: Formulario): string | null {
  if (!f.clienteId) return 'Elige el cliente.'
  if (!f.fecha) return 'Indica la fecha.'
  if (f.lineas.length === 0) return 'Agrega al menos un producto.'
  for (const [i, l] of f.lineas.entries()) {
    const n = i + 1
    if (!(Number(l.cantidad) > 0)) return `Falta la cantidad en la línea ${n}.`
    if (l.precioUnit.trim() === '' || Number(l.precioUnit) < 0) return `Falta el precio en la línea ${n}.`
    if (l.productoGenerico && !l.descripcion.trim()) return `Escribe la descripción de la línea ${n}.`
  }
  return null
}

function payloadDe(f: Formulario, tipo: TipoDocumento): DocumentoPayload {
  return {
    tipo,
    clienteId: f.clienteId,
    proyectoId: f.proyectoId || null,
    fecha: f.fecha,
    notas: f.notas.trim() || null,
    mostrarEntrega: f.mostrarEntrega,
    mostrarTotalUsd: f.mostrarTotalUsd,
    lineas: f.lineas.map(l => ({
      id: l.id,
      productoId: l.productoId,
      descripcion: l.descripcion.trim(),
      cantidad: Number(l.cantidad),
      pesoUnitKg: l.pesoUnitKg.trim() === '' ? null : Number(l.pesoUnitKg),
      precioUnit: Number(l.precioUnit),
      metodoPrecio: l.metodoPrecio,
      parametros: l.parametros,
      fechaEntrega: l.fechaEntrega || null,
      lugarEntrega: l.lugarEntrega.trim() || null,
    })),
  }
}

type Aviso = { tipo: 'ok' | 'error'; texto: string } | null

function precioDeSugerido(res: PrecioSugerido): {
  precioUnit: string
  metodoPrecio: MetodoPrecio
  parametros: string | null
} | null {
  if (res.precioUnit != null) {
    return {
      precioUnit: String(res.precioUnit),
      metodoPrecio: 'ULTIMO_PRECIO',
      parametros: JSON.stringify({
        folio: res.folio,
        fecha: res.fecha,
        clienteNombre: res.clienteNombre,
        otroCliente: res.origen === 'OTRO_CLIENTE',
        dias: res.diasAntiguedad,
        viejo: res.viejo,
      }),
    }
  }
  if (res.precioFormula != null) {
    return {
      precioUnit: String(res.precioFormula),
      metodoPrecio: 'FORMULA_LISTA',
      parametros: JSON.stringify({
        lista: res.precioLista,
        descuentoPct: res.descuentoPct,
      }),
    }
  }
  return null
}

/** Crea o edita una cotización o factura. */
export function DocumentoEditorPage() {
  const { id } = useParams()
  const [parametros] = useSearchParams()
  const navigate = useNavigate()

  const [documento, setDocumento] = useState<Documento | null>(null)
  const [form, setForm] = useState<Formulario>(formularioVacio)
  const guardadoRef = useRef('')
  // Al crear ya tenemos el documento: la ruta cambia pero no hace falta pedirlo otra vez
  const recienCreadoRef = useRef<string | null>(null)
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [proyectos, setProyectos] = useState<Proyecto[]>([])
  const [nuevoProyecto, setNuevoProyecto] = useState<string | null>(null)
  const [verEntrega, setVerEntrega] = useState(false)
  const [claveNueva, setClaveNueva] = useState<string | null>(null)
  const [cargando, setCargando] = useState(Boolean(id))
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState<Aviso>(null)
  const [exportando, setExportando] = useState(false)
  const [viendoCambios, setViendoCambios] = useState(false)
  const [registrandoCompra, setRegistrandoCompra] = useState(false)
  const [tipoCambioConfig, setTipoCambioConfig] = useState<number | null>(null)
  const [config, setConfig] = useState<ConfiguracionSistema | null>(null)
  const [lineaParaPrecio, setLineaParaPrecio] = useState<string | null>(null)
  const [lineaParaCalculadora, setLineaParaCalculadora] = useState<string | null>(null)
  const [modoCalculadoraSoloPeso, setModoCalculadoraSoloPeso] = useState(false)

  const tipoCambio = documento?.tipoCambio ?? tipoCambioConfig

  const lineaActivaPrecio = useMemo(
    () => form.lineas.find(l => l.clave === lineaParaPrecio) ?? null,
    [form.lineas, lineaParaPrecio],
  )
  const lineaActivaCalculadora = useMemo(
    () => form.lineas.find(l => l.clave === lineaParaCalculadora) ?? null,
    [form.lineas, lineaParaCalculadora],
  )

  const tipo: TipoDocumento = documento?.tipo
    ?? (parametros.get('tipo') === 'COTIZACION' ? 'COTIZACION' : 'FACTURA')
  const titulos = TITULOS[tipo]
  const clienteActual = clientes.find(c => c.id === form.clienteId)

  const mostrar = useCallback((d: Documento) => {
    const f = formularioDe(d)
    setDocumento(d)
    setForm(f)
    guardadoRef.current = JSON.stringify(f)
    setVerEntrega(d.mostrarEntrega || d.lineas.some(l => l.fechaEntrega || l.lugarEntrega))
  }, [])

  useEffect(() => {
    clienteService.listar().then(setClientes).catch(console.error)
    configuracionService.obtener().then(c => {
      setConfig(c)
      setTipoCambioConfig(c.tipoCambio)
    }).catch(console.error)
  }, [])

  useEffect(() => {
    if (id && id === recienCreadoRef.current) {
      recienCreadoRef.current = null
      return
    }
    setAviso(null)
    setClaveNueva(null)
    if (!id) {
      const f = formularioVacio()
      setDocumento(null)
      setForm(f)
      guardadoRef.current = JSON.stringify(f)
      setVerEntrega(false)
      setCargando(false)
      return
    }
    setCargando(true)
    documentoService.obtener(id)
      .then(mostrar)
      .catch(e => setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo cargar el documento' }))
      .finally(() => setCargando(false))
  }, [id, mostrar])

  useEffect(() => {
    if (!form.clienteId) {
      setProyectos([])
      return
    }
    proyectoService.listar(form.clienteId).then(setProyectos).catch(console.error)
  }, [form.clienteId])

  const cambiosSinGuardar = JSON.stringify(form) !== guardadoRef.current
  const soloLectura = documento?.estado === 'ACEPTADA' || documento?.estado === 'ANULADA' || documento?.estado === 'HISTORICA'

  const abrirExportar = () => {
    if (cambiosSinGuardar && !window.confirm('Se exportará la última versión guardada. ¿Continuar?')) {
      return
    }
    setExportando(true)
  }

  // Que un clic en el menú o cerrar la pestaña no pierda lo escrito
  useAvisoSalir(cambiosSinGuardar)

  const proyectosVisibles = useMemo(
    () => proyectos.filter(p => p.estado === 'ACTIVO' || p.id === form.proyectoId),
    [proyectos, form.proyectoId],
  )

  const totales = useMemo(() => form.lineas.reduce((acum, l) => {
    const { total, pesoTotal } = calcularLinea(l)
    return { total: acum.total + total, kilos: acum.kilos + (pesoTotal ?? 0) }
  }, { total: 0, kilos: 0 }), [form.lineas])

  const PREGUNTA_CAMBIO_CLIENTE =
    '¿Recalcular los precios para el nuevo cliente?\n\n' +
    'Aceptar: recalcula los precios que salieron del historial, la lista o el peso.\n' +
    'Cancelar: conserva los precios escritos.'

  const agregarProducto = async (p: Producto) => {
    const clave = `nueva-${Date.now()}-${Math.random()}`
    setClaveNueva(clave)

    const forma = (p.formaCalculo || 'NINGUNA') as 'NINGUNA' | 'PLACA' | 'PLATINA' | 'ANGULAR'
    const esPlacaOPlatinaGenerica = p.generico && (forma === 'PLACA' || forma === 'PLATINA')
    const esLibre = p.codigoInterno === 'GEN-LIBRE'

    let precioUnit = ''
    let metodoPrecio: MetodoPrecio = 'MANUAL'
    let parametros: string | null = null

    if (!esPlacaOPlatinaGenerica && !esLibre && form.clienteId) {
      try {
        const res = await precioService.sugerido(form.clienteId, p.id)
        const sugerido = precioDeSugerido(res)
        if (sugerido) {
          precioUnit = sugerido.precioUnit
          metodoPrecio = sugerido.metodoPrecio
          parametros = sugerido.parametros
        }
      } catch {
        // Si la consulta falla, el precio queda vacío, sin mensaje de error
      }
    }

    setForm(f => ({
      ...f,
      lineas: [...f.lineas, {
        clave,
        productoId: p.id,
        productoCodigo: p.codigoInterno,
        productoGenerico: p.generico,
        productoForma: forma,
        descripcion: p.generico ? '' : p.nombrePrincipal,
        cantidad: '1',
        pesoUnitKg: p.peso ? String(p.peso) : '',
        precioUnit,
        metodoPrecio,
        parametros,
        fechaEntrega: '',
        lugarEntrega: '',
      }],
    }))

    if (esPlacaOPlatinaGenerica) {
      setModoCalculadoraSoloPeso(false)
      setLineaParaCalculadora(clave)
    }
  }

  const alSalirDescripcion = async (clave: string) => {
    const l = form.lineas.find(x => x.clave === clave)
    if (!l || !form.clienteId) return
    if (l.productoCodigo === 'GEN-LIBRE' && l.descripcion.trim() && !l.precioUnit.trim()) {
      try {
        const res = await precioService.sugerido(form.clienteId, l.productoId, l.descripcion.trim())
        const sugerido = precioDeSugerido(res)
        if (sugerido) {
          cambiarLinea(clave, {
            precioUnit: sugerido.precioUnit,
            metodoPrecio: sugerido.metodoPrecio,
            parametros: sugerido.parametros,
          })
        }
      } catch {
        // Silencio en error
      }
    }
  }

  const alCambiarCliente = async (clienteNuevo: string) => {
    if (clienteNuevo === form.clienteId) return

    if (form.lineas.length === 0) {
      setNuevoProyecto(null)
      setForm(f => ({ ...f, clienteId: clienteNuevo, proyectoId: '' }))
      return
    }

    const aceptar = clienteNuevo ? window.confirm(PREGUNTA_CAMBIO_CLIENTE) : false

    setNuevoProyecto(null)

    if (!aceptar) {
      setForm(f => ({
        ...f,
        clienteId: clienteNuevo,
        proyectoId: '',
        lineas: f.lineas.map(l => {
          if (l.metodoPrecio === 'ULTIMO_PRECIO' || l.metodoPrecio === 'FORMULA_LISTA') {
            return { ...l, metodoPrecio: 'MANUAL' as MetodoPrecio, parametros: null }
          }
          return l
        }),
      }))
      return
    }

    const lineasActualizadas: LineaForm[] = await Promise.all(
      form.lineas.map(async (l): Promise<LineaForm> => {
        if (l.metodoPrecio === 'ULTIMO_PRECIO' || l.metodoPrecio === 'FORMULA_LISTA') {
          try {
            const res = await precioService.sugerido(
              clienteNuevo,
              l.productoId,
              l.productoGenerico ? l.descripcion : undefined,
            )
            const sugerido = precioDeSugerido(res)
            if (sugerido) {
              return {
                ...l,
                precioUnit: sugerido.precioUnit,
                metodoPrecio: sugerido.metodoPrecio,
                parametros: sugerido.parametros,
              }
            }
          } catch {
            // Silencio en error
          }
          return { ...l, metodoPrecio: 'MANUAL' as MetodoPrecio, parametros: null }
        }

        if (l.metodoPrecio === 'POR_PESO') {
          try {
            const res = await precioService.sugerido(
              clienteNuevo,
              l.productoId,
              l.productoGenerico ? l.descripcion : undefined,
            )
            let paramsObj: Record<string, any> = {}
            try {
              if (l.parametros) paramsObj = JSON.parse(l.parametros)
            } catch {
              // Parámetros previos no parseables
            }
            if (res.tarifaKg != null) paramsObj.tarifa = res.tarifaKg
            if (res.monedaTarifa != null) paramsObj.moneda = res.monedaTarifa
            const nuevosParams = JSON.stringify(paramsObj)
            const nuevoPrecio = tipoCambio && config
              ? recalcularPrecioPorPeso('POR_PESO', nuevosParams, l.pesoUnitKg, tipoCambio, config.ivaPct)
              : null
            return {
              ...l,
              parametros: nuevosParams,
              precioUnit: nuevoPrecio ?? l.precioUnit,
            }
          } catch {
            return l
          }
        }

        return l
      }),
    )

    setForm(f => ({
      ...f,
      clienteId: clienteNuevo,
      proyectoId: '',
      lineas: lineasActualizadas,
    }))
  }

  const cambiarLinea = (clave: string, cambios: Partial<LineaForm>) =>
    setForm(f => ({ ...f, lineas: f.lineas.map(l => (l.clave === clave ? { ...l, ...cambios } : l)) }))

  const cambiarPeso = (clave: string, pesoUnitKg: string) => {
    const linea = form.lineas.find(l => l.clave === clave)
    const precio = linea && tipoCambio && config
      ? recalcularPrecioPorPeso(linea.metodoPrecio, linea.parametros, pesoUnitKg, tipoCambio, config.ivaPct)
      : null
    cambiarLinea(clave, precio ? { pesoUnitKg, precioUnit: precio } : { pesoUnitKg })
  }

  const quitarLinea = (clave: string) =>
    setForm(f => ({ ...f, lineas: f.lineas.filter(l => l.clave !== clave) }))

  const crearProyecto = async () => {
    const nombre = nuevoProyecto?.trim()
    if (!nombre) return
    try {
      const proyecto = await proyectoService.crear({ clienteId: form.clienteId, nombre, estado: 'ACTIVO' })
      setProyectos(p => [...p, proyecto])
      setForm(f => ({ ...f, proyectoId: proyecto.id }))
      setNuevoProyecto(null)
    } catch (e) {
      setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo crear el proyecto' })
    }
  }

  /** Guarda y devuelve el documento guardado, o null si hubo un problema. */
  const guardar = async (): Promise<Documento | null> => {
    const problema = nuevoProyecto?.trim() ? 'Termina de crear el proyecto o cancélalo.' : problemaDe(form)
    if (problema) {
      setAviso({ tipo: 'error', texto: problema })
      return null
    }
    setTrabajando(true)
    setAviso(null)
    try {
      const guardado = documento
        ? await documentoService.actualizar(documento.id, payloadDe(form, tipo))
        : await documentoService.crear(payloadDe(form, tipo))
      if (documento) {
        // La respuesta de editar no trae los enlaces a otros documentos
        mostrar(await documentoService.obtener(guardado.id))
      } else {
        recienCreadoRef.current = guardado.id
        mostrar(guardado)
        navigate(`/documentos/${guardado.id}`, { replace: true })
      }
      setAviso({ tipo: 'ok', texto: `${guardado.folio} guardado.` })
      return guardado
    } catch (e) {
      setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo guardar' })
      return null
    } finally {
      setTrabajando(false)
    }
  }

  /** Corre una acción sobre el documento y muestra el resultado. */
  const ejecutar = async (accion: () => Promise<Documento>, mensaje: string) => {
    setTrabajando(true)
    setAviso(null)
    try {
      mostrar(await accion())
      setAviso({ tipo: 'ok', texto: mensaje })
    } catch (e) {
      setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo completar la acción' })
    } finally {
      setTrabajando(false)
    }
  }

  const convertir = async () => {
    if (!documento) return
    const aviso = cambiosSinGuardar ? ' Los cambios sin guardar se guardarán primero.' : ''
    if (!window.confirm(`¿Convertir ${documento.folio} en factura?${aviso}`)) return
    if (cambiosSinGuardar && !(await guardar())) return
    setTrabajando(true)
    try {
      const factura = await documentoService.convertirAFactura(documento.id, hoyIso())
      navigate(`/documentos/${factura.id}`)
    } catch (e) {
      setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo convertir' })
    } finally {
      setTrabajando(false)
    }
  }

  const rechazar = () => {
    if (documento && window.confirm(`¿Marcar ${documento.folio} como rechazada?`)) {
      void ejecutar(() => documentoService.cambiarEstado(documento.id, 'RECHAZADA'), 'Cotización rechazada.')
    }
  }

  const reabrir = () => {
    if (documento) void ejecutar(() => documentoService.cambiarEstado(documento.id, 'PENDIENTE'), 'Cotización reabierta.')
  }

  const anular = () => {
    if (!documento) return
    const avisoOrigen = documento.cotizacionOrigenFolio
      ? ` La cotización ${documento.cotizacionOrigenFolio} volverá a pendiente.`
      : ''
    if (window.confirm(`¿Anular ${documento.folio}? Dejará de contar en ingresos.${avisoOrigen}`)) {
      void ejecutar(() => documentoService.anular(documento.id), 'Factura anulada.')
    }
  }

  const convertirHistorico = () => {
    if (!documento) return
    const avisoSaldo = clienteActual?.fechaInicioCuenta && documento.fecha < clienteActual.fechaInicioCuenta
      ? ' Su fecha es anterior al inicio de la cuenta: no cambiará el saldo.'
      : ''
    if (
      !window.confirm(
        `¿Convertir ${documento.folio} en factura activa? Recibirá un folio de factura nuevo y sumará ${formatCurrency(documento.total)} al saldo de ${documento.clienteNombre}.${avisoSaldo}`
      )
    ) return
    void ejecutar(() => documentoService.convertirHistorico(documento.id), 'Factura activa creada.')
  }

  const reactivar = () => {
    if (!documento) return
    if (!window.confirm(`¿Reactivar ${documento.folio}? Volverá a contar en ingresos y en el saldo.`)) return
    void ejecutar(() => documentoService.reactivar(documento.id), 'Factura reactivada.')
  }

  const alRegistrarCompra = async () => {
    if (!documento) return
    try {
      const actualizado = await documentoService.obtener(documento.id)
      mostrar(actualizado)
      setAviso({ tipo: 'ok', texto: 'Compra registrada.' })
    } catch (e) {
      setAviso({ tipo: 'error', texto: e instanceof Error ? e.message : 'Error al recargar el documento' })
    }
  }

  const volver = () => {
    if (!cambiosSinGuardar || window.confirm(SALIR_SIN_GUARDAR)) {
      navigate(titulos.ruta)
    }
  }

  if (cargando) {
    return <div className="p-4 md:p-8 text-sm text-[#705D56]">Cargando…</div>
  }
  if (id && !documento) {
    return (
      <div className="p-4 md:p-8 flex flex-col gap-3 items-start">
        <p className="text-sm text-red-600">{aviso?.texto ?? 'No se encontró el documento.'}</p>
        <Button variant="outline" onClick={() => navigate(titulos.ruta)}>Volver a {titulos.lista}</Button>
      </div>
    )
  }

  const Icono = tipo === 'COTIZACION' ? FileText : ReceiptText

  return (
    <div className="p-4 md:p-8 flex flex-col gap-5 w-full max-w-7xl mx-auto">
      {/* ── Encabezado ── */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <button type="button" onClick={volver} className="min-h-11 text-sm text-[#705D56] hover:text-[#022F40] flex items-center gap-1 mb-1">
            <ArrowLeft className="w-3.5 h-3.5" /> {titulos.lista}
          </button>
          <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
            <Icono className="w-5 h-5" />
            {documento ? documento.folio : titulos.nuevo}
            {documento && <EstadoDocumentoBadge documento={documento} />}
          </h1>
        </div>

        {documento && (
          <div className="flex flex-wrap items-center gap-2">
            {documento.estado !== 'HISTORICA' && (
              <Button variant="outline" onClick={abrirExportar} className="gap-2">
                <FileDown className="w-4 h-4" /> Exportar
              </Button>
            )}
            <Button variant="outline" onClick={() => setViendoCambios(true)} className="gap-2">
              <History className="w-4 h-4" /> Ver cambios
            </Button>
            {documento.estado === 'PENDIENTE' && (
              <>
                <Button variant="outline" disabled={trabajando} onClick={rechazar} >Rechazar</Button>
                <Button disabled={trabajando} onClick={convertir} className="bg-[#558564] hover:bg-[#558564]/90 text-white">Convertir en factura</Button>
              </>
            )}
            {documento.estado === 'RECHAZADA' && (
              <Button variant="outline" disabled={trabajando} onClick={reabrir} >Reabrir</Button>
            )}
            {documento.estado === 'ACTIVA' && (
              <>
                <span title={cambiosSinGuardar ? 'Guarda primero los cambios' : undefined}>
                  <Button
                    variant="outline"
                    disabled={trabajando || cambiosSinGuardar}
                    onClick={() => setRegistrandoCompra(true)}
                    className="gap-2"
                  >
                    <ShoppingCart className="w-4 h-4" /> Registrar compra
                  </Button>
                </span>
                <Button variant="outline" disabled={trabajando} onClick={anular} className="text-red-600 hover:text-red-700">Anular</Button>
              </>
            )}
            {documento.estado === 'HISTORICA' && (
              <Button disabled={trabajando} onClick={convertirHistorico} className="bg-[#558564] hover:bg-[#558564]/90 text-white">
                Convertir en factura activa
              </Button>
            )}
            {documento.estado === 'ANULADA' && (
              <Button variant="outline" disabled={trabajando} onClick={reactivar}>
                Reactivar
              </Button>
            )}
          </div>
        )}
      </div>

      {/* ── Avisos del estado del documento ── */}
      {documento?.estado === 'ACEPTADA' && (
        <p className="text-sm bg-[#558564]/10 border border-[#558564]/30 text-[#022F40] rounded px-4 py-3">
          Esta cotización ya se convirtió en factura
          {documento.facturaGeneradaId && (
            <> <Link className="font-semibold underline" to={`/documentos/${documento.facturaGeneradaId}`}>{documento.facturaGeneradaFolio}</Link></>
          )}. Para corregir algo, edita la factura.
        </p>
      )}
      {documento?.estado === 'ANULADA' && (
        <p className="text-sm bg-[#F9FAFB] border border-[#E5E7EB] text-[#705D56] rounded px-4 py-3">
          Esta factura está anulada: no cuenta en ingresos. Para editarla, reactívala.
        </p>
      )}
      {documento?.estado === 'HISTORICA' && (
        <p className="text-sm bg-[#F9FAFB] border border-[#E5E7EB] text-[#705D56] rounded px-4 py-3">
          Este registro viene del histórico de precios. No cuenta en saldos ni reportes. Para usarlo como venta, conviértelo en factura activa.
        </p>
      )}
      {documento?.estado === 'RECHAZADA' && (
        <p className="text-sm bg-[#F9FAFB] border border-[#E5E7EB] text-[#705D56] rounded px-4 py-3">
          Esta cotización está rechazada. Sus precios siguen contando como referencia. Para convertirla en factura, reábrela.
        </p>
      )}
      {documento?.cotizacionOrigenId && (
        <p className="text-sm text-[#705D56]">
          Viene de la cotización <Link className="font-semibold underline" to={`/documentos/${documento.cotizacionOrigenId}`}>{documento.cotizacionOrigenFolio}</Link>.
        </p>
      )}

      {/* ── Datos ── */}
      <fieldset disabled={soloLectura} className="contents">
        <section className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cliente" className="text-sm font-medium text-[#705D56]">Cliente</label>
            <select
              id="cliente"
              value={form.clienteId}
              onChange={e => alCambiarCliente(e.target.value)}
              className={SELECT}
            >
              <option value="">Elige un cliente…</option>
              {clientes.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="proyecto" className="text-sm font-medium text-[#705D56]">Proyecto (opcional)</label>
            {nuevoProyecto === null ? (
              <div className="flex gap-2">
                <select
                  id="proyecto"
                  value={form.proyectoId}
                  disabled={!form.clienteId}
                  onChange={e => setForm(f => ({ ...f, proyectoId: e.target.value }))}
                  className={SELECT}
                >
                  <option value="">Sin proyecto</option>
                  {proyectosVisibles.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                </select>
                <Button
                  type="button"
                  variant="outline"
                  aria-label="Crear un proyecto nuevo"
                  title="Crear un proyecto nuevo"
                  disabled={!form.clienteId}
                  onClick={() => setNuevoProyecto('')}
                  className="px-2.5"
                >
                  <Plus className="w-4 h-4" />
                </Button>
              </div>
            ) : (
              <div className="flex gap-2">
                <Input
                  id="proyecto"
                  autoFocus
                  placeholder="Nombre del proyecto"
                  value={nuevoProyecto}
                  onChange={e => setNuevoProyecto(e.target.value)}
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      void crearProyecto()
                    }
                  }}
                />
                <Button type="button" variant="outline" disabled={!nuevoProyecto.trim()} onClick={crearProyecto} >Crear</Button>
                <Button type="button" variant="outline" onClick={() => setNuevoProyecto(null)} >Cancelar</Button>
              </div>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="fecha" className="text-sm font-medium text-[#705D56]">Fecha</label>
            <Input id="fecha" type="date" value={form.fecha} onChange={e => setForm(f => ({ ...f, fecha: e.target.value }))} />
            {tipo === 'FACTURA' && clienteActual?.fechaInicioCuenta && form.fecha < clienteActual.fechaInicioCuenta && (
              <p className="text-sm text-amber-700">
                Esta fecha es anterior al inicio de la cuenta ({formatFechaCorta(clienteActual.fechaInicioCuenta)}): la factura no afectará el saldo.
              </p>
            )}
          </div>
        </section>

        {/* ── Líneas ── */}
        <section className="bg-white border border-[#E5E7EB] rounded shadow-sm p-5 flex flex-col gap-4">
          {!soloLectura && (
            <ProductoBuscador
              onElegir={agregarProducto}
              deshabilitado={!form.clienteId}
            />
          )}

          <LineasTabla
            lineas={form.lineas}
            verEntrega={verEntrega}
            soloLectura={soloLectura}
            claveNueva={claveNueva}
            onCambiar={cambiarLinea}
            onCambiarPeso={cambiarPeso}
            onQuitar={quitarLinea}
            onAbrirPrecio={clave => setLineaParaPrecio(clave)}
            onAbrirCalculadora={clave => {
              setModoCalculadoraSoloPeso(true)
              setLineaParaCalculadora(clave)
            }}
            onBlurDescripcion={alSalirDescripcion}
          />

          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="flex flex-col gap-2 text-sm text-[#705D56]">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={verEntrega}
                  onChange={e => {
                    setVerEntrega(e.target.checked)
                    if (!e.target.checked) setForm(f => ({ ...f, mostrarEntrega: false }))
                  }}
                />
                Anotar fecha y lugar de entrega
              </label>
              {verEntrega && (
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.mostrarEntrega}
                    onChange={e => setForm(f => ({ ...f, mostrarEntrega: e.target.checked }))}
                  />
                  Mostrarlos también al exportar
                </label>
              )}
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.mostrarTotalUsd}
                  onChange={e => setForm(f => ({ ...f, mostrarTotalUsd: e.target.checked }))}
                />
                Mostrar total en dólares
              </label>
            </div>

            <dl className="text-right">
              <div className="flex items-baseline justify-end gap-3">
                <dt className="text-sm text-[#705D56]">Total</dt>
                <dd className="text-2xl font-bold text-[#022F40]">{formatCurrency(totales.total)}</dd>
              </div>
              {form.mostrarTotalUsd && (
                <div className="flex items-baseline justify-end gap-3">
                  <dt className="text-sm text-[#705D56]">Total U$</dt>
                  <dd className="text-lg font-bold text-[#022F40]">
                    {tipoCambio ? formatUsd(r2(totales.total / tipoCambio)) : '—'}
                  </dd>
                </div>
              )}
              <div className="flex items-baseline justify-end gap-3">
                <dt className="text-sm text-[#705D56]">Total KG</dt>
                <dd className="text-sm font-semibold text-[#022F40]">{totales.kilos.toFixed(2)}</dd>
              </div>
            </dl>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="notas" className="text-sm font-medium text-[#705D56]">Notas (opcional)</label>
            <textarea
              id="notas"
              rows={2}
              value={form.notas}
              onChange={e => setForm(f => ({ ...f, notas: e.target.value }))}
              className="w-full rounded border border-[#E5E7EB] px-3 py-2 text-sm text-[#022F40] outline-none focus:border-[#022F40] resize-none disabled:opacity-60"
            />
          </div>
        </section>
      </fieldset>

      {/* ── Guardar ── */}
      {!soloLectura && (
        <div className="sticky bottom-0 z-20 bg-white border border-[#E5E7EB] rounded shadow-md px-5 py-3 flex flex-wrap items-center justify-between gap-3">
          <p role="status" className={`text-sm ${aviso?.tipo === 'error' ? 'text-red-600' : aviso?.tipo === 'ok' && !cambiosSinGuardar ? 'text-[#558564]' : 'text-[#705D56]'}`}>
            {aviso && (aviso.tipo === 'error' || !cambiosSinGuardar) ? aviso.texto : cambiosSinGuardar ? 'Hay cambios sin guardar.' : ''}
          </p>
          <Button
            disabled={trabajando || (Boolean(documento) && !cambiosSinGuardar)}
            onClick={() => void guardar()}
            className="px-6 gap-2 bg-[#022F40] hover:bg-[#022F40]/90 text-white"
          >
            <Save className="w-4 h-4" /> {trabajando ? 'Guardando…' : 'Guardar'}
          </Button>
        </div>
      )}
      {soloLectura && aviso && (
        <p role="status" className={`text-sm ${aviso.tipo === 'error' ? 'text-red-600' : 'text-[#558564]'}`}>{aviso.texto}</p>
      )}

      {documento && (
        <ExportarDialog
          open={exportando}
          onOpenChange={setExportando}
          encabezadoInicial={`${documento.clienteNombre}${documento.proyectoNombre ? ` / ${documento.proyectoNombre}` : ''}${documento.tipo === 'COTIZACION' ? ' / Cotización' : ''}`}
          nombreBase={(ext) => nombreArchivo(documento.folio, documento.clienteNombre, documento.fecha, ext)}
          horizontal={documento.mostrarEntrega}
          plantilla={(encabezado) => (
            <DocumentoPlantilla documento={documento} encabezado={encabezado} />
          )}
          descargarExcel={(enc) => documentoService.descargarExcel(documento.id, enc)}
        />
      )}
      {documento && <RevisionesDialog documentoActual={documento} open={viendoCambios} onOpenChange={setViendoCambios} />}
      {documento && (
        <RegistrarCompraDialog
          open={registrandoCompra}
          onOpenChange={setRegistrandoCompra}
          documentoId={documento.id}
          lineas={documento.lineas}
          tipoCambio={tipoCambio ?? 0}
          ivaPct={config?.ivaPct ?? 15}
          onCompraRegistrada={() => void alRegistrarCompra()}
        />
      )}

      <PrecioLineaDialog
        open={lineaParaPrecio !== null}
        onOpenChange={v => { if (!v) setLineaParaPrecio(null) }}
        clienteId={form.clienteId}
        linea={lineaActivaPrecio}
        tipoCambio={tipoCambio}
        onAplicar={(precioUnit, metodoPrecio, parametros) => {
          if (lineaParaPrecio) {
            cambiarLinea(lineaParaPrecio, { precioUnit, metodoPrecio, parametros })
          }
        }}
        onAbrirCalculadoraPeso={() => {
          setModoCalculadoraSoloPeso(false)
          setLineaParaCalculadora(lineaParaPrecio)
          setLineaParaPrecio(null)
        }}
      />

      <CalculadoraPesoDialog
        open={lineaParaCalculadora !== null}
        onOpenChange={v => { if (!v) setLineaParaCalculadora(null) }}
        clienteId={form.clienteId}
        linea={lineaActivaCalculadora}
        soloPeso={modoCalculadoraSoloPeso}
        tipoCambio={tipoCambio}
        onAplicar={datos => {
          if (lineaParaCalculadora) {
            if (!datos.precioUnit) {
              cambiarPeso(lineaParaCalculadora, datos.pesoUnitKg)
            }
            const cambios: Partial<LineaForm> = { pesoUnitKg: datos.pesoUnitKg }
            if (datos.descripcion) cambios.descripcion = datos.descripcion
            if (datos.precioUnit) cambios.precioUnit = datos.precioUnit
            if (datos.metodoPrecio) cambios.metodoPrecio = datos.metodoPrecio
            if (datos.parametros !== undefined) cambios.parametros = datos.parametros
            cambiarLinea(lineaParaCalculadora, cambios)
          }
        }}
      />
    </div>
  )
}
