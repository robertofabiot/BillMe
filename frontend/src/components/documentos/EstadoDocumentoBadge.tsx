import type { Documento } from '@/types'

const VERDE = { dot: 'bg-[#558564]', clase: 'bg-[#558564]/10 text-[#558564] border-[#558564]/30' }
const AMBAR = { dot: 'bg-amber-400', clase: 'bg-amber-50 text-amber-700 border-amber-200' }
const GRIS = { dot: 'bg-[#80727B]', clase: 'bg-[#F9FAFB] text-[#705D56] border-[#E5E7EB]' }

function apariencia(d: Pick<Documento, 'estado'>) {
  switch (d.estado) {
    case 'PENDIENTE': return { texto: 'Pendiente', ...AMBAR }
    case 'ACEPTADA': return { texto: 'Aceptada', ...VERDE }
    case 'RECHAZADA': return { texto: 'Rechazada', ...GRIS }
    case 'ANULADA': return { texto: 'Anulada', ...GRIS }
    case 'ACTIVA': return { texto: 'Activa', ...VERDE }
    case 'HISTORICA': return { texto: 'Histórico', ...GRIS }
    default: return { texto: d.estado, ...GRIS }
  }
}

export function EstadoDocumentoBadge({ documento }: { documento: Pick<Documento, 'estado'> }) {
  const a = apariencia(documento)
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-sm font-medium border ${a.clase}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${a.dot}`} />
      {a.texto}
    </span>
  )
}
