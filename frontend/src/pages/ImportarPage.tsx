import { Upload } from 'lucide-react'
import { ImportarListaPanel } from '@/components/importacion/ImportarListaPanel'

export function ImportarPage() {
  return (
    <div className="p-8 flex flex-col gap-6 max-w-4xl mx-auto w-full">
      <div>
        <h1 className="text-xl font-semibold text-[#022F40] flex items-center gap-2">
          <Upload className="w-5 h-5" /> Importar
        </h1>
        <p className="text-sm text-[#705D56] mt-0.5">Carga datos desde archivos de Excel.</p>
      </div>

      <ImportarListaPanel />
    </div>
  )
}
