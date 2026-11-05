import { api } from './api'
import type { CompraRequest, CompraResponse } from '@/types'

export const compraService = {
  registrar: (documentoId: string, data: CompraRequest) =>
    api.post<CompraResponse>(`/documentos/${documentoId}/compras`, data),
  listarPorDocumento: (documentoId: string) =>
    api.get<CompraResponse[]>(`/documentos/${documentoId}/compras`),
}
