import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { TooltipProvider } from '@/components/ui/tooltip'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/context/AuthContext'
import { LoginPage } from '@/pages/LoginPage'
import { DocumentosPage } from '@/pages/DocumentosPage'
import { DocumentoEditorPage } from '@/pages/DocumentoEditorPage'
import { ClientesPage } from '@/pages/ClientesPage'
import { ProductosPage } from '@/pages/ProductosPage'
import { ProyectosPage } from '@/pages/ProyectosPage'
import { ReportesPage } from '@/pages/ReportesPage'
import { ConfiguracionPage } from '@/pages/ConfiguracionPage'
import { ConsolidadosPage } from '@/pages/ConsolidadosPage'
import { ImportarPage } from '@/pages/ImportarPage'
import { PreciosPage } from '@/pages/PreciosPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <TooltipProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route index         element={<DocumentosPage tipo="FACTURA" />}  />
                <Route path="cotizaciones" element={<DocumentosPage tipo="COTIZACION" />} />
                <Route path="precios" element={<PreciosPage />} />
                <Route path="documentos/nuevo" element={<DocumentoEditorPage />} />
                <Route path="documentos/:id" element={<DocumentoEditorPage />} />
                <Route path="clientes"  element={<ClientesPage />}  />
                <Route path="productos" element={<ProductosPage />} />
                <Route path="proyectos" element={<ProyectosPage />} />
                <Route path="consolidados" element={<ConsolidadosPage />} />
                <Route path="reportes"  element={<ReportesPage />}  />
                <Route path="configuracion" element={<ConfiguracionPage />} />
                <Route path="importar" element={<ImportarPage />} />
              </Route>
            </Route>
          </Routes>
        </TooltipProvider>
      </BrowserRouter>
    </AuthProvider>
  )
}
