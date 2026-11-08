import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { ApiError, esperarServidor } from '@/services/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ReceiptText, ShieldAlert, Loader2, WifiOff } from 'lucide-react'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [servidor, setServidor] = useState<'conectando' | 'listo' | 'sin-conexion'>('conectando')
  const [tardando, setTardando] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  // Despierta el backend apenas se abre el login, mientras se escribe el usuario
  useEffect(() => {
    let vigente = true
    const aviso = setTimeout(() => { if (vigente) setTardando(true) }, 1500)
    esperarServidor().then(ok => {
      clearTimeout(aviso)
      if (vigente) setServidor(ok ? 'listo' : 'sin-conexion')
    })
    return () => { vigente = false; clearTimeout(aviso) }
  }, [])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username || !password) return
    
    setIsSubmitting(true)
    setError(null)
    
    try {
      await login({ username, password })
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401
        ? 'Usuario o contraseña incorrectos.'
        : 'No se pudo conectar con el servidor. Revisa tu internet e inténtalo de nuevo.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col justify-center items-center p-4">
      <div className="w-full max-w-sm bg-[#FFFFFF] border border-[#E5E7EB] rounded p-8 shadow-sm">
        
        {/* Cabecera - Alineación a la Izquierda estricta */}
        <div className="flex flex-col gap-2 mb-8 text-left">
          <div className="w-10 h-10 rounded bg-[#022F40] flex items-center justify-center mb-2">
            <ReceiptText className="w-5 h-5 text-[#91E5F6]" />
          </div>
          <h1 className="text-xl font-bold text-[#022F40] tracking-tight">Iniciar Sesión</h1>
          <p className="text-sm text-[#705D56]">Accede al panel de administración de BillMe.</p>
        </div>

        {/* Formulario - Elementos alineados de acuerdo a la grilla */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-6 text-left">

          {servidor === 'conectando' && tardando && (
            <div className="border border-[#E5E7EB] bg-[#F9FAFB] text-[#022F40] text-sm p-4 rounded flex items-start gap-2">
              <Loader2 className="w-4 h-4 shrink-0 mt-0.5 animate-spin" />
              <p>Conectando con el servidor… La primera vez del día puede tardar hasta un minuto.</p>
            </div>
          )}

          {servidor === 'sin-conexion' && (
            <div className="border border-red-200 text-red-600 text-sm p-4 rounded flex items-start gap-2">
              <WifiOff className="w-4 h-4 shrink-0 mt-0.5" />
              <p>No se pudo conectar con el servidor. Revisa tu internet y recarga la página.</p>
            </div>
          )}

          {error && (
            <div className="bg-[#FFFFFF] border border-red-200 text-red-600 text-sm p-4 rounded flex items-start gap-2">
              <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
              <p>{error}</p>
            </div>
          )}

          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-[#022F40]">Usuario</label>
            <Input 
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Tu usuario" 
              className="bg-[#FFFFFF] border-[#E5E7EB] text-[#022F40] placeholder:text-[#80727B] rounded focus-visible:ring-[#91E5F6]"
              disabled={isSubmitting}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-[#022F40]">Contraseña</label>
            <Input 
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••" 
              className="bg-[#FFFFFF] border-[#E5E7EB] text-[#022F40] placeholder:text-[#80727B] rounded focus-visible:ring-[#91E5F6]"
              disabled={isSubmitting}
            />
          </div>

          <Button 
            type="submit"
            disabled={isSubmitting || !username || !password}
            className="mt-2 w-full bg-[#91E5F6] hover:bg-[#91E5F6]/80 text-[#022F40] font-semibold rounded shadow-none border border-[#91E5F6]/20 transition-colors"
          >
            {isSubmitting ? 'Verificando...' : 'Entrar'}
          </Button>
        </form>
      </div>
    </div>
  )
}
