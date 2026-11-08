package com.example.billme.service;

import com.example.billme.calculo.CalculadoraPrecio;
import com.example.billme.dto.PrecioFormulaResponse;
import com.example.billme.dto.PrecioHistorialResponse;
import com.example.billme.dto.PrecioSugeridoResponse;
import com.example.billme.enums.MonedaTarifa;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.PrecioLista;
import com.example.billme.model.Producto;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoLineaRepository;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.util.Fechas;
import com.example.billme.util.NormalizadorTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecioSugeridoService {

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final DocumentoLineaRepository documentoLineaRepository;
    private final PrecioListaRepository precioListaRepository;
    private final ConfiguracionService configuracionService;

    public PrecioSugeridoResponse sugerir(UUID clienteId, UUID productoId, String descripcion) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        String norm = producto.isGenerico()
                ? (descripcion != null && !descripcion.trim().isEmpty() ? NormalizadorTexto.normalizar(descripcion) : null)
                : null;

        DocumentoLinea linea = null;
        String origen = null;

        // Un genérico sin descripción no tiene historial
        if (!producto.isGenerico() || norm != null) {
            List<DocumentoLinea> propias = documentoLineaRepository.ultimosPrecios(productoId, clienteId, norm, PageRequest.of(0, 1));
            if (!propias.isEmpty()) {
                linea = propias.get(0);
                origen = "ESTE_CLIENTE";
            } else {
                List<DocumentoLinea> otras = documentoLineaRepository.ultimosPrecios(productoId, null, norm, PageRequest.of(0, 1));
                if (!otras.isEmpty()) {
                    linea = otras.get(0);
                    origen = "OTRO_CLIENTE";
                }
            }
        }

        BigDecimal precioUnit = null;
        String clienteNombre = null;
        String folio = null;
        LocalDate fecha = null;
        Long diasAntiguedad = null;
        boolean viejo = false;

        if (linea != null) {
            precioUnit = linea.getPrecioUnit();
            clienteNombre = linea.getDocumento().getCliente().getNombre();
            folio = linea.getDocumento().getFolio();
            fecha = linea.getDocumento().getFecha();
            diasAntiguedad = ChronoUnit.DAYS.between(fecha, Fechas.hoy());
            viejo = diasAntiguedad > configuracionService.diasPrecioViejo();
        }

        Optional<PrecioLista> listaOpt = precioListaRepository.findFirstByProductoIdOrderByFechaDescCreatedAtDesc(productoId);
        BigDecimal precioLista = listaOpt.map(PrecioLista::getPrecioSinIva).orElse(null);
        BigDecimal descuentoPct = cliente.getDescuentoPct();
        BigDecimal precioFormula = null;
        if (precioLista != null) {
            precioFormula = CalculadoraPrecio.porFormula(precioLista, configuracionService.factorIva(), descuentoPct);
        }

        boolean propia = cliente.getTarifaKg() != null;
        BigDecimal tarifaKg = propia ? cliente.getTarifaKg() : configuracionService.tarifaKgUsd();
        String monedaTarifa = propia ? cliente.getTarifaMoneda().name() : MonedaTarifa.USD.name();
        String origenTarifa = propia ? "CLIENTE" : "CONFIGURACION";

        return new PrecioSugeridoResponse(
                precioUnit,
                origen,
                clienteNombre,
                folio,
                fecha,
                diasAntiguedad,
                viejo,
                precioLista,
                descuentoPct,
                precioFormula,
                tarifaKg,
                monedaTarifa,
                origenTarifa
        );
    }

    public PrecioFormulaResponse calcularFormula(UUID productoId, BigDecimal descuentoPct) {
        if (!productoRepository.existsById(productoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        PrecioLista lista = precioListaRepository.findFirstByProductoIdOrderByFechaDescCreatedAtDesc(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Este producto no tiene precio de lista"));

        BigDecimal desc = descuentoPct != null ? descuentoPct : BigDecimal.ZERO;
        BigDecimal pu = CalculadoraPrecio.porFormula(lista.getPrecioSinIva(), configuracionService.factorIva(), desc);
        return new PrecioFormulaResponse(lista.getPrecioSinIva(), desc, pu);
    }

    public List<PrecioHistorialResponse> historial(String q, UUID clienteId) {
        String qNorm = (q != null && !q.isBlank()) ? NormalizadorTexto.normalizar(q) : "";
        String contiene = !qNorm.isEmpty() ? "%" + qNorm + "%" : "";
        List<DocumentoLinea> lineas = documentoLineaRepository.historial(qNorm, contiene, clienteId);
        int diasPrecioViejo = configuracionService.diasPrecioViejo();
        LocalDate hoy = Fechas.hoy();

        return lineas.stream().map(l -> {
            Documento doc = l.getDocumento();
            long dias = ChronoUnit.DAYS.between(doc.getFecha(), hoy);
            boolean viejo = dias > diasPrecioViejo;
            return new PrecioHistorialResponse(
                    doc.getId(),
                    doc.getFolio(),
                    doc.getTipo().name(),
                    doc.getFecha(),
                    doc.getCliente().getNombre(),
                    l.getDescripcion(),
                    l.getCantidad(),
                    l.getPrecioUnit(),
                    viejo
            );
        }).toList();
    }
}
