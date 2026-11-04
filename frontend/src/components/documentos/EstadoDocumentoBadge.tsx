import type { Documento } from '@/types'

const VERDE = { dot: 'bg-[#558564]', clase: 'bg-[#558564]/10 text-[#558564] border-[#558564]/30' }
const AMBAR = { dot: 'bg-amber-400', clase: 'bg-amber-50 text-amber-700 border-amber-200' }
const ROJO = { dot: 'bg-red-400', clase: 'bg-red-50 text-red-700 border-red-200' }
const GRIS = { dot: 'bg-[#80727B]', clase: 'bg-[#F9FAFB] text-[#705D56] border-[#E5E7EB]' }

/** En las facturas, lo que importa ver es si están cobradas. */
function apariencia(d: Pick<Documento, 'tipo' | 'estado' | 'totalAbonado' | 'saldoPendiente'>) {
  switch (d.estado) {
    case 'PENDIENTE': return { texto: 'Pendiente', ...AMBAR }
    case 'ACEPTADA': return { texto: 'Aceptada', ...VERDE }
    case 'RECHAZADA': return { texto: 'Rechazada', ...GRIS }
    case 'ANULADA': return { texto: 'Anulada', ...GRIS }
    default:
      if ((d.saldoPendiente ?? 0) <= 0) return { texto: 'Pagada', ...VERDE }
      return d.totalAbonado > 0 ? { texto: 'Abono parcial', ...AMBAR } : { texto: 'Por cobrar', ...ROJO }
  }
}

export function EstadoDocumentoBadge({ documento }: { documento: Pick<Documento, 'tipo' | 'estado' | 'totalAbonado' | 'saldoPendiente'> }) {
  const a = apariencia(documento)
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium border ${a.clase}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${a.dot}`} />
      {a.texto}
    </span>
  )
}
