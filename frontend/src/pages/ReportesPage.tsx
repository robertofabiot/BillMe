import { useState, useMemo, useEffect } from 'react'
import { facturaService } from '@/services/facturaService'
import { clienteService } from '@/services/clienteService'
import { productoService } from '@/services/productoService'
import { proyectoService } from '@/services/proyectoService'
import type { Factura, Cliente, Proyecto, Producto } from '@/types'
import { formatCurrency, formatDate } from '@/lib/format'
import {
  BarChart3, TrendingUp, Users, Package, FolderGit2, Calendar,
  PieChart as PieChartIcon, DollarSign, ArrowUpRight, ArrowDownRight, AlertCircle, Filter
} from 'lucide-react'
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts'

const COLORS = ['#022F40', '#38AECC', '#558564', '#F4A261', '#E76F51', '#2A9D8F', '#E9C46A']

function StatCard({ label, value, subtext, icon: Icon, trend }: {
  label: string; value: string; subtext?: string; icon: React.ElementType; trend?: 'up' | 'down'
}) {
  return (
    <div className="bg-white border border-[#E5E7EB] rounded p-5 flex flex-col shadow-sm">
      <div className="flex justify-between items-start mb-2">
        <p className="text-xs font-semibold uppercase tracking-wider text-[#80727B]">{label}</p>
        <div className="w-8 h-8 rounded bg-[#F9FAFB] flex items-center justify-center">
          <Icon className="w-4 h-4 text-[#022F40]" />
        </div>
      </div>
      <p className="text-2xl font-bold text-[#022F40] leading-none mb-2">{value}</p>
      {subtext && (
        <div className="flex items-center gap-1 text-[11px] font-medium">
          {trend === 'up' && <ArrowUpRight className="w-3 h-3 text-[#558564]" />}
          {trend === 'down' && <ArrowDownRight className="w-3 h-3 text-red-500" />}
          <span className={trend === 'up' ? 'text-[#558564]' : trend === 'down' ? 'text-red-500' : 'text-[#80727B]'}>
            {subtext}
          </span>
        </div>
      )}
    </div>
  )
}

