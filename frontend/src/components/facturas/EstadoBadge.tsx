import type { EstadoFactura } from '@/types'

export const ESTADO_CONFIG: Record<EstadoFactura, { label: string; className: string; dot: string }> = {
  COTIZACION:   { label: 'Cotización',   dot: 'bg-[#91E5F6]',  className: 'bg-[#91E5F6]/20 text-[#022F40] border-[#91E5F6]/50'    },
  PENDIENTE:    { label: 'Pendiente',    dot: 'bg-red-400',    className: 'bg-red-50 text-red-700 border-red-200'                  },
  PAGO_PARCIAL: { label: 'Pago Parcial', dot: 'bg-amber-400',  className: 'bg-amber-50 text-amber-700 border-amber-200'            },
  PAGADO:       { label: 'Pagado',       dot: 'bg-[#558564]',  className: 'bg-[#558564]/10 text-[#558564] border-[#558564]/30'     },
}

export function EstadoBadge({ estado }: { estado: EstadoFactura }) {
  const cfg = ESTADO_CONFIG[estado]
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium border ${cfg.className}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  )
}
