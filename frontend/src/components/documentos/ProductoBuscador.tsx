import { useEffect, useRef, useState } from 'react'
import type { Producto } from '@/types'
import { productoService } from '@/services/productoService'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { PenLine, Search } from 'lucide-react'

interface Props {
  onElegir: (producto: Producto) => void
  deshabilitado?: boolean
}

/** Busca productos aunque el nombre esté escrito distinto. Enter agrega el primer resultado. */
export function ProductoBuscador({ onElegir, deshabilitado }: Props) {
  const [texto, setTexto] = useState('')
  const [resultados, setResultados] = useState<Producto[]>([])
  const [abierto, setAbierto] = useState(false)
  const [buscando, setBuscando] = useState(false)
  const entrada = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const q = texto.trim()
    const temporizador = setTimeout(() => {
      if (!q) {
        setResultados([])
        setBuscando(false)
        return
      }
      setBuscando(true)
      productoService.listar(q)
        .then(setResultados)
        .catch(() => setResultados([]))
        .finally(() => setBuscando(false))
    }, 250)
    return () => clearTimeout(temporizador)
  }, [texto])

  const elegir = (producto: Producto) => {
    onElegir(producto)
    setTexto('')
    setResultados([])
    setAbierto(false)
    entrada.current?.focus()
  }

  const agregarLineaLibre = async () => {
    const encontrados = await productoService.listar('GEN-LIBRE')
    const libre = encontrados.find(p => p.codigoInterno === 'GEN-LIBRE')
    if (libre) onElegir(libre)
  }

  const q = texto.trim()

  return (
    <div className="flex flex-wrap items-start gap-3">
      <div className="relative flex-1 min-w-[260px]">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#80727B]" />
        <Input
          ref={entrada}
          value={texto}
          disabled={deshabilitado}
          onChange={e => { setTexto(e.target.value); setAbierto(true) }}
          onFocus={() => setAbierto(true)}
          onBlur={() => setTimeout(() => setAbierto(false), 150)}
          onKeyDown={e => {
            if (e.key === 'Enter') {
              e.preventDefault()
              if (resultados[0]) elegir(resultados[0])
            }
          }}
          placeholder={deshabilitado ? 'Elige primero el cliente' : 'Agregar producto: escribe el nombre o el código…'}
          className="pl-8"
        />

        {abierto && q && (
          <div className="absolute z-30 top-full mt-1 w-full max-h-80 overflow-y-auto bg-white border border-[#E5E7EB] rounded shadow-md">
            {resultados.map(p => (
              <button
                key={p.id}
                type="button"
                onMouseDown={() => elegir(p)}
                className="w-full text-left px-3 py-2 hover:bg-[#F9FAFB] border-b border-[#E5E7EB] last:border-b-0"
              >
                <p className="text-sm font-medium text-[#022F40]">{p.nombrePrincipal}</p>
                <p className="text-sm text-[#80727B] font-mono">{p.codigoInterno}</p>
              </button>
            ))}
            {resultados.length === 0 && !buscando && (
              <p className="px-3 py-3 text-sm text-[#705D56]">
                No se encontró "{q}". Si no está en el catálogo, usa "Línea libre".
              </p>
            )}
          </div>
        )}
      </div>

      <Button type="button" variant="outline" disabled={deshabilitado} onClick={agregarLineaLibre} className="gap-2">
        <PenLine className="w-4 h-4" /> Línea libre
      </Button>
    </div>
  )
}