export function ReportesPage() {
  const [activeTab, setActiveTab] = useState<'GENERAL' | 'CLIENTES' | 'PROYECTOS' | 'PRODUCTOS'>('GENERAL')

  // Filtros Globales
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [clienteId, setClienteId] = useState('ALL')
  const [proyectoId, setProyectoId] = useState('ALL')

  const [clientes, setClientes] = useState<Cliente[]>([])
  const [proyectos, setProyectos] = useState<Proyecto[]>([])
  const [productos, setProductos] = useState<Producto[]>([])
  const [facturas, setFacturas] = useState<Factura[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      clienteService.listar(),
      proyectoService.listar(),
      productoService.listar()
    ]).then(([resClientes, resProyectos, resProductos]) => {
      setClientes(resClientes)
      setProyectos(resProyectos)
      setProductos(resProductos)
    }).catch(err => {
      setError('Error al cargar datos iniciales')
    })
  }, [])

  useEffect(() => {
    setLoading(true)
    const filtros: any = {}
    if (clienteId !== 'ALL') filtros.clienteId = clienteId
    if (proyectoId !== 'ALL') filtros.proyectoId = proyectoId
    if (dateFrom) filtros.desde = dateFrom
    if (dateTo) filtros.hasta = dateTo

    facturaService.listar(filtros)
      .then(res => setFacturas(res))
      .catch(err => setError('Error al cargar facturas'))
      .finally(() => setLoading(false))
  }, [clienteId, proyectoId, dateFrom, dateTo])

  /* ── Core Data Processing ── */
  const filteredFacturas = useMemo(() => {
    return facturas.filter(f => f.estado !== 'COTIZACION')
  }, [facturas])

  // 1. General Metrics
  const metrics = useMemo(() => {
    let totalVenta = 0
    let totalCosto = 0
    let totalCobrado = 0

    filteredFacturas.forEach(f => {
      totalVenta += f.totalVenta
      
      const costoFac = f.costoRealEmpresa ?? f.detalles.reduce((s, d) => s + (d.cantidad * (d.costoUnitario || 0)), 0)
      totalCosto += costoFac
      
      totalCobrado += f.abonos.reduce((s, a) => s + a.monto, 0)
    })

    const ganancia = totalVenta - totalCosto
    const margen = totalVenta > 0 ? (ganancia / totalVenta) * 100 : 0
    const porCobrar = totalVenta - totalCobrado

    return { totalVenta, totalCosto, totalCobrado, porCobrar, ganancia, margen }
  }, [filteredFacturas])

  // 2. Trend by Month (Fechas)
  const trendsByMonth = useMemo(() => {
    const months: Record<string, { month: string; ventas: number; costos: number; ganancia: number }> = {}
    
    filteredFacturas.forEach(f => {
      const date = new Date(f.createdAt)
      // Ajuste por zona horaria simple
      const month = date.toLocaleString('es-MX', { timeZone: 'UTC', month: 'short', year: '2-digit' }).toUpperCase()
      
      if (!months[month]) months[month] = { month, ventas: 0, costos: 0, ganancia: 0 }
      
      const costo = f.costoRealEmpresa ?? f.detalles.reduce((s, d) => s + (d.cantidad * (d.costoUnitario || 0)), 0)
      
      months[month].ventas += f.totalVenta
      months[month].costos += costo
      months[month].ganancia += (f.totalVenta - costo)
    })

    return Object.values(months).reverse() // In mock data we are assuming chronological order.
  }, [filteredFacturas])

  // 3. By Client
  const byClient = useMemo(() => {
    const clients: Record<string, { name: string; ventas: number; cobrado: number; facturas: number }> = {}
    
    filteredFacturas.forEach(f => {
      const name = f.clienteNombre || 'Desconocido'
      
      if (!clients[name]) clients[name] = { name, ventas: 0, cobrado: 0, facturas: 0 }
      
      clients[name].ventas += f.totalVenta
      clients[name].cobrado += f.abonos.reduce((s, a) => s + a.monto, 0)
      clients[name].facturas += 1
    })

    return Object.values(clients).sort((a, b) => b.ventas - a.ventas)
  }, [filteredFacturas])

  // 4. By Project
  const byProject = useMemo(() => {
    const projects: Record<string, { name: string; cliente: string; ventas: number; facturas: number }> = {}
    
    filteredFacturas.forEach(f => {
      if (!f.proyectoId) return
      
      const name = f.proyectoNombre || 'Desconocido'
      
      if (!projects[name]) projects[name] = { name, cliente: f.clienteNombre || 'Desconocido', ventas: 0, facturas: 0 }
      
      projects[name].ventas += f.totalVenta
      projects[name].facturas += 1
    })

    return Object.values(projects).sort((a, b) => b.ventas - a.ventas)
  }, [filteredFacturas])

  // 5. By Product
  const byProduct = useMemo(() => {
    const prods: Record<string, { name: string; cantidad: number; ingresos: number; ganancia: number }> = {}
    
    filteredFacturas.forEach(f => {
      f.detalles.forEach(d => {
        const name = d.productoNombre
        if (!prods[name]) prods[name] = { name, cantidad: 0, ingresos: 0, ganancia: 0 }
        
        const ingresos = d.cantidad * d.precioUnitarioVenta
        const costo = d.cantidad * (d.costoUnitario || 0)
        
        prods[name].cantidad += d.cantidad
        prods[name].ingresos += ingresos
        prods[name].ganancia += (ingresos - costo)
      })
    })

    return Object.values(prods).sort((a, b) => b.ingresos - a.ingresos).slice(0, 10)
  }, [filteredFacturas])

  const proyectosDisponibles = useMemo(() => {
    if (clienteId === 'ALL') return proyectos
    return proyectos.filter(p => p.clienteId === clienteId)
  }, [clienteId, proyectos])

  return (
    <div className="p-8 flex flex-col gap-6">

      {/* ── Header ── */}
      <div>
        <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
          <BarChart3 className="w-5 h-5" /> Reportes y Analíticas
          {loading && <span className="text-sm font-normal text-blue-500 ml-2">(Cargando...)</span>}
        </h1>
        <p className="text-sm text-[#705D56] mt-0.5">Filtra y cruza información para obtener vistas detalladas</p>
        {error && <p className="text-sm text-red-500 mt-2">{error}</p>}
      </div>

      {/* ── Barra de Filtros Globales ── */}
      <div className="bg-white border border-[#E5E7EB] rounded shadow-sm p-4 flex flex-col md:flex-row items-center gap-4">
        <div className="flex items-center gap-2 text-[#80727B] shrink-0 border-r border-[#E5E7EB] pr-4">
          <Filter className="w-4 h-4" />
          <span className="text-xs font-bold uppercase tracking-wider">Filtros</span>
        </div>
        
        <div className="flex flex-1 items-center gap-4 w-full">
          <div className="flex flex-col gap-1 w-32">
            <label className="text-[10px] uppercase font-bold text-[#705D56] tracking-wider">Desde</label>
            <input 
              type="date" 
              value={dateFrom} 
              onChange={e => setDateFrom(e.target.value)}
              className="h-8 rounded border border-[#E5E7EB] px-2 text-xs text-[#022F40] outline-none focus:border-[#022F40]" 
            />
          </div>
          <div className="flex flex-col gap-1 w-32">
            <label className="text-[10px] uppercase font-bold text-[#705D56] tracking-wider">Hasta</label>
            <input 
              type="date" 
              value={dateTo} 
              onChange={e => setDateTo(e.target.value)}
              className="h-8 rounded border border-[#E5E7EB] px-2 text-xs text-[#022F40] outline-none focus:border-[#022F40]" 
            />
          </div>
          <div className="flex flex-col gap-1 flex-1 min-w-[200px]">
            <label className="text-[10px] uppercase font-bold text-[#705D56] tracking-wider">Cliente</label>
            <select 
              value={clienteId} 
              onChange={e => { setClienteId(e.target.value); setProyectoId('ALL') }}
              className="h-8 rounded border border-[#E5E7EB] px-2 text-xs text-[#022F40] outline-none focus:border-[#022F40]"
            >
              <option value="ALL">Todos los clientes</option>
              {clientes.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-1 flex-1 min-w-[200px]">
            <label className="text-[10px] uppercase font-bold text-[#705D56] tracking-wider">Proyecto</label>
            <select 
              value={proyectoId} 
              onChange={e => setProyectoId(e.target.value)}
              className="h-8 rounded border border-[#E5E7EB] px-2 text-xs text-[#022F40] outline-none focus:border-[#022F40] disabled:bg-[#F9FAFB] disabled:text-[#80727B]"
              disabled={proyectosDisponibles.length === 0}
            >
              <option value="ALL">Todos los proyectos</option>
              {proyectosDisponibles.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </select>
          </div>
        </div>
      </div>

      {/* ── Tabs ── */}
      <div className="flex items-center gap-2 border-b border-[#E5E7EB] pb-px">
        {[
          { id: 'GENERAL',   label: 'General & Fechas', icon: TrendingUp },
          { id: 'CLIENTES',  label: 'Por Cliente',      icon: Users },
          { id: 'PROYECTOS', label: 'Por Proyecto',     icon: FolderGit2 },
          { id: 'PRODUCTOS', label: 'Por Producto',     icon: Package },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id as any)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              activeTab === t.id
                ? 'border-[#022F40] text-[#022F40]'
                : 'border-transparent text-[#80727B] hover:text-[#022F40] hover:border-[#E5E7EB]'
            }`}
          >
            <t.icon className="w-4 h-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* ── Tab Content ── */}
      <div className="flex flex-col gap-6">
        
        {/* VIEW: GENERAL */}
        {activeTab === 'GENERAL' && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <StatCard label="Ingresos Totales" value={formatCurrency(metrics.totalVenta)} icon={DollarSign} trend="up" subtext="Facturado" />
              <StatCard label="Total Cobrado" value={formatCurrency(metrics.totalCobrado)} icon={DollarSign} />
              <StatCard label="Por Cobrar (Cartera)" value={formatCurrency(metrics.porCobrar)} icon={AlertCircle} trend="down" subtext="Pendiente de pago" />
              <StatCard label="Ganancia Neta" value={formatCurrency(metrics.ganancia)} icon={TrendingUp} trend="up" subtext="Utilidad bruta" />
              <StatCard label="Margen Promedio" value={`${metrics.margen.toFixed(1)}%`} icon={PieChartIcon} />
            </div>

            <div className="bg-white border border-[#E5E7EB] rounded p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-6">
                <Calendar className="w-4 h-4 text-[#80727B]" />
                <h2 className="text-sm font-semibold text-[#022F40]">Evolución de Ingresos y Ganancias</h2>
              </div>
              <div className="h-[350px] w-full">
                {trendsByMonth.length === 0 ? (
                  <div className="flex items-center justify-center h-full text-[#80727B] text-sm">No hay datos en este periodo</div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={trendsByMonth} margin={{ top: 10, right: 30, left: 20, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                      <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#80727B' }} axisLine={false} tickLine={false} />
                      <YAxis tickFormatter={v => `$${(v/1000).toFixed(0)}k`} tick={{ fontSize: 12, fill: '#80727B' }} axisLine={false} tickLine={false} />
                      <Tooltip 
                        formatter={(v: number) => formatCurrency(v)}
                        contentStyle={{ borderRadius: '6px', border: '1px solid #E5E7EB', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' }}
                      />
                      <Legend iconType="circle" wrapperStyle={{ fontSize: '12px', paddingTop: '20px' }} />
                      <Line type="monotone" name="Ingresos (Ventas)" dataKey="ventas" stroke="#38AECC" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                      <Line type="monotone" name="Costos" dataKey="costos" stroke="#F4A261" strokeWidth={2} dot={{ r: 3 }} />
                      <Line type="monotone" name="Ganancia Neta" dataKey="ganancia" stroke="#558564" strokeWidth={3} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </>
        )}

        {/* VIEW: CLIENTES */}
        {activeTab === 'CLIENTES' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden flex flex-col">
              <div className="px-5 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB]">
                <h2 className="text-sm font-semibold text-[#022F40]">Ranking de Clientes por Facturación</h2>
              </div>
              <div className="flex-1 overflow-auto">
                <table className="w-full text-sm">
                  <thead className="bg-white border-b border-[#E5E7EB] sticky top-0">
                    <tr>
                      <th className="text-left px-5 py-3 font-medium text-[#705D56]">Cliente</th>
                      <th className="text-center px-5 py-3 font-medium text-[#705D56]">Facturas</th>
                      <th className="text-right px-5 py-3 font-medium text-[#705D56]">Facturado</th>
                      <th className="text-right px-5 py-3 font-medium text-[#705D56]">Cobrado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {byClient.length === 0 && (
                      <tr><td colSpan={4} className="text-center py-8 text-[#80727B]">No hay datos para mostrar</td></tr>
                    )}
                    {byClient.map(c => (
                      <tr key={c.name} className="border-b border-[#E5E7EB] last:border-0 hover:bg-[#F9FAFB]">
                        <td className="px-5 py-3 font-medium text-[#022F40]">{c.name}</td>
                        <td className="px-5 py-3 text-center text-[#80727B]">{c.facturas}</td>
                        <td className="px-5 py-3 text-right font-semibold text-[#022F40]">{formatCurrency(c.ventas)}</td>
                        <td className="px-5 py-3 text-right font-medium text-[#558564]">{formatCurrency(c.cobrado)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="bg-white border border-[#E5E7EB] rounded p-5 shadow-sm flex flex-col">
              <h2 className="text-sm font-semibold text-[#022F40] mb-6">Distribución de Ingresos</h2>
              <div className="flex-1 min-h-[300px]">
                {byClient.length === 0 ? (
                  <div className="flex items-center justify-center h-full text-[#80727B] text-sm">Sin datos</div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={byClient}
                        dataKey="ventas"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={100}
                        paddingAngle={2}
                      >
                        {byClient.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(v: number) => formatCurrency(v)} />
                      <Legend layout="vertical" verticalAlign="bottom" wrapperStyle={{ fontSize: '11px', paddingTop: '20px' }} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </div>
        )}

        {/* VIEW: PROYECTOS */}
        {activeTab === 'PROYECTOS' && (
          <div className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden flex flex-col">
            <div className="px-5 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB]">
              <h2 className="text-sm font-semibold text-[#022F40]">Facturación por Proyectos / Obras</h2>
            </div>
            <div className="p-5">
              <div className="h-[400px] w-full">
                {byProject.length === 0 ? (
                  <div className="flex items-center justify-center h-full text-[#80727B] text-sm">No hay proyectos facturados en este cruce de filtros</div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={byProject} layout="vertical" margin={{ top: 5, right: 30, left: 40, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} stroke="#E5E7EB" />
                      <XAxis type="number" tickFormatter={v => `$${(v/1000).toFixed(0)}k`} tick={{ fontSize: 12, fill: '#80727B' }} axisLine={false} tickLine={false} />
                      <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: '#022F40', fontWeight: 500 }} width={150} axisLine={false} tickLine={false} />
                      <Tooltip 
                        formatter={(v: number) => formatCurrency(v)}
                        contentStyle={{ borderRadius: '6px', border: '1px solid #E5E7EB' }}
                      />
                      <Bar dataKey="ventas" name="Total Facturado" fill="#38AECC" radius={[0, 4, 4, 0]} barSize={24}>
                        {byProject.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </div>
        )}

        {/* VIEW: PRODUCTOS */}
        {activeTab === 'PRODUCTOS' && (
          <div className="grid grid-cols-1 gap-6">
            <div className="bg-white border border-[#E5E7EB] rounded shadow-sm overflow-hidden flex flex-col">
              <div className="px-5 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB]">
                <h2 className="text-sm font-semibold text-[#022F40]">Top 10 Productos por Ingresos</h2>
              </div>
              <div className="flex-1 overflow-auto">
                <table className="w-full text-sm">
                  <thead className="bg-white border-b border-[#E5E7EB]">
                    <tr>
                      <th className="text-left px-5 py-3 font-medium text-[#705D56]">Producto</th>
                      <th className="text-center px-5 py-3 font-medium text-[#705D56]">Unidades Vendidas</th>
                      <th className="text-right px-5 py-3 font-medium text-[#705D56]">Ingresos Brutos</th>
                      <th className="text-right px-5 py-3 font-medium text-[#705D56]">Ganancia Neta</th>
                    </tr>
                  </thead>
                  <tbody>
                    {byProduct.length === 0 && (
                      <tr><td colSpan={4} className="text-center py-8 text-[#80727B]">No hay datos para mostrar</td></tr>
                    )}
                    {byProduct.map((p, idx) => (
                      <tr key={p.name} className="border-b border-[#E5E7EB] last:border-0 hover:bg-[#F9FAFB]">
                        <td className="px-5 py-3 font-medium text-[#022F40] flex items-center gap-2">
                          <span className="w-5 text-xs text-[#80727B] font-mono">{idx + 1}.</span> {p.name}
                        </td>
                        <td className="px-5 py-3 text-center font-medium text-[#022F40]">{p.cantidad}</td>
                        <td className="px-5 py-3 text-right font-bold text-[#022F40]">{formatCurrency(p.ingresos)}</td>
                        <td className="px-5 py-3 text-right font-semibold text-[#558564]">{formatCurrency(p.ganancia)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  )
}
