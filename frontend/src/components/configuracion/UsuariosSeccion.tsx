import { useEffect, useState, type FormEvent } from 'react'
import type { Usuario } from '@/types'
import { usuarioService } from '@/services'
import { useAuth } from '@/context/AuthContext'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Users, KeyRound, UserPlus } from 'lucide-react'

const PASSWORD_MINIMO = 8

type Aviso = { tipo: 'ok' | 'error'; texto: string } | null

function mensajeDeError(e: unknown) {
  return e instanceof Error ? e.message : 'Ocurrió un error. Intenta de nuevo.'
}

function AvisoTexto({ aviso }: { aviso: Aviso }) {
  if (!aviso) return null
  return (
    <p className={`text-sm ${aviso.tipo === 'ok' ? 'text-[#558564]' : 'text-red-600'}`}>{aviso.texto}</p>
  )
}

export function UsuariosSeccion() {
  const { user } = useAuth()
  const [usuarios, setUsuarios] = useState<Usuario[]>([])

  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetir, setRepetir] = useState('')
  const [avisoPassword, setAvisoPassword] = useState<Aviso>(null)

  const [nombre, setNombre] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [avisoNuevo, setAvisoNuevo] = useState<Aviso>(null)

  const [restableciendo, setRestableciendo] = useState<string | null>(null)
  const [passwordOtro, setPasswordOtro] = useState('')
  const [avisoOtro, setAvisoOtro] = useState<Aviso>(null)

  const cargar = () => usuarioService.listar().then(setUsuarios).catch(console.error)

  useEffect(() => { cargar() }, [])

  const cambiarMiPassword = async (e: FormEvent) => {
    e.preventDefault()
    if (nueva !== repetir) {
      setAvisoPassword({ tipo: 'error', texto: 'La contraseña nueva no coincide en los dos campos.' })
      return
    }
    try {
      await usuarioService.cambiarMiPassword(actual, nueva)
      setActual(''); setNueva(''); setRepetir('')
      setAvisoPassword({ tipo: 'ok', texto: 'Contraseña cambiada.' })
    } catch (err) {
      setAvisoPassword({ tipo: 'error', texto: mensajeDeError(err) })
    }
  }

  const agregarUsuario = async (e: FormEvent) => {
    e.preventDefault()
    try {
      await usuarioService.crear({ nombre, username, password })
      setNombre(''); setUsername(''); setPassword('')
      setAvisoNuevo({ tipo: 'ok', texto: 'Usuario agregado.' })
      cargar()
    } catch (err) {
      setAvisoNuevo({ tipo: 'error', texto: mensajeDeError(err) })
    }
  }

  const restablecer = async (e: FormEvent, id: string) => {
    e.preventDefault()
    try {
      await usuarioService.restablecerPassword(id, passwordOtro)
      setRestableciendo(null); setPasswordOtro('')
      setAvisoOtro({ tipo: 'ok', texto: 'Contraseña restablecida.' })
    } catch (err) {
      setAvisoOtro({ tipo: 'error', texto: mensajeDeError(err) })
    }
  }

  const passwordCorta = (valor: string) => valor.length > 0 && valor.length < PASSWORD_MINIMO

  return (
    <div className="bg-white border border-[#E5E7EB] rounded shadow-sm flex flex-col overflow-hidden">
      <div className="px-6 py-4 border-b border-[#E5E7EB] bg-[#F9FAFB] flex items-center gap-2">
        <Users className="w-4 h-4 text-[#022F40]" />
        <h2 className="text-sm font-semibold text-[#022F40]">Usuarios</h2>
      </div>

      <div className="p-6 flex flex-col gap-8">
        {/* Lista */}
        <div className="flex flex-col gap-2">
          {usuarios.map(u => (
            <div key={u.id} className="border border-[#E5E7EB] rounded px-4 py-3 flex flex-col gap-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-[#022F40]">{u.nombre}</p>
                  <p className="text-xs text-[#705D56]">Usuario: {u.username}{u.username === user?.username && ' (tú)'}</p>
                </div>
                {u.username !== user?.username && restableciendo !== u.id && (
                  <Button variant="outline" onClick={() => { setRestableciendo(u.id); setPasswordOtro(''); setAvisoOtro(null) }}>
                    Restablecer contraseña
                  </Button>
                )}
              </div>
              {restableciendo === u.id && (
                <form onSubmit={e => restablecer(e, u.id)} className="flex flex-col sm:flex-row gap-2">
                  <Input type="password" placeholder="Contraseña nueva" value={passwordOtro}
                         onChange={e => setPasswordOtro(e.target.value)} />
                  <Button type="submit" disabled={passwordOtro.length < PASSWORD_MINIMO}>Guardar</Button>
                  <Button type="button" variant="outline" onClick={() => setRestableciendo(null)}>Cancelar</Button>
                </form>
              )}
            </div>
          ))}
          <AvisoTexto aviso={avisoOtro} />
        </div>

        {/* Cambiar mi contraseña */}
        <form onSubmit={cambiarMiPassword} className="flex flex-col gap-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
            <KeyRound className="w-3.5 h-3.5" /> Cambiar mi contraseña
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Input type="password" placeholder="Contraseña actual" value={actual} onChange={e => setActual(e.target.value)} />
            <Input type="password" placeholder="Contraseña nueva" value={nueva} onChange={e => setNueva(e.target.value)} />
            <Input type="password" placeholder="Repetir contraseña nueva" value={repetir} onChange={e => setRepetir(e.target.value)} />
          </div>
          {passwordCorta(nueva) && <p className="text-xs text-[#705D56]">Mínimo {PASSWORD_MINIMO} caracteres.</p>}
          <AvisoTexto aviso={avisoPassword} />
          <div>
            <Button type="submit" disabled={!actual || nueva.length < PASSWORD_MINIMO || !repetir}>
              Cambiar contraseña
            </Button>
          </div>
        </form>

        {/* Agregar usuario */}
        <form onSubmit={agregarUsuario} className="flex flex-col gap-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-[#80727B] flex items-center gap-1.5">
            <UserPlus className="w-3.5 h-3.5" /> Agregar usuario
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Input placeholder="Nombre (ej. Juan Pérez)" value={nombre} onChange={e => setNombre(e.target.value)} />
            <Input placeholder="Usuario para entrar" value={username} onChange={e => setUsername(e.target.value)} />
            <Input type="password" placeholder="Contraseña" value={password} onChange={e => setPassword(e.target.value)} />
          </div>
          {passwordCorta(password) && <p className="text-xs text-[#705D56]">Mínimo {PASSWORD_MINIMO} caracteres.</p>}
          <AvisoTexto aviso={avisoNuevo} />
          <div>
            <Button type="submit" disabled={!nombre.trim() || !username.trim() || password.length < PASSWORD_MINIMO}>
              Agregar usuario
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
