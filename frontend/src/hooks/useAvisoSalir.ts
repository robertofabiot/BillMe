import { useEffect } from 'react'

export const SALIR_SIN_GUARDAR = 'Hay cambios sin guardar. ¿Salir sin guardar?'

export function useAvisoSalir(cambiosSinGuardar: boolean) {
  useEffect(() => {
    if (!cambiosSinGuardar) return

    const alClic = (e: MouseEvent) => {
      const enlace = e.target instanceof Element && e.target.closest('a[href]')
      if (enlace && !window.confirm(SALIR_SIN_GUARDAR)) {
        e.preventDefault()
        e.stopPropagation()
      }
    }

    const alCerrar = (e: BeforeUnloadEvent) => e.preventDefault()

    document.addEventListener('click', alClic, true)
    window.addEventListener('beforeunload', alCerrar)

    return () => {
      document.removeEventListener('click', alClic, true)
      window.removeEventListener('beforeunload', alCerrar)
    }
  }, [cambiosSinGuardar])
}
