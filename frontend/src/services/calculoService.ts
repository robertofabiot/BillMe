import { api } from './api'
import type { CalculoPesoRequest, CalculoPesoResponse, PrecioCalculadoResponse } from '@/types'

export const calculoService = {
  peso: (data: CalculoPesoRequest) =>
    api.post<CalculoPesoResponse>('/calculos/peso', data),

  precioPorPeso: (data: { pesoUnitKg: number; tarifaUsd: number }) =>
    api.post<PrecioCalculadoResponse>('/calculos/precio-por-peso', data),

  precioPorMetro: (data: { precioReferencia: number; largoReferencia: number; largoNuevo: number }) =>
    api.post<PrecioCalculadoResponse>('/calculos/precio-por-metro', data),
}
