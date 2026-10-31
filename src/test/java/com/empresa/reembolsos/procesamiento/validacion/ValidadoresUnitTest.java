package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadoresUnitTest {

    @Test
    void validadorDocumentosSoporteRechazaCuandoNoHaySoporte() {
        var validador = new ValidadorDocumentosSoporte();
        var solicitudSinSoporte = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.VIAJE, new BigDecimal("100000"), LocalDate.of(2026, 10, 10), 1, false);

        var resultado = validador.validar(solicitudSinSoporte, List.of());

        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void validadorMontoMaximoRechazaCuandoSuperaTopeDeCategoria() {
        var validador = new ValidadorMontoMaximo();
        var solicitudExcedida = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.VIAJE, new BigDecimal("200000"), LocalDate.of(2026, 10, 10), 2, true);

        var resultado = validador.validar(solicitudExcedida, List.of());

        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().contains("Monto excede el tope de la categoria VIAJE"));
    }

    @Test
    void validadorDuplicadosRechazaCuandoExistePendienteEnMismoMesYCategoria() {
        var validador = new ValidadorDuplicados();
        var existente = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.CAPACITACION, new BigDecimal("500000"), LocalDate.of(2026, 10, 5), 0, true);
        var nueva = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.CAPACITACION, new BigDecimal("600000"), LocalDate.of(2026, 10, 20), 0, true);

        var resultado = validador.validar(nueva, List.of(existente));

        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", resultado.motivoRechazo());
    }
}