import { useRef, useState, useEffect } from 'react'
import { toPng } from 'html-to-image'
import type { Factura } from '@/types'
import { formatCurrency, formatDate } from '@/lib/format'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Download, Loader2 } from 'lucide-react'

interface Props {
  factura: Factura | null
  open: boolean
  onOpenChange: (v: boolean) => void
}

export function FacturaExportSheet({ factura, open, onOpenChange }: Props) {
  const printRef = useRef<HTMLDivElement>(null)
  const [isExporting, setIsExporting] = useState(false)
  
  // Editable fields
  const [editableDate, setEditableDate] = useState('')
  const [editableClient, setEditableClient] = useState('')

  useEffect(() => {
    if (factura && open) {
      setEditableDate(formatDate(factura.createdAt))
      setEditableClient(factura.clienteNombre || 'CLIENTE GENERICO')
    }
  }, [factura, open])

  if (!factura) return null

  const handleExport = async () => {
    if (!printRef.current) return
    setIsExporting(true)
    try {
      // Small delay to ensure any layout shifts are done
      await new Promise(r => setTimeout(r, 100))
      
      const dataUrl = await toPng(printRef.current, { 
        quality: 1, 
        backgroundColor: '#FFFFFF',
        pixelRatio: 2 // High quality
      })
      
      const link = document.createElement('a')
      link.download = `Factura_${factura.folioInterno}.png`
      link.href = dataUrl
      link.click()
    } catch (err) {
      console.error('Error exporting image:', err)
      alert('Hubo un error al generar la imagen.')
    } finally {
      setIsExporting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-full max-w-[calc(100%-2rem)] sm:max-w-[850px] max-h-[90vh] p-0 flex flex-col bg-[#F9FAFB] overflow-hidden">
        <DialogHeader className="px-6 py-4 border-b border-[#E5E7EB] bg-white shrink-0">
          <div className="flex items-center justify-between">
            <DialogTitle className="text-[#022F40]">Exportar Documento</DialogTitle>
            <Button 
              onClick={handleExport}
              disabled={isExporting}
              className="bg-[#558564] hover:bg-[#558564]/90 text-white gap-2 h-9 rounded shadow-sm"
            >
              {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
              {isExporting ? 'Generando...' : 'Descargar Imagen'}
            </Button>
          </div>
        </DialogHeader>

        <div className="flex-1 overflow-auto p-6 flex justify-center bg-gray-100">
          
          {/* El contenedor que será capturado, diseñado estilo Excel puro */}
          <div 
            ref={printRef}
            className="bg-white p-8 border border-gray-300 shadow-sm font-sans"
            style={{ width: '700px', color: '#000000' }}
          >
            {/* Fila 1: Fecha a la derecha */}
            <div className="flex justify-end mb-4">
              <input 
                type="text" 
                value={editableDate} 
                onChange={(e) => setEditableDate(e.target.value)}
                className="text-right font-bold focus:outline-none border-b border-transparent focus:border-gray-300 hover:bg-gray-50"
                style={{ width: '200px' }}
              />
            </div>

            {/* Fila 2: Cliente */}
            <div className="mb-6">
              <input 
                type="text" 
                value={editableClient} 
                onChange={(e) => setEditableClient(e.target.value)}
                className="w-full font-bold text-lg focus:outline-none border-b border-transparent focus:border-gray-300 hover:bg-gray-50 uppercase"
              />
            </div>

            {/* Tabla Excel */}
            <table className="w-full border-collapse border border-black mb-1">
              <thead>
                <tr>
                  <th className="border border-black px-2 py-1 text-center font-bold text-sm w-16">CANTIDAD</th>
                  <th className="border border-black px-2 py-1 text-center font-bold text-sm">DESCRIPCIÓN</th>
                  <th className="border border-black px-2 py-1 text-center font-bold text-sm w-24">PESO TOTAL (KG)</th>
                  <th className="border border-black px-2 py-1 text-center font-bold text-sm w-28">PRECIO UNITARIO</th>
                  <th className="border border-black px-2 py-1 text-center font-bold text-sm w-28">PRECIO TOTAL</th>
                </tr>
              </thead>
              <tbody>
                {factura.detalles.map((d) => (
                  <tr key={d.id}>
                    <td className="border border-black px-2 py-1 text-center text-sm font-semibold">{d.cantidad}</td>
                    <td className="border border-black px-2 py-1 text-sm uppercase">
                      <input 
                        type="text"
                        defaultValue={d.productoNombre}
                        className="w-full focus:outline-none hover:bg-gray-50 uppercase"
                      />
                    </td>
                    <td className="border border-black px-2 py-1 text-center text-sm">
                      <input 
                        type="text"
                        defaultValue={d.pesoUnitario ? (d.cantidad * d.pesoUnitario).toFixed(2) : "-"}
                        className="w-full text-center focus:outline-none hover:bg-gray-50"
                      />
                    </td>
                    <td className="border border-black px-2 py-1 text-right text-sm">
                      <input 
                        type="text"
                        defaultValue={formatCurrency(d.precioUnitarioVenta)}
                        className="w-full text-right focus:outline-none hover:bg-gray-50"
                      />
                    </td>
                    <td className="border border-black px-2 py-1 text-right text-sm font-semibold">
                      {formatCurrency(d.cantidad * d.precioUnitarioVenta)}
                    </td>
                  </tr>
                ))}
                
                {/* FIN DE LINEA */}
                <tr>
                  <td className="border border-black px-2 py-1 text-center font-bold italic" colSpan={5}>
                    ***FIN DE LINEA***
                  </td>
                </tr>
              </tbody>
            </table>

            {/* TOTALES */}
            <div className="flex justify-end mt-4">
              <table className="border-collapse border border-black w-[280px] bg-white">
                <tbody>
                  <tr className="border-b border-black">
                    <td className="px-4 py-1 border-r border-black font-bold">TOTAL C$</td>
                    <td className="px-4 py-1 font-bold text-right w-[120px]">
                      {formatCurrency(factura.totalVenta)}
                    </td>
                  </tr>
                  <tr>
                    <td className="px-4 py-1 border-r border-black font-bold">PESO TOTAL (KG)</td>
                    <td className="px-4 py-1 font-bold text-right w-[120px]">
                      {factura.detalles.reduce((sum, d) => sum + (d.pesoUnitario ? d.cantidad * d.pesoUnitario : 0), 0).toFixed(2)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
