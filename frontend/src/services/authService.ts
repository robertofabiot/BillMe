import { api } from './api'
import type { AuthRequest, AuthResponse } from '@/types'

export const authService = {
  login: async (credentials: AuthRequest): Promise<AuthResponse> => {
    return api.post<AuthResponse>('/auth/login', credentials)
  }
}
