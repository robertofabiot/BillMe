package com.example.billme.importacion;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Arma archivos de Excel en memoria para los tests (sin datos reales de clientes). */
public final class ExcelDePrueba {

    private final XSSFWorkbook libro = new XSSFWorkbook();
    private XSSFSheet hoja;

    public ExcelDePrueba hoja(String nombre) {
        hoja = libro.createSheet(nombre);
        return this;
    }

    /** Agrega una fila; los Number se guardan como celdas numéricas y el resto como texto. */
    public ExcelDePrueba fila(Object... valores) {
        XSSFRow fila = hoja.createRow(hoja.getPhysicalNumberOfRows());
        for (int i = 0; i < valores.length; i++) {
            Object valor = valores[i];
            if (valor instanceof Number numero) {
                fila.createCell(i).setCellValue(numero.doubleValue());
            } else if (valor != null) {
                fila.createCell(i).setCellValue(valor.toString());
            }
        }
        return this;
    }

    public byte[] bytes() {
        try (libro; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Lista como la del proveedor: sus encabezados reales y algunas filas de catálogo. */
    public static ExcelDePrueba listaDelProveedor() {
        return new ExcelDePrueba()
                .hoja("cotizaciones").fila("cod_producto", "fecha", "cantidad")
                .hoja("lista_precios")
                .fila("Número de artículo", "Descripción del artículo", "Unidad de medida de inventario",
                        "Peso 1 - Inventario Kgs", "Nueva Lista Precios en Cordobas C$ antes de IVA");
    }
}
