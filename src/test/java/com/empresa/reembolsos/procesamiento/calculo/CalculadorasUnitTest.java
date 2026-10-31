package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadorasUnitTest {

    @Test
    void calculadoraViajeAplicaTopeDiarioMultiplicadoPorDias() {
        var calculadora = new CalculadoraViaje();
        var solicitud = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.VIAJE, new BigDecimal("140000"), LocalDate.of(2026, 10, 10), 1, true);

        BigDecimal aprobado = calculadora.calcular(solicitud);

        assertEquals(0, new BigDecimal("140000").compareTo(aprobado));
    }

    @Test
    void calculadoraRepresentacionReembolsaMontoCompletoHastaTopeMensual() {
        var calculadora = new CalculadoraRepresentacion();
        var solicitud = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.REPRESENTACION, new BigDecimal("1800000"), LocalDate.of(2026, 10, 10), 0, true);

        BigDecimal aprobado = calculadora.calcular(solicitud);

        assertEquals(0, new BigDecimal("1800000").compareTo(aprobado));
    }

    @Test
    void calculadoraCapacitacionReembolsaOchentaPorCientoHastaTopeAnual() {
        var calculadora = new CalculadoraCapacitacion();
        var solicitud = new SolicitudReembolso(
            "EMP-01", CategoriaGasto.CAPACITACION, new BigDecimal("1000000"), LocalDate.of(2026, 10, 10), 0, true);

        BigDecimal aprobado = calculadora.calcular(solicitud);

        assertEquals(0, new BigDecimal("800000.0").compareTo(aprobado));
    }
}
