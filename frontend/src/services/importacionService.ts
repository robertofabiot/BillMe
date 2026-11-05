import { api } from './api'
import type { ImportacionLista, ImportacionCotizaciones, DecisionConciliacion } from '@/types'

const formulario = (archivo: File, fecha?: string) => {
  const form = new FormData()
  form.append('archivo', archivo)
  if (fecha) form.append('fecha', fecha)
  return form
}

export const importacionService = {
  /** Revisa el archivo sin guardar nada. */
  vistaPreviaLista: (archivo: File) =>
    api.subir<ImportacionLista>('/importaciones/lista/vista-previa', formulario(archivo)),

  aplicarLista: (archivo: File, fecha: string) =>
    api.subir<ImportacionLista>('/importaciones/lista/aplicar', formulario(archivo, fecha)),

  vistaPreviaCotizaciones: (archivo: File, clienteId: string) => {
    const form = new FormData()
    form.append('archivo', archivo)
    form.append('clienteId', clienteId)
    return api.subir<ImportacionCotizaciones>('/importaciones/cotizaciones/vista-previa', form)
  },

  aplicarCotizaciones: (archivo: File, clienteId: string, decisiones?: DecisionConciliacion[]) => {
    const form = new FormData()
    form.append('archivo', archivo)
    form.append('clienteId', clienteId)
    if (decisiones && decisiones.length > 0) {
      form.append('decisiones', JSON.stringify(decisiones))
    }
    return api.subir<ImportacionCotizaciones>('/importaciones/cotizaciones/aplicar', form)
  },
}
