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
export async function descargarPdf(
  elemento: HTMLElement,
  nombre: string,
  horizontal: boolean
): Promise<void> {
  const dataUrl = await toPng(elemento, {
    pixelRatio: 2,
    backgroundColor: '#FFFFFF',
  })

  const img = new Image()
  img.src = dataUrl
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve()
    img.onerror = (e) => reject(new Error('No se pudo cargar la imagen para el PDF: ' + String(e)))
  })

  const pdf = new jsPDF({
    orientation: horizontal ? 'l' : 'p',
    unit: 'mm',
    format: 'letter',
  })

  const margin = 10
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const anchoUtil = pageWidth - margin * 2
  const altoUtil = pageHeight - margin * 2

  const imgWidth = img.naturalWidth || elemento.offsetWidth || 1
  const imgHeight = img.naturalHeight || elemento.offsetHeight || 1
  const altoPdf = (imgHeight / imgWidth) * anchoUtil

  const totalPaginas = Math.max(1, Math.ceil(altoPdf / altoUtil))

  for (let i = 0; i < totalPaginas; i++) {
    if (i > 0) {
      pdf.addPage()
    }
    const yOffset = margin - i * altoUtil
    pdf.addImage(dataUrl, 'PNG', margin, yOffset, anchoUtil, altoPdf)

    // Máscaras blancas sobre los márgenes de 10mm para evitar sangrado fuera del área útil
    pdf.setFillColor(255, 255, 255)
    pdf.rect(0, 0, pageWidth, margin, 'F')
    pdf.rect(0, pageHeight - margin, pageWidth, margin, 'F')
    pdf.rect(0, 0, margin, pageHeight, 'F')
    pdf.rect(pageWidth - margin, 0, margin, pageHeight, 'F')
  }

  const blob = pdf.output('blob')
  descargarArchivo(blob, nombre)
}
