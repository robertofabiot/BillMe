import { api } from './api'
import type { ImportacionLista } from '@/types'

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
}
