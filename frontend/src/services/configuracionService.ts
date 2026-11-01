import { api } from './api'
import type { ConfiguracionSistema } from '@/types'

export const configuracionService = {
  obtener: () =>
    api.get<ConfiguracionSistema>('/configuracion'),

  actualizar: (data: ConfiguracionSistema) =>
    api.put<ConfiguracionSistema>('/configuracion', data),
}
