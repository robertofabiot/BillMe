import { toPng } from 'html-to-image'
import { jsPDF } from 'jspdf'

/**
 * Genera el nombre del archivo según la convención estándar:
 * {folio}_{clienteLimpio}_{fecha}.{extension}
 * El nombre del cliente se normaliza en NFKD, se eliminan acentos
 * y se reemplaza cualquier carácter no alfanumérico por '-'.
 */
export function nombreArchivo(
  folio: string,
  cliente: string,
  fecha: string,
  extension: string
): string {
  const extLimpia = extension ? (extension.startsWith('.') ? extension.slice(1) : extension) : ''
  const clienteStr = cliente || ''
  const clienteSinAcentos = clienteStr
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
  const clienteLimpio = clienteSinAcentos
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  const fechaStr = fecha || ''
  return `${folio}_${clienteLimpio}_${fechaStr}.${extLimpia}`
}

/**
 * Descarga un Blob en el navegador creando un elemento <a> temporal.
 */
export function descargarArchivo(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

/**
 * Exporta el elemento HTML a imagen PNG con pixelRatio: 2 y fondo blanco.
 */
export async function descargarPng(elemento: HTMLElement, nombre: string): Promise<void> {
  const dataUrl = await toPng(elemento, {
    pixelRatio: 2,
    backgroundColor: '#FFFFFF',
  })
  const res = await fetch(dataUrl)
  const blob = await res.blob()
  descargarArchivo(blob, nombre)
}

/**
 * Exporta el elemento HTML a un documento PDF en formato carta con márgenes de 10mm.
 * Si la altura calculada excede el alto útil de una página, se distribuye en múltiples
 * páginas desplazando la imagen y aplicando máscaras en los márgenes.
 */
export async function descargarPdf(elemento: HTMLElement, nombre: string, horizontal: boolean): Promise<void> {
  const dataUrl = await toPng(elemento, { pixelRatio: 2, backgroundColor: '#FFFFFF' })
  const img = new Image()
  img.src = dataUrl
  await img.decode()

  const pdf = new jsPDF({ orientation: horizontal ? 'l' : 'p', unit: 'mm', format: 'letter' })
  const margen = 10
  const anchoUtil = pdf.internal.pageSize.getWidth() - margen * 2
  const altoUtil = pdf.internal.pageSize.getHeight() - margen * 2

  const anchoCss = elemento.offsetWidth
  const escala = img.naturalWidth / anchoCss
  const altoTotalCss = img.naturalHeight / escala
  const altoPaginaCss = (altoUtil / anchoUtil) * anchoCss
  const origen = elemento.getBoundingClientRect().top
  const finesDeFila = [...elemento.querySelectorAll('tr')]
    .map(fila => fila.getBoundingClientRect().bottom - origen)

  let inicio = 0
  let primera = true
  while (inicio < altoTotalCss - 1) {
    const limite = inicio + altoPaginaCss
    let fin = altoTotalCss
    if (limite < altoTotalCss) {
      const candidatos = finesDeFila.filter(y => y > inicio && y <= limite)
      fin = candidatos.length > 0 ? Math.max(...candidatos) : limite
    }

    const recorte = document.createElement('canvas')
    recorte.width = img.naturalWidth
    recorte.height = Math.ceil((fin - inicio) * escala)
    const contexto = recorte.getContext('2d')
    if (!contexto) throw new Error('No se pudo preparar el PDF')
    contexto.fillStyle = '#FFFFFF'
    contexto.fillRect(0, 0, recorte.width, recorte.height)
    contexto.drawImage(img, 0, -Math.floor(inicio * escala))

    if (!primera) pdf.addPage()
    pdf.addImage(recorte.toDataURL('image/png'), 'PNG', margen, margen, anchoUtil,
      ((fin - inicio) / anchoCss) * anchoUtil, undefined, 'FAST')
    primera = false
    inicio = fin
  }
  descargarArchivo(pdf.output('blob'), nombre)
}
