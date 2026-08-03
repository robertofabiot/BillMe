import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { TooltipProvider } from '@/components/ui/tooltip'
import { AppLayout } from '@/components/layout/AppLayout'
import { FacturasPage } from '@/pages/FacturasPage'
import { ClientesPage } from '@/pages/ClientesPage'
import { ProductosPage } from '@/pages/ProductosPage'
import { ProyectosPage } from '@/pages/ProyectosPage'
import { ReportesPage } from '@/pages/ReportesPage'
import { ConfiguracionPage } from '@/pages/ConfiguracionPage'
import { ConsolidadosPage } from '@/pages/ConsolidadosPage'

export default function App() {
  return (
    <BrowserRouter>
      <TooltipProvider>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index         element={<FacturasPage />}  />
            <Route path="clientes"  element={<ClientesPage />}  />
            <Route path="productos" element={<ProductosPage />} />
            <Route path="proyectos" element={<ProyectosPage />} />
            <Route path="consolidados" element={<ConsolidadosPage />} />
            <Route path="reportes"  element={<ReportesPage />}  />
            <Route path="configuracion" element={<ConfiguracionPage />} />
          </Route>
        </Routes>
      </TooltipProvider>
    </BrowserRouter>
  )
}
