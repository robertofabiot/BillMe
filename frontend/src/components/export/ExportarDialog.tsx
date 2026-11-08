import { useState, useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Loader2 } from 'lucide-react'
import { descargarArchivo, descargarPdf, descargarPng } from '@/lib/exportar'

export interface ExportarDialogProps {
  open: boolean
  onOpenChange: (v: boolean) => void
  encabezadoInicial: string
  nombreBase: (extension: string) => string
  horizontal: boolean
  plantilla: (encabezado: string) => ReactNode
  descargarExcel: (encabezado: string) => Promise<Blob>
}

export function ExportarDialog({
  open,
  onOpenChange,
  encabezadoInicial,
  nombreBase,
  horizontal,
  plantilla,
  descargarExcel,
}: ExportarDialogProps) {
  const printRef = useRef<HTMLDivElement>(null)
  const [encabezado, setEncabezado] = useState(encabezadoInicial)
  const [generando, setGenerando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setEncabezado(encabezadoInicial)
      setError(null)
      setGenerando(false)
    }
  }, [open, encabezadoInicial])

  const handleExportPng = async () => {
    if (!printRef.current) return
    setGenerando(true)
    setError(null)
    try {
      await new Promise((r) => setTimeout(r, 50))
      await descargarPng(printRef.current, nombreBase('png'))
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : 'Error al generar la imagen PNG')
    } finally {
      setGenerando(false)
    }
  }

  const handleExportPdf = async () => {
    if (!printRef.current) return
    setGenerando(true)
    setError(null)
    try {
      await new Promise((r) => setTimeout(r, 50))
      await descargarPdf(printRef.current, nombreBase('pdf'), horizontal)
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : 'Error al generar el documento PDF')
    } finally {
      setGenerando(false)
    }
  }

  const handleExportExcel = async () => {
    setGenerando(true)
    setError(null)
    try {
      const blob = await descargarExcel(encabezado)
      descargarArchivo(blob, nombreBase('xlsx'))
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : 'Error al generar el archivo Excel')
    } finally {
      setGenerando(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className={`w-full max-w-[calc(100%-2rem)] ${
          horizontal ? 'sm:max-w-[1060px]' : 'sm:max-w-[850px]'
        } max-h-[90vh] p-0 flex flex-col bg-[#F9FAFB] overflow-hidden`}
      >
        <DialogHeader className="pl-6 pr-12 py-4 border-b border-[#E5E7EB] bg-white shrink-0">
          <DialogTitle className="text-[#022F40] font-semibold text-base">
            Exportar
          </DialogTitle>
        </DialogHeader>

        {/* Barra de Controles: Encabezado y Botones de Exportación */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-6 py-3 border-b border-[#E5E7EB] bg-white shrink-0">
          <div className="flex-1 flex items-center gap-2">
            <label
              htmlFor="exportar-encabezado-input"
              className="text-sm font-medium text-gray-700 whitespace-nowrap"
            >
              Encabezado:
            </label>
            <Input
              id="exportar-encabezado-input"
              value={encabezado}
              onChange={(e) => setEncabezado(e.target.value)}
              disabled={generando}
              placeholder="Encabezado del documento"
            />
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <Button
              type="button"
              variant="outline"
              onClick={handleExportPng}
              disabled={generando}
              className="gap-1.5"
            >
              {generando && <Loader2 className="w-4 h-4 animate-spin" />}
              Imagen (PNG)
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleExportPdf}
              disabled={generando}
              className="gap-1.5"
            >
              {generando && <Loader2 className="w-4 h-4 animate-spin" />}
              PDF
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleExportExcel}
              disabled={generando}
              className="gap-1.5"
            >
              {generando && <Loader2 className="w-4 h-4 animate-spin" />}
              Excel
            </Button>
          </div>
        </div>

        {/* Banner de error en línea */}
        {error && (
          <div
            role="alert"
            className="mx-6 mt-3 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm shrink-0"
          >
            {error}
          </div>
        )}

        {/* Vista previa con desplazamiento */}
        <div className="flex-1 overflow-auto p-6 flex justify-center bg-gray-100">
          <div ref={printRef} className="shadow-sm border border-gray-300">
            {plantilla(encabezado)}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
