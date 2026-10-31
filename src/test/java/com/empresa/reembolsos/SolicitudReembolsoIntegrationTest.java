package com.empresa.reembolsos;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SolicitudReembolsoIntegrationTest {

    @Autowired
    private SolicitudReembolsoService servicio;

    @Test
    void flujoCompletoCrearEnTresCategoriasValidarCalcularYCambiarEstado() {
        // 1. Crear y calcular en las 3 categorias
        var viaje = servicio.crear(new SolicitudReembolso(
            "EMP-INT", CategoriaGasto.VIAJE, new BigDecimal("120000"), LocalDate.of(2026, 10, 10), 2, true));
        assertNotNull(viaje.getId());
        assertEquals(0, new BigDecimal("120000").compareTo(viaje.getMontoAprobado()));
        assertEquals(EstadoSolicitud.PENDIENTE, viaje.getEstado());

        var representacion = servicio.crear(new SolicitudReembolso(
            "EMP-INT", CategoriaGasto.REPRESENTACION, new BigDecimal("1500000"), LocalDate.of(2026, 10, 12), 0, true));
        assertEquals(0, new BigDecimal("1500000").compareTo(representacion.getMontoAprobado()));

        var capacitacion = servicio.crear(new SolicitudReembolso(
            "EMP-INT", CategoriaGasto.CAPACITACION, new BigDecimal("1000000"), LocalDate.of(2026, 10, 15), 0, true));
        assertEquals(0, new BigDecimal("800000.0").compareTo(capacitacion.getMontoAprobado()));

        // 2. Verificar rechazo por duplicado en mismo mes y categoria
        var duplicada = new SolicitudReembolso(
            "EMP-INT", CategoriaGasto.VIAJE, new BigDecimal("90000"), LocalDate.of(2026, 10, 25), 1, true);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(duplicada));

        // 3. Cambiar estado y disparar notificadores
        var aprobada = servicio.cambiarEstado(viaje.getId(), EstadoSolicitud.APROBADA);
        assertEquals(EstadoSolicitud.APROBADA, aprobada.getEstado());
    }
}
